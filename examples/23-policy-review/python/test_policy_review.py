from policy_review import BROAD, NARROW, ROLES, review_policy, review_role


def test_the_broad_policy_has_a_wildcard_action_and_a_wildcard_resource():
    assert [f.split(": ")[1].split(" ")[0] for f in review_policy(BROAD)] == ["action", "resource"]


def test_the_narrow_policy_has_no_findings():
    assert review_policy(NARROW) == []


def test_a_predefined_role_with_deploy_is_flagged_twice_and_the_custom_role_passes():
    assert len(review_role(ROLES["predefined"])) == 2
    assert review_role(ROLES["custom"]) == []
