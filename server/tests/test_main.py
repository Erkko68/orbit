import pytest

import main

QUESTION = {"text": "Who lives there?", "options": ["Roommates", "Family"], "multiSelect": False, "freeText": True}


def verify(token):
    if token != "valid":
        raise ValueError("not a Firebase ID token")
    return {"uid": "ana"}


def client(generate=lambda prompt, schema: QUESTION):
    return main.create_app(verify_token=verify, generate=generate).test_client()


def test_answers_a_signed_in_caller_with_the_first_question():
    response = client().post("/interview", json={"data": {}}, headers={"Authorization": "Bearer valid"})

    assert response.status_code == 200
    assert response.get_json()["result"]["text"] == "Who lives there?"
    assert response.get_json()["result"]["id"] == "q1"


@pytest.mark.parametrize("headers", [{}, {"Authorization": "Bearer forged"}, {"Authorization": "Basic valid"}])
def test_rejects_a_caller_without_a_valid_token_before_the_model(headers):
    calls = []

    response = client(lambda *args: calls.append(args)).post("/interview", json={"data": {}}, headers=headers)

    assert response.status_code == 401
    assert response.get_json()["error"]["status"] == "UNAUTHENTICATED"
    assert calls == []


def test_a_model_that_fails_is_unavailable():
    def down(prompt, schema):
        raise OSError("connection refused")

    response = client(down).post("/interview", json={"data": {}}, headers={"Authorization": "Bearer valid"})

    assert response.status_code == 503
    assert response.get_json()["error"] == {
        "status": "UNAVAILABLE",
        "message": "The assistant could not answer.",
        "details": {"reason": "unavailable"},
    }


def test_says_it_is_up():
    assert client().get("/health").get_json() == {"ok": True}


def test_uses_firebase_and_the_configured_model_by_default(monkeypatch):
    made = []
    monkeypatch.setattr("firebase_admin.initialize_app", lambda **kwargs: made.append(kwargs))
    monkeypatch.setenv("FIREBASE_PROJECT_ID", "orbit-test")
    monkeypatch.setenv("ASSISTANT_PROVIDER", "local")

    main.create_app()

    assert made == [{"options": {"projectId": "orbit-test"}}]
