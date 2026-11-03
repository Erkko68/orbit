"""JSON over HTTP with the standard library, so the server installs nothing."""

import json
import urllib.request
from collections.abc import Callable

# An HTTP POST of a JSON body, with extra headers, that returns the JSON reply. Raises on an HTTP error.
Post = Callable[..., dict]


def post_json(url: str, body: dict, headers: dict[str, str] | None = None) -> dict:
    """Generous timeout: a small model on a CPU can take a while to answer."""
    request = urllib.request.Request(
        url,
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json", **(headers or {})},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.load(response)
