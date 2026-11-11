import pytest

from assistant import auth


def test_a_valid_token_names_its_user():
    calls = []

    def post(url, body, headers):
        calls.append((url, body, headers))
        return {"users": [{"localId": "ana"}]}

    assert auth.firebase_tokens({"FIREBASE_API_KEY": "key"}, post)("token") == "ana"
    assert calls == [(auth.LOOKUP, {"idToken": "token"}, {"X-Goog-Api-Key": "key"})]


def test_a_token_that_names_no_user_is_not_valid():
    verify = auth.firebase_tokens({"FIREBASE_API_KEY": "key"}, lambda *args, **kwargs: {})

    with pytest.raises(ValueError):
        verify("token")


def test_sends_the_public_url_as_referer_for_a_key_restricted_to_a_website():
    calls = []

    def post(url, body, headers):
        calls.append(headers)
        return {"users": [{"localId": "ana"}]}

    auth.firebase_tokens({"FIREBASE_API_KEY": "key", "PUBLIC_URL": "https://orbit.example"}, post)("token")

    assert calls == [{"X-Goog-Api-Key": "key", "Referer": "https://orbit.example"}]
