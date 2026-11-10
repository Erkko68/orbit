import json

from assistant import net


class Reply:
    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False

    def read(self, *args):
        return b'{"ok": true}'


def test_posts_json_with_its_headers_and_reads_the_json_reply(monkeypatch):
    sent = []

    def urlopen(request, timeout):
        sent.append((request.full_url, request.get_method(), json.loads(request.data), dict(request.header_items())))
        return Reply()

    monkeypatch.setattr("urllib.request.urlopen", urlopen)

    assert net.post_json("http://model/x", {"a": 1}, headers={"X-Goog-Api-Key": "k"}) == {"ok": True}
    url, method, body, headers = sent[0]
    assert (url, method, body) == ("http://model/x", "POST", {"a": 1})
    assert headers["Content-type"] == "application/json"
    assert headers["X-goog-api-key"] == "k"
