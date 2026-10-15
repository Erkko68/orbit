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
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
