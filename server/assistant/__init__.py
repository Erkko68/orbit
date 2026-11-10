"""The assistant's server (ADR 0007): who is calling, the interview, the models it asks, and what it may propose.

Its data sits next to the package, not inside it: the prompts, and the schemas the app reads too.
"""

from pathlib import Path

SERVER = Path(__file__).resolve().parent.parent
PROMPTS = SERVER / "prompts"
SCHEMAS = SERVER / "schemas"
