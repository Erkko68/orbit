"""Who is calling: the uid in a Firebase ID token, checked by Firebase itself, so the server needs no SDK."""

from collections.abc import Callable, Mapping

from assistant.net import Post, post_json

# Firebase Authentication's own check of an ID token: it answers with the user, or an error for a bad token.
LOOKUP = "https://identitytoolkit.googleapis.com/v1/accounts:lookup"

# The uid in a Firebase ID token. Raises when the token is not valid.
VerifyToken = Callable[[str], str]


def firebase_tokens(env: Mapping[str, str], post: Post = post_json) -> VerifyToken:
    """Checks tokens with FIREBASE_API_KEY, an API key of the project allowed to call the Identity Toolkit API."""
    headers = {"X-Goog-Api-Key": env["FIREBASE_API_KEY"]}
    # The key may be restricted to websites: Google then checks the Referer, so send the server's public address.
    if public_url := env.get("PUBLIC_URL"):
        headers["Referer"] = public_url

    def verify(token: str) -> str:
        users = post(LOOKUP, {"idToken": token}, headers=headers).get("users") or []
        if not users:
            raise ValueError("The token names no user")
        return users[0]["localId"]

    return verify
