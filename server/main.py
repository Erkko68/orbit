"""Cloud Functions for Orbit. The AI assistant runs here, on Vertex AI as the function's service account."""

import functools
import json

from firebase_admin import initialize_app
from firebase_functions import https_fn
from firebase_functions.params import PROJECT_ID, StringParam

from assistant import first_question

initialize_app()

# The model and where it runs are configuration, so #70 can change them without a code change.
ASSISTANT_MODEL = StringParam("ASSISTANT_MODEL", default="gemini-3.5-flash")
ASSISTANT_LOCATION = StringParam("ASSISTANT_LOCATION", default="global")


@functools.cache
def _client():
    """One client per instance, made on the first call: params have no value while the code is loaded."""
    from google import genai

    return genai.Client(vertexai=True, project=PROJECT_ID.value, location=ASSISTANT_LOCATION.value)


def _gemini(prompt: str, schema: dict) -> object:
    """Gemini's answer to [prompt], as JSON that follows [schema]. No key: Vertex AI trusts the service account."""
    from google.genai import types

    response = _client().models.generate_content(
        model=ASSISTANT_MODEL.value,
        contents=prompt,
        config=types.GenerateContentConfig(response_mime_type="application/json", response_schema=schema),
    )
    return json.loads(response.text)


@https_fn.on_call()
def interview(req: https_fn.CallableRequest) -> dict:
    return first_question(req.auth, _gemini)
