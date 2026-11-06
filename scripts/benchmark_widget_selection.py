#!/usr/bin/env python3
"""Compare widget selection only. Explicitly run; each request may incur provider fees."""
import argparse
import json
import math
import statistics
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://localhost:8080')
    parser.add_argument('--cases', type=Path, default=Path(__file__).resolve().parents[1] / 'src/test/resources/widget-selection-cases.json')
    parser.add_argument('--providers', nargs='+', choices=['jev', 'openai'], default=['jev', 'openai'])
    parser.add_argument('--repeats', type=int, default=1)
    parser.add_argument('--limit', type=int, default=0, help='0 runs all cases')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.repeats < 1 or args.limit < 0:
        parser.error('repeats must be positive and limit cannot be negative')
    cases = json.loads(args.cases.read_text(encoding='utf-8'))
    if args.limit:
        cases = cases[:args.limit]
    report = {'startedAtUtc': datetime.now(timezone.utc).isoformat(),
              'scope': 'widget_selection_only',
              'note': 'Excludes destination/amount extraction and widget rendering. Synthetic cases, not overall accuracy. P95 is descriptive only for small samples.',
              'results': [], 'summary': {}}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    for repeat in range(args.repeats):
        for index, case in enumerate(cases):
            providers = args.providers if (index + repeat) % 2 == 0 else list(reversed(args.providers))
            for provider in providers:
                row = {'caseId': case['id'], 'repeat': repeat, 'provider': provider, 'expected': case['expected']}
                request = urllib.request.Request(args.base_url.rstrip('/') + '/api/experiments/widget-selection?provider=' + provider,
                        data=json.dumps({'message': case['message']}).encode('utf-8'), headers={'Content-Type': 'application/json'})
                started = time.perf_counter()
                try:
                    with urllib.request.urlopen(request, timeout=25) as response:
                        result = json.load(response)
                        row['requestId'] = response.headers.get('X-Request-Id')
                    row.update({k: result.get(k) for k in ['model', 'durationMs', 'inputTokens', 'outputTokens', 'needsReview', 'widgets']})
                    row['correct'] = set(row['widgets']) == set(case['expected'])
                    row['success'] = True
                except urllib.error.HTTPError as error:
                    row.update(success=False, correct=False, error='HTTP ' + str(error.code))
                except (urllib.error.URLError, TimeoutError, ValueError) as error:
                    row.update(success=False, correct=False, error=type(error).__name__)
                row['roundTripMs'] = round((time.perf_counter() - started) * 1000)
                report['results'].append(row)
                args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
                print(json.dumps({k: row.get(k) for k in ['caseId', 'provider', 'durationMs', 'correct', 'needsReview', 'error']}, ensure_ascii=False), flush=True)
    for provider in args.providers:
        rows = [r for r in report['results'] if r['provider'] == provider]
        good = [r for r in rows if r['success']]
        durations = sorted(r['durationMs'] for r in good)
        report['summary'][provider] = {
            'requests': len(rows), 'successful': len(good),
            'exactWidgetMatches': sum(r['correct'] for r in rows),
            'reviewNeeded': sum(bool(r.get('needsReview')) for r in good),
            'meanDurationMs': round(statistics.mean(durations), 1) if durations else None,
            'p95DurationMs': durations[max(0, math.ceil(len(durations) * .95) - 1)] if durations else None,
            'models': sorted({r['model'] for r in good}),
        }
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(report['summary'], ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
