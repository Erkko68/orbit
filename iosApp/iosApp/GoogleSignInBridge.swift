import FirebaseCore
import GoogleSignIn
import Shared
import UIKit

/// Shows Google's account picker for `IosGoogleAuthClient`: the SDK is a Swift package Kotlin cannot call.
final class SdkGoogleSignInBridge: GoogleSignInBridge {
    func signIn(onResult: @escaping (String?, String?, String?) -> Void) {
        DispatchQueue.main.async {
            guard let clientID = FirebaseApp.app()?.options.clientID else {
                onResult(nil, nil, "No CLIENT_ID in GoogleService-Info.plist")
                return
            }
            guard let presenter = Self.topViewController() else {
                onResult(nil, nil, "No window to present Google Sign-In from")
                return
            }
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if (error as? GIDSignInError)?.code == .canceled {
                    onResult(nil, nil, nil)
                } else if let error {
                    onResult(nil, nil, error.localizedDescription)
                } else if let user = result?.user, let idToken = user.idToken {
                    onResult(idToken.tokenString, user.accessToken.tokenString, nil)
                } else {
                    onResult(nil, nil, "Google Sign-In returned no ID token")
                }
            }
        }
    }

    private static func topViewController() -> UIViewController? {
        var top = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
