#!/usr/bin/env python3
"""Dependency-free repository sanity checks; not an Android compiler or device test."""
from pathlib import Path
import hashlib
import re
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[1]
settings = (root / 'settings.gradle.kts').read_text()
modules = re.findall(r'include\(":([\w-]+)"\)', settings)
assert len(modules) == len(set(modules)) == 21
for module in modules:
    build = root / module / 'build.gradle.kts'
    assert build.is_file(), module
    for dependency in re.findall(r'project\(":([\w-]+)"\)', build.read_text()):
        assert dependency in modules, dependency
for xml in root.glob('*/src/**/*.xml'):
    ET.parse(xml)
wrapper = root / 'gradle/wrapper/gradle-wrapper.jar'
assert hashlib.sha256(wrapper.read_bytes()).hexdigest() == 'cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8'
with zipfile.ZipFile(wrapper) as archive:
    assert archive.testzip() is None
    assert 'org/gradle/wrapper/GradleWrapperMain.class' in archive.namelist()
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
assert 'uses-permission' not in manifest
assert '0x00030000' in manifest
for source in root.glob('*/src/**/*.kt'):
    text = source.read_text()
    assert 'TODO' not in text and 'NotImplementedError' not in text, source
assert len(list(root.glob('*/src/test/**/*.kt'))) == 7
assert len(list(root.glob('*/src/androidTest/**/*.kt'))) == 6
asset_build=(root / 'engine-assets/build.gradle.kts').read_text()
assert 'kotlin("plugin.serialization")' in asset_build
assert 'api(project(":engine-assets"))' in (root / 'editor-assets/build.gradle.kts').read_text()
assert 'api(project(":editor-assets"))' in (root / 'editor-ui/build.gradle.kts').read_text()
generator=(root / 'engine-assets/src/main/kotlin/world/engine/assets/ProceduralAssets.kt').read_text()
counts=re.findall(r'"(?:Character|Enemy|NPC|Animal|Vehicle|Building|Nature|Prop|Texture)" to (\d+)', generator)
assert sum(map(int,counts))==335
assert 'org.jbox2d:jbox2d-library:2.2.1.1' in (root / 'engine-physics/build.gradle.kts').read_text()
assert 'api(project(":engine-animation"))' in (root / 'editor-viewport/build.gradle.kts').read_text()
print(f'PASS: {len(modules)} modules, project references, XML, official wrapper integrity, permission policy, and test-source presence.')
print('Android compilation, JUnit execution, Compose tests and GLES device tests are NOT covered by this check.')
