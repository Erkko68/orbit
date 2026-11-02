"""Cloud Functions for Orbit. The AI assistant runs here, on Vertex AI as the function's service account."""

from firebase_admin import initialize_app
from firebase_functions import https_fn
from firebase_functions.params import PROJECT_ID, StringParam

from assistant.interview import first_question
from assistant.provider import GeminiProvider

initialize_app()

# The model and where it runs are configuration, so #70 can change them without a code change.
ASSISTANT_MODEL = StringParam("ASSISTANT_MODEL", default="gemini-3.5-flash")
ASSISTANT_LOCATION = StringParam("ASSISTANT_LOCATION", default="global")

_provider: GeminiProvider | None = None


def _gemini() -> GeminiProvider:
    """One client per instance, made on the first call: params have no value while the code is loaded."""
    global _provider
    if _provider is None:
        from google import genai

        client = genai.Client(vertexai=True, project=PROJECT_ID.value, location=ASSISTANT_LOCATION.value)
        _provider = GeminiProvider(client, ASSISTANT_MODEL.value)
    return _provider


@https_fn.on_call()
def interview(req: https_fn.CallableRequest) -> dict:
    return first_question(req.auth, _gemini)
