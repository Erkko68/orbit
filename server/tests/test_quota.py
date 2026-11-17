from datetime import UTC, datetime

import pytest

from assistant import api, quota
from assistant.interview import AssistantError

NOW = datetime(2026, 11, 17, 15, 30, tzinfo=UTC)


class Clock:
    def __init__(self, now=NOW):
        self.now = now

    def __call__(self):
        return self.now


def spent(meter, uid="ana"):
    with pytest.raises(AssistantError) as refused:
        meter(uid)
    return refused.value


def test_counts_each_call_up_to_the_daily_limit_then_says_when_it_resets():
    use = quota.Quota(":memory:", daily=2, monthly=10, clock=Clock()).use

    use("ana")
    use("ana")
    refusal = spent(use)

    assert refusal.status == "RESOURCE_EXHAUSTED"
    assert refusal.details == {"reason": "quota", "resetsAt": "2026-11-18T00:00:00Z"}


def test_each_caller_has_their_own_count():
    use = quota.Quota(":memory:", daily=1, monthly=10, clock=Clock()).use

    use("ana")
    use("ben")

    assert spent(use, "ana").status == "RESOURCE_EXHAUSTED"


def test_the_daily_count_resets_the_next_utc_day():
    clock = Clock()
    use = quota.Quota(":memory:", daily=1, monthly=10, clock=clock).use
    use("ana")

    clock.now = datetime(2026, 11, 18, 0, 0, tzinfo=UTC)
    use("ana")


def test_the_monthly_limit_holds_across_days_and_resets_with_the_month():
    clock = Clock(datetime(2026, 12, 30, tzinfo=UTC))
    use = quota.Quota(":memory:", daily=5, monthly=2, clock=clock).use
    use("ana")
    clock.now = datetime(2026, 12, 31, tzinfo=UTC)
    use("ana")

    assert spent(use).details["resetsAt"] == "2027-01-01T00:00:00Z"
    clock.now = datetime(2027, 1, 1, tzinfo=UTC)
    use("ana")


def test_a_refused_call_does_not_count():
    clock = Clock()
    use = quota.Quota(":memory:", daily=1, monthly=2, clock=clock).use
    use("ana")
    spent(use)

    clock.now = datetime(2026, 11, 18, tzinfo=UTC)
    use("ana")


def test_the_counts_survive_a_restart(tmp_path):
    path = str(tmp_path / "quota.sqlite3")
    quota.Quota(path, daily=1, monthly=10, clock=Clock()).use("ana")

    assert spent(quota.Quota(path, daily=1, monthly=10, clock=Clock()).use).status == "RESOURCE_EXHAUSTED"


def test_the_limits_come_from_the_environment(tmp_path):
    use = quota.from_env({"QUOTA_DB": str(tmp_path / "q.sqlite3"), "ASSISTANT_DAILY_QUOTA": "1"})

    use("ana")
    assert spent(use).status == "RESOURCE_EXHAUSTED"


def test_a_spent_quota_is_refused_before_the_model_with_too_many_requests():
    calls = []
    use = quota.Quota(":memory:", daily=0, monthly=10, clock=Clock()).use

    status, body = api.respond(
        "POST", "/interview", "Bearer valid", lambda token: "ana", lambda *args: calls.append(args),
        b'{"data": {"answers": []}}', meter=use,
    )

    assert status == 429
    assert body["error"]["status"] == "RESOURCE_EXHAUSTED"
    assert body["error"]["details"] == {"reason": "quota", "resetsAt": "2026-11-18T00:00:00Z"}
    assert calls == []


def test_a_caller_without_a_token_is_not_counted():
    counted = []

    api.respond("POST", "/interview", "", lambda token: "ana", lambda *args: {}, b"{}", meter=counted.append)

    assert counted == []
