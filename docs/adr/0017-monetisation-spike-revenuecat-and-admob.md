# ADR 0017: RevenueCat and AdMob fit the project; billing stays simulated until the team wants real stores

- Status: Proposed
- Date: 2026-11-17
- Issue: #81

## Context

Sprint 3 adds monetisation in test mode (#117 to #122): ads for the free tier and a paid tier that removes them. Before building it, #81 checked on a spike branch (never merged) whether the two usual SDKs fit Orbit's stack: Kotlin 2.4.20, Compose Multiplatform 1.12.1, AGP 9.1.1 and `minSdk` 24.

- **AdMob** shows ads. Google ships no Kotlin Multiplatform SDK, only the native ones (`play-services-ads` on Android, Google Mobile Ads on iOS).
- **RevenueCat** sells subscriptions and purchases, and talks to Google Play and the App Store for us. It ships an official KMP SDK, `purchases-kmp`.

Orbit is a university project that will not be published (see AGENTS.md), so no real money or store listing is involved.

## Decision

**Both SDKs fit the stack. Sprint 3 builds billing behind a domain interface with a simulated implementation, and swaps in RevenueCat only if the team decides to go to real stores.** This is a proposal for the team to accept.

What the spike found:

- **AdMob on Android: works.** `play-services-ads` 25.5.0 builds with this stack, and on an Android emulator the test banner loaded (`AdListener.onAdLoaded`, banner marked "Test Ad"), using Google's public test app and ad unit IDs. No AdMob account is needed for that.
- **RevenueCat on Android: builds, offering not read.** `purchases-kmp-core` 4.2.0 resolves in `commonMain` and links into the debug APK. It needs Kotlin 2.3.20 or newer and Android API 23 or newer, and Orbit meets both. Reading a sandbox offering needs a RevenueCat project and a Test Store key (`test_…`). The sign-up asked for a credit card, so the team did not create one. That part of #81 is **not verified**.
- **iOS: not verified.** The spike ran on Android only, because no machine had a Mac. Both SDKs have iOS builds (`purchases-kmp` lists iOS 13+), but nothing here shows them working with Orbit's iOS framework.

What each one needs beyond the dependency:

- **AdMob:** an AdMob account and its real app and ad unit IDs for anything but test ads; the Apple developer account (#174) for the iOS app; and a consent prompt (Google's UMP SDK) for users in the EU, which includes our own.
- **RevenueCat:** a project and API key per platform; the store accounts (Google Play Console, and #174 for Apple) to sell for real; the Test Store key is debug-only and must never ship in a store build.

What #117 to #122 should do:

- Put billing behind an interface in `domain/` (for example `BillingRepository`: current plan, available offer, purchase, restore). ViewModels depend on the interface only, as with every repository.
- Ship a simulated implementation: a plan that is stored in Firestore, and a "purchase" that only flips it. No payment is taken.
- Show ads with the AdMob test IDs, behind its own `expect`/`actual` (the banner is a native view on each platform).
- Keep RevenueCat out of the dependencies until real billing is wanted. Then it replaces the simulated implementation without touching a screen.

## Alternatives considered

- **Use RevenueCat's Test Store now.** Gives a verified offering, but needs an account that asks for a card, for a result that has no use once the project is graded.
- **Real store sandboxes (Play Console, App Store Connect).** Need paid developer accounts and review, and the Apple account is not ready (#174). Far more than a university project needs.
- **Build billing by hand against the store APIs.** More code and more risk than a simulated implementation, and it still needs the store accounts.
- **Skip monetisation.** Possible, but #117 to #122 are in the plan; a simulated version keeps them with little cost.

## Consequences

- AGENTS.md says fakes live in tests, not in the app. A simulated billing implementation in `data/` would be the one exception, and the team has to agree to it; if it does, AGENTS.md ("Architecture") gets a line saying so.
- #81 closes with two parts unmet: the sandbox offering read and the iOS run. The issue says so and links the follow-up, which is the decision of #117 to #122 above.
- #117 to #122 are rescoped to the interface plus the simulated implementation, and #174 stays a blocker only if real iOS ads or purchases are ever wanted.
- If the team later goes to real stores, the facts above (versions, accounts, EU consent) are the starting list.
