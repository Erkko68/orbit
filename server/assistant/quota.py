"""Each caller's share of the assistant (#67): calls a day and a month, counted by the server itself, out of any client's reach.

The counts live in SQLite (standard library) on a Docker volume, so they survive restarts. Days and months are UTC.
"""

import sqlite3
import threading
from collections.abc import Callable, Mapping
from datetime import UTC, datetime, timedelta

from assistant.interview import AssistantError

# Counts one call by a uid, or raises RESOURCE_EXHAUSTED when the caller has none left.
Meter = Callable[[str], None]


def next_day(now: datetime) -> datetime:
    return datetime(now.year, now.month, now.day, tzinfo=UTC) + timedelta(days=1)


def next_month(now: datetime) -> datetime:
    return datetime(now.year + now.month // 12, now.month % 12 + 1, 1, tzinfo=UTC)


class Quota:
    """At most [daily] calls a UTC day and [monthly] a UTC month for each uid, counted in the SQLite file at [path]."""

    def __init__(self, path: str, daily: int, monthly: int, clock: Callable[[], datetime] = lambda: datetime.now(UTC)):
        self.daily = daily
        self.monthly = monthly
        self.clock = clock
        self.lock = threading.Lock()
        self.db = sqlite3.connect(path, check_same_thread=False)
        # ponytail: one row per uid and period, never pruned; delete old periods if the file ever matters.
        self.db.execute(
            "CREATE TABLE IF NOT EXISTS usage (uid TEXT, period TEXT, calls INTEGER, PRIMARY KEY (uid, period))"
        )

    def use(self, uid: str) -> None:
        """Counts a call by [uid]. Over either limit it counts nothing and says when the caller has calls again."""
        now = self.clock()
        periods = [
            (now.strftime("%Y-%m-%d"), self.daily, next_day(now)),
            (now.strftime("%Y-%m"), self.monthly, next_month(now)),
        ]
        with self.lock, self.db:
            for period, limit, resets in periods:
                row = self.db.execute("SELECT calls FROM usage WHERE uid = ? AND period = ?", (uid, period)).fetchone()
                if (row[0] if row else 0) >= limit:
                    raise AssistantError(
                        "RESOURCE_EXHAUSTED",
                        "The assistant's quota is spent.",
                        {"reason": "quota", "resetsAt": resets.isoformat().replace("+00:00", "Z")},
                    )
            for period, _, _ in periods:
                self.db.execute(
                    "INSERT INTO usage VALUES (?, ?, 1) ON CONFLICT (uid, period) DO UPDATE SET calls = calls + 1",
                    (uid, period),
                )


def from_env(env: Mapping[str, str]) -> Meter:
    """The quota the environment sets: ASSISTANT_DAILY_QUOTA and ASSISTANT_MONTHLY_QUOTA calls, counted in QUOTA_DB."""
    return Quota(
        env.get("QUOTA_DB", "quota.sqlite3"),
        int(env.get("ASSISTANT_DAILY_QUOTA", "50")),
        int(env.get("ASSISTANT_MONTHLY_QUOTA", "500")),
    ).use
