#!/usr/bin/env python3
"""Expose bounded Gradle/test diagnostics in check annotations as well as log artifacts."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

path = Path(sys.argv[1])
if path.is_file():
    lines = path.read_text(errors='replace').splitlines()
    selected = [line for line in lines if line.startswith('e: ') or ' FAILED' in line]
    for index, line in enumerate(lines):
        if line.startswith('* What went wrong:'):
            selected.extend(lines[index:index + 20])
    for result in Path('.').glob('*/build/test-results/**/TEST-*.xml'):
        try:
            for failure in ET.parse(result).iter('failure'):
                selected.append(f'{result}: {failure.get("message", "")} {(failure.text or "")[:1200]}')
        except ET.ParseError:
            continue
    message = '\n'.join(selected or lines[-45:])[:18000]
    escaped = message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print('::error title=Build or test diagnostics::' + escaped)
else:
    print('::warning::Build log unavailable: toolchain setup failed before Gradle execution.')
