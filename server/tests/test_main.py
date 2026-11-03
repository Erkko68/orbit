import json
import threading
import urllib.error
import urllib.request

import pytest

import main

QUESTION = {"text": "Who lives there?", "options": ["Roommates", "Family"], "multiSelect": False, "freeText": True}


def verify(token):
    if token != "valid":
        raise ValueError("not a Firebase ID token")
    return "ana"


def ask(path="/interview", method="POST", authorization="Bearer valid", generate=lambda prompt, schema: QUESTION):
    return main.respond(method, path, authorization, verify, generate)


def test_answers_a_signed_in_caller_with_the_first_question():
    status, body = ask()

    assert status == 200
    assert body["result"]["text"] == "Who lives there?"
    assert body["result"]["id"] == "q1"


@pytest.mark.parametrize("authorization", ["", "Bearer forged", "Basic valid"])
def test_rejects_a_caller_without_a_valid_token_before_the_model(authorization):
    calls = []

    status, body = ask(authorization=authorization, generate=lambda *args: calls.append(args))

    assert status == 401
    assert body["error"]["status"] == "UNAUTHENTICATED"
    assert calls == []


def test_a_model_that_fails_is_unavailable():
    def down(prompt, schema):
        raise OSError("connection refused")

    status, body = ask(generate=down)

    assert status == 503
    assert body["error"] == {
        "status": "UNAVAILABLE",
        "message": "The assistant could not answer.",
        "details": {"reason": "unavailable"},
    }


def test_says_it_is_up_and_knows_no_other_endpoint():
    assert ask("/health", "GET") == (200, {"ok": True})
    assert ask("/other")[0] == 404


@pytest.fixture
def running(monkeypatch):
    """The real server on a free local port, with fake token checks and a fake model."""
    monkeypatch.setattr(main.auth, "firebase_tokens", lambda env: verify)
    monkeypatch.setattr(main.models, "from_env", lambda env: lambda prompt, schema: QUESTION)
    server = main.server({"HOST": "127.0.0.1", "PORT": "0"})
    threading.Thread(target=server.serve_forever, daemon=True).start()
    yield f"http://127.0.0.1:{server.server_address[1]}"
    server.shutdown()
    server.server_close()


def call(url, method, authorization=None, body=None):
    headers = {"Content-Type": "application/json", **({"Authorization": authorization} if authorization else {})}
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


def test_serves_the_interview_over_http(running):
    status, body = call(f"{running}/interview", "POST", "Bearer valid", {"data": {}})

    assert status == 200
    assert body["result"]["text"] == "Who lives there?"


def test_answers_errors_over_http_as_json(running):
    assert call(f"{running}/interview", "POST", body={"data": {}})[0] == 401
    assert call(f"{running}/health", "GET") == (200, {"ok": True})
