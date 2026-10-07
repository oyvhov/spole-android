"""Offline tests of evidence handling, API boundary and advisory reporting."""

import contextlib
import copy
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error

SCRIPT = Path(__file__).resolve().parents[1] / "jev-review.py"
SPEC = importlib.util.spec_from_file_location("jev_review", SCRIPT)
review = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(review)


def response(choice="unknown", kind="failure", confidence=1.0):
    return {
        "model": "jev-1.13.0",
        "answers": {"assessment": {
            "type": "choice", "choice": choice, "confidence": confidence,
            "probabilities": {key: float(key == choice) for key in review.options(kind)},
        }},
        "usage": {"input_tokens": 123},
    }


def case(kind="failure"):
    return {"id": "one", "kind": kind,
            "state": {"current": {"failure": "timeout"}} if kind == "failure" else
                     {"claim": "All tests passed", "evidence": "One assertion failed"}}


class ReviewTest(unittest.TestCase):
    def setUp(self):
        (review.REPO / "app/build").mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=review.REPO / "app/build", prefix="jev-test-")
        self.root = Path(self.temp.name)
        self.env = patch.dict(os.environ, {"TYPESAFE_API_KEY": "synthetic-private-key"})
        self.env.start()

    def tearDown(self):
        self.env.stop()
        self.temp.cleanup()

    def write_xml(self, text, directory=None, name="TEST-example.xml"):
        directory = directory or self.root
        directory.mkdir(parents=True, exist_ok=True)
        (directory / name).write_text(text, encoding="utf-8")
        return directory

    def test_scrubs_credentials_addresses_and_actual_key(self):
        source = ('Authorization: Bearer private-bearer token="another-secret" '
                  'password=real-password api_key=api-value cookie=session-value '
                  'https://private.test/path?token=bad person@example.test 192.168.1.23 '
                  'synthetic-private-key C:\\Users\\PrivateName\\test /home/private/log '
                  'eyJhbGci.eyJzdWIi.signature')
        cleaned = review.scrub(source)
        for secret in ("private-bearer", "another-secret", "real-password", "api-value", "session-value",
                       "private.test", "person@example.test", "192.168.1.23", "synthetic-private-key",
                       "PrivateName", "/home/private", "eyJhbGci"):
            self.assertNotIn(secret, cleaned)

    def test_scrubs_nested_state(self):
        self.assertEqual(review.clean_state({"evidence": ["token=private", 2, None]}),
                         {"evidence": ["token=[REDACTED]", 2, None]})

    def test_counts_real_testcases_not_claimed_suite_totals(self):
        self.write_xml('<testsuite tests="999"><testcase classname="A" name="ok"/>'
                       '<testcase classname="A" name="fail"><failure message="bad"/></testcase>'
                       '<testcase classname="A" name="error"><error>crash</error></testcase>'
                       '<testcase classname="A" name="skip"><skipped/></testcase></testsuite>')
        _, counts, issues = review.collect_junit([self.root])
        self.assertEqual(counts, {"passed": 1, "failed": 2, "skipped": 1, "total": 4})
        self.assertEqual(issues, [])

    def test_discards_stdout_and_properties(self):
        self.write_xml('<testsuite><properties><property name="password" value="secret"/></properties>'
                       '<testcase classname="A" name="f"><failure>timeout</failure>'
                       '<system-out>secret stdout</system-out></testcase><system-err>secret stderr</system-err></testsuite>')
        tests, _, _ = review.collect_junit([self.root])
        self.assertNotIn("secret", json.dumps(list(tests.values())))

    def test_duplicate_inputs_not_double_counted(self):
        self.write_xml('<testsuite><testcase classname="A" name="ok"/></testsuite>')
        _, counts, _ = review.collect_junit([self.root, self.root])
        self.assertEqual(counts["total"], 1)

    def test_conflicting_results_are_not_green(self):
        self.write_xml('<testsuite><testcase classname="A" name="f"/></testsuite>')
        self.write_xml('<testsuite><testcase classname="A" name="f"><failure>bad</failure></testcase></testsuite>', name="TEST-second.xml")
        _, counts, issues = review.collect_junit([self.root])
        self.assertEqual(counts["failed"], 1)
        self.assertEqual(counts["passed"], 0)
        self.assertTrue(issues)

    def test_empty_and_invalid_reports_are_explicit(self):
        _, counts, issues = review.collect_junit([self.root])
        self.assertEqual(counts["total"], 0)
        self.assertTrue(issues)
        self.write_xml("broken xml")
        self.assertTrue(review.collect_junit([self.root])[2])

    def test_rejects_entities(self):
        self.write_xml('<!DOCTYPE x [<!ENTITY secret "private">]><testsuite/>')
        self.assertTrue(review.collect_junit([self.root])[2])

    def test_reads_nested_android_reports(self):
        self.write_xml('<testsuites><testsuite><testcase classname="Tv" name="play"/></testsuite></testsuites>',
                       directory=self.root / "connected/device")
        self.assertEqual(review.collect_junit([self.root])[1]["passed"], 1)

    def test_baseline_same_failure_stays_failed(self):
        current = {("A", "f"): {"test": "A#f", "outcome": "failed", "failure": "timeout"}}
        record = review.failure_cases(current, current)[0]
        self.assertEqual(record["comparison"], "same_failure")
        self.assertEqual(record["state"]["current"]["outcome"], "failed")

    def test_baseline_passed_and_missing_are_distinct(self):
        current = {("A", "f"): {"test": "A#f", "outcome": "failed", "failure": "timeout"}}
        baseline = {("A", "f"): {"outcome": "passed", "failure": ""}}
        self.assertEqual(review.failure_cases(current, baseline)[0]["comparison"], "previously_passed")
        self.assertEqual(review.failure_cases(current, {})[0]["comparison"], "no_comparable_baseline")

    def test_different_failures_are_not_assumed_identical(self):
        current = {("A", "f"): {"test": "A#f", "outcome": "failed", "failure": "crash"}}
        baseline = {("A", "f"): {"outcome": "failed", "failure": "timeout"}}
        self.assertEqual(review.failure_cases(current, baseline)[0]["comparison"], "no_comparable_baseline")

    def test_fasit_is_never_sent(self):
        sample = dict(case(), expected="unknown", origin="private answer")
        payload = review.request_for(sample, review.MODEL)
        self.assertNotIn("expected", payload)
        self.assertNotIn("private answer", json.dumps(payload))

    def test_all_24_cases_have_valid_unique_labels(self):
        cases = review.load_cases(Path(__file__).parent / "cases.json")
        self.assertEqual(len(cases), 24)
        self.assertEqual(sum(c["origin"].startswith("Synthetic") for c in cases), 21)

    def test_offline_never_calls_model_even_with_key(self):
        with patch.object(review, "call_jev", side_effect=AssertionError("network forbidden")):
            results, _, calls = review.evaluate([case()], review.MODEL, False, 10)
        self.assertEqual(calls, 0)
        self.assertEqual(results[0]["status"], "offline")

    def test_missing_key_is_explicit_and_no_network(self):
        with patch.dict(os.environ, {"TYPESAFE_API_KEY": ""}), patch.object(review, "call_jev") as call:
            results, _, calls = review.evaluate([case()], review.MODEL, True, 10)
        call.assert_not_called()
        self.assertEqual(calls, 0)
        self.assertEqual(results[0]["status"], "missing_key")

    def test_budget_and_low_confidence(self):
        answer = review.validate_response(response(confidence=0.6), review.options("failure"))
        with patch.object(review, "call_jev", return_value=(answer, None)) as call:
            results, _, calls = review.evaluate([case(), dict(case(), id="two")], review.MODEL, True, 1)
        self.assertEqual(call.call_count, 1)
        self.assertEqual(calls, 1)
        self.assertTrue(results[0]["manual_review"])
        self.assertEqual(results[1]["status"], "call_limit")

    def test_no_retry_after_service_failure(self):
        with patch.object(review, "call_jev", return_value=(None, "http_401")) as call:
            results, _, calls = review.evaluate([case(), dict(case(), id="two")], review.MODEL, True, 10)
        self.assertEqual(call.call_count, 1)
        self.assertEqual(calls, 1)
        self.assertEqual([r["status"] for r in results], ["http_401", "service_unavailable"])

    def test_oversized_evidence_is_not_silently_truncated(self):
        sample = dict(case(), state={"current": "a" * (review.STATE_LIMIT + 1)})
        with patch.object(review, "call_jev") as call:
            results, previews, _ = review.evaluate([sample], review.MODEL, True, 10)
        call.assert_not_called()
        self.assertEqual(results[0]["status"], "input_too_large")
        self.assertEqual(previews, [])

    def test_rejects_unexpected_choice_invalid_numbers_and_probabilities(self):
        mutations = [lambda a: a.update(choice="approved"), lambda a: a.update(confidence=float("nan")),
                     lambda a: a.update(confidence=True), lambda a: a.update(probabilities={"unknown": 1}),
                     lambda a: a.update(probabilities={k: 0.0 for k in review.options("failure")})]
        for mutate in mutations:
            data = response()
            mutate(data["answers"]["assessment"])
            with self.subTest(data=data), self.assertRaises(ValueError):
                review.validate_response(data, review.options("failure"))

    def test_drops_arbitrary_response_fields(self):
        data = response()
        data["secret"] = "private"
        data["answers"]["assessment"]["explanation"] = "private"
        self.assertNotIn("private", json.dumps(review.validate_response(data, review.options("failure"))))

    def test_http_uses_fixed_endpoint_post_and_no_secret_in_body(self):
        mock_response = io.BytesIO(json.dumps(response()).encode())
        with patch.object(review.urllib.request, "build_opener") as build:
            build.return_value.open.return_value = mock_response
            answer, error = review.call_jev(review.request_for(case(), review.MODEL), "synthetic-private-key")
        req = build.return_value.open.call_args.args[0]
        self.assertEqual(req.full_url, review.ENDPOINT)
        self.assertEqual(req.method, "POST")
        self.assertNotIn(b"synthetic-private-key", req.data)
        self.assertIsNone(error)
        self.assertEqual(answer["choice"], "unknown")

    def test_http_errors_never_echo_error_body(self):
        error = urllib.error.HTTPError(review.ENDPOINT, 401, "private", {}, io.BytesIO(b"private-secret"))
        with patch.object(review.urllib.request, "build_opener") as build:
            build.return_value.open.side_effect = error
            answer, status = review.call_jev(review.request_for(case(), review.MODEL), "private")
        self.assertIsNone(answer)
        self.assertEqual(status, "http_401")

    def test_redirects_are_not_followed(self):
        self.assertIsNone(review.NoRedirect().redirect_request(None, None, 302, "", {}, "https://other.test"))

    def test_offline_metrics_have_no_fabricated_accuracy(self):
        metrics = review.calibration_metrics([{"id": "one", "expected": "app", "status": "offline"}])
        self.assertEqual(metrics["reviewed"], 0)
        self.assertIsNone(metrics["accuracy"])

    def test_false_reassurance_is_counted_separately(self):
        results = [{"id": "one", "kind": "claim", "expected": "contradicted", "choice": "supported", "status": "reviewed"},
                   {"id": "two", "kind": "failure", "expected": "app", "choice": "app", "status": "reviewed"}]
        metrics = review.calibration_metrics(results)
        self.assertEqual(metrics["accuracy"], 0.5)
        self.assertEqual(metrics["false_support"], ["one"])

    def test_end_to_end_offline_report_preserves_failure(self):
        self.write_xml('<testsuite><testcase classname="A" name="f"><failure>timeout</failure></testcase></testsuite>')
        output = self.root / "report"
        with patch.object(review, "call_jev", side_effect=AssertionError("no network")), contextlib.redirect_stdout(io.StringIO()):
            exit_code = review.main(["--junit", str(self.root), "--output", str(output)])
        self.assertEqual(exit_code, 0)
        report = json.loads((output / "report.json").read_text(encoding="utf-8"))
        self.assertEqual(report["junit"]["failed"], 1)
        self.assertEqual(report["calls"], 0)
        self.assertIn("Ingen Jev-vurdering", (output / "report.md").read_text(encoding="utf-8"))

    def test_missing_key_main_exits_nonzero_and_writes_report(self):
        with patch.dict(os.environ, {"TYPESAFE_API_KEY": ""}), contextlib.redirect_stdout(io.StringIO()):
            exit_code = review.main(["--cases", str(Path(__file__).parent / "cases.json"), "--live", "--output", str(self.root / "out")])
        self.assertEqual(exit_code, 1)
        data = json.loads((self.root / "out/report.json").read_text(encoding="utf-8"))
        self.assertEqual(data["calls"], 0)
        self.assertTrue(all(r["status"] == "missing_key" for r in data["results"]))


if __name__ == "__main__":
    unittest.main()
