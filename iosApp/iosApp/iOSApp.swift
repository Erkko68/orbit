import FirebaseCore
import GoogleSignIn
import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        PlatformModule_iosKt.doInitKoin(googleSignIn: SdkGoogleSignInBridge())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Google Sign-In returns through its own scheme. Any other link is an invitation: orbit://join/<code>.
                    if !GIDSignIn.sharedInstance.handle(url) {
                        InvitationLinksKt.offerInvitationLink(link: url.absoluteString)
                    }
                }
        }
    }
}
