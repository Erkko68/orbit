import FirebaseCore
import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        KoinKt.doInitKoin(config: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
