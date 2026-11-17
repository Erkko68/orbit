import json
import threading
import urllib.error
import urllib.request

import pytest

from assistant import api

QUESTION = {"text": "Who lives there?", "options": ["Roommates", "Family"], "multiSelect": False, "freeText": True}


def verify(token):
    if token != "valid":
        raise ValueError("not a Firebase ID token")
    return "ana"


def ask(
    path="/interview",
    method="POST",
    authorization="Bearer valid",
    generate=lambda prompt, schema: QUESTION,
    body=b'{"data": {"answers": []}}',
    enabled=True,
):
    return api.respond(method, path, authorization, verify, generate, body, enabled)


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


def test_a_switched_off_assistant_refuses_everyone_before_the_model():
    calls = []

    status, body = ask(enabled=False, generate=lambda *args: calls.append(args))

    assert status == 412
    assert body["error"] == {
        "status": "FAILED_PRECONDITION",
        "message": "The assistant is switched off.",
        "details": {"reason": "disabled"},
    }
    assert calls == []


def test_a_switched_off_assistant_still_says_it_is_up():
    assert ask("/health", "GET", enabled=False) == (200, {"ok": True})


@pytest.mark.parametrize("value", ["false", "False", " 0 ", "no", "OFF"])
def test_the_environment_switches_the_assistant_off(value):
    assert not api.assistant_enabled({"ASSISTANT_ENABLED": value})


@pytest.mark.parametrize("env", [{}, {"ASSISTANT_ENABLED": "true"}, {"ASSISTANT_ENABLED": ""}, {"ASSISTANT_ENABLED": "1"}])
def test_the_assistant_is_on_unless_the_environment_says_otherwise(env):
    assert api.assistant_enabled(env)


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


@pytest.mark.parametrize("body", [b"", b"not json", b"[]", b'{"data": "answers"}'])
def test_a_body_that_is_not_an_interview_is_an_invalid_argument(body):
    status, response = ask(body=body)

    assert status == 400
    assert response["error"]["status"] == "INVALID_ARGUMENT"


def test_answers_with_the_plan_once_the_model_is_ready():
    answer = {"questionId": "q1", "question": "Who lives there?", "selected": ["Family"], "text": ""}
    body = json.dumps({"data": {"answers": [answer]}}).encode()

    def model(prompt, schema):
        return {"version": 1, "items": [{"title": "Groceries", "modules": {}}]} if "items" in schema["properties"] else {**QUESTION, "ready": True}

    status, response = ask(generate=model, body=body)

    assert status == 200
    assert response["result"] == {"plan": {"version": 1, "items": [{"title": "Groceries", "modules": {}}]}}


def test_says_it_is_up_and_knows_no_other_endpoint():
    assert ask("/health", "GET") == (200, {"ok": True})
    assert ask("/other")[0] == 404


@pytest.fixture
def running(monkeypatch):
    """The real server on a free local port, with fake token checks and a fake model."""
    monkeypatch.setattr(api.auth, "firebase_tokens", lambda env: verify)
    monkeypatch.setattr(api.providers, "from_env", lambda env: lambda prompt, schema: QUESTION)
    server = api.server({"HOST": "127.0.0.1", "PORT": "0"})
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


def test_refuses_over_http_when_switched_off(monkeypatch):
    monkeypatch.setattr(api.auth, "firebase_tokens", lambda env: verify)
    monkeypatch.setattr(api.providers, "from_env", lambda env: lambda prompt, schema: QUESTION)
    server = api.server({"HOST": "127.0.0.1", "PORT": "0", "ASSISTANT_ENABLED": "false"})
    threading.Thread(target=server.serve_forever, daemon=True).start()
    try:
        url = f"http://127.0.0.1:{server.server_address[1]}"
        status, body = call(f"{url}/interview", "POST", "Bearer valid", {"data": {}})
    finally:
        server.shutdown()
        server.server_close()

    assert status == 412
    assert body["error"]["status"] == "FAILED_PRECONDITION"


def test_answers_errors_over_http_as_json(running):
    assert call(f"{running}/interview", "POST", body={"data": {}})[0] == 401
    assert call(f"{running}/health", "GET") == (200, {"ok": True})
