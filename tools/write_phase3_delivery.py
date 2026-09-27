#!/usr/bin/env python3
"""Generate/check complete Phase 3 source and file inventory against the Phase 2 snapshot."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
baseline = json.loads((root / 'docs/PHASE3_BASELINE.json').read_text())
generated = {'docs/PHASE3_FILES.md', 'docs/PHASE3_SOURCE.md'}
tracked = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
paths = sorted({p for p in tracked if p and (root / p).is_file()} | generated)
hashes = {p: hashlib.sha256((root / p).read_bytes()).hexdigest() for p in paths if p not in generated}
status = {p: 'NEW' if p not in baseline else 'UNCHANGED' if hashes.get(p) == baseline[p] else 'CHANGED' for p in paths}
removed = sorted(set(baseline) - set(paths))
assert not removed, f'Unexpected removed baseline files: {removed}'
inventory = '# Phase 3 file inventory\n\nCompared with the captured end-of-Phase-2 SHA-256 baseline. Generated inventory/source files do not hash themselves.\n\n'
for group in ('NEW', 'CHANGED', 'UNCHANGED'):
    items = [p for p in paths if status[p] == group]
    inventory += f'## {group} — {len(items)} files\n\n| Path | SHA-256 |\n|---|---|\n'
    inventory += ''.join(f'| `{p}` | `{hashes.get(p, "generated; not self-hashed")}` |\n' for p in items) + '\n'
changed = [p for p in paths if status[p] != 'UNCHANGED' and p != 'docs/PHASE3_SOURCE.md']
source = '# Phase 3 — complete new/changed source\n\nGenerated from the actual repository files. Includes implementation, tests, configuration, licensing and documents in full; it does not recursively embed this appendix itself. The unchanged source remains in its original repository paths.\n\n'
source += ''.join(f'- `{p}` ({status[p]})\n' for p in changed) + '\n'
for p in changed:
    text = inventory if p == 'docs/PHASE3_FILES.md' else (root / p).read_text()
    language = 'kotlin' if p.endswith(('.kt', '.kts')) else {'.py': 'python', '.json': 'json', '.yml': 'yaml', '.xml': 'xml', '.md': 'markdown'}.get(Path(p).suffix, 'text')
    fence = '`' * max(4, max((len(line) + 1 for line in text.splitlines() if line and not line.strip('`')), default=4))
    source += f'## {p}\n\n{fence}{language}\n{text.rstrip()}\n{fence}\n\n'
for name, contents in (('PHASE3_FILES.md', inventory), ('PHASE3_SOURCE.md', source)):
    path = root / 'docs' / name
    if args.check:
        assert path.is_file() and path.read_text() == contents, f'{name} is stale; regenerate delivery documents'
    else:
        path.write_text(contents)
print(f'PASS: Phase 3 inventory and complete source snapshots ({len(changed)} full files plus the appendix itself).')
