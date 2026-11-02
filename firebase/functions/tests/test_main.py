from types import SimpleNamespace

import main


def test_makes_one_vertex_client_per_instance(monkeypatch):
    made = []
    monkeypatch.setenv("GCLOUD_PROJECT", "orbit-test")
    monkeypatch.setenv("GOOGLE_CLOUD_PROJECT", "orbit-test")
    monkeypatch.setattr("google.genai.Client", lambda **kwargs: made.append(kwargs) or SimpleNamespace(models=None))
    monkeypatch.setattr(main, "_provider", None)

    first = main._gemini()
    second = main._gemini()

    assert first is second
    assert len(made) == 1
    assert made[0]["vertexai"] is True
    assert made[0]["location"] == "global"
