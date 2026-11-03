import pytest

import auth


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
