#!/usr/bin/env python3
"""Summarize actual JUnit XML in GitHub check annotations, without downloading artifacts."""
from collections import defaultdict
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

kind = sys.argv[1]
assert kind in {'unit', 'device'}
pattern = '*/build/test-results/**/TEST-*.xml' if kind == 'unit' else '*/build/outputs/androidTest-results/**/TEST-*.xml'
counts = defaultdict(lambda: [0, 0, 0])
for path in sorted(Path('.').glob(pattern)):
    root = ET.parse(path).getroot()
    cases = list(root.iter('testcase'))
    counts[path.parts[0]][0] += len(cases)
    counts[path.parts[0]][1] += sum(case.find('failure') is not None or case.find('error') is not None for case in cases)
    counts[path.parts[0]][2] += sum(case.find('skipped') is not None for case in cases)
message = '; '.join(f'{module}: {total} tests, {failed} failures, {skipped} skipped' for module, (total, failed, skipped) in counts.items())
print(f'::notice title=Executed {kind} tests::{message or "No JUnit XML produced"}')
assert sum(c[0] for c in counts.values()) > 0, 'No test cases were discovered in result XML'
assert all(c[1] == 0 for c in counts.values()), 'Test failures were recorded'
