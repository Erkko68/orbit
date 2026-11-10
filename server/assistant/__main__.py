"""`python -m assistant`: serves the assistant with the configuration in the environment."""

import logging
import os

from assistant.api import server

logging.basicConfig(level=logging.INFO)
server(os.environ).serve_forever()
