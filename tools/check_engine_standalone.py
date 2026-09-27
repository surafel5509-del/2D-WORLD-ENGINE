#!/usr/bin/env python3
"""Compile engine modules and execute core checks without Gradle/Compose.

Diagnostic only: uses Kotlin 1.9.23 from kotlin-jupyter-kernel 0.12.0.322,
not the app's pinned Gradle/Kotlin 1.9.24 toolchain. No Android runtime tests.
Install optional tools with: pip install jdk4py==17.0.9.2 kotlin-jupyter-kernel==0.12.0.322
Provide an actual Android API 34 android.jar with --android-jar.
"""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile
import shutil
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--android-jar', type=Path, required=True)
parser.add_argument('--keep-output', type=Path, help='Optional external cache directory for diagnostic JARs')
parser.add_argument('--jbox2d', type=Path, required=True, help='JBox2D library 2.2.1.1 JAR (Maven or diagnostic cache)')
parser.add_argument('--java-home', type=Path)
parser.add_argument('--jars', type=Path)
args = parser.parse_args()
if args.java_home is None:
    import jdk4py
    args.java_home = Path(jdk4py.JAVA_HOME)
if args.jars is None:
    import importlib.util
    spec = importlib.util.find_spec('run_kotlin_kernel')
    if spec is None:
        raise SystemExit('Install the documented kotlin-jupyter-kernel version or provide --jars')
    args.jars = Path(next(iter(spec.submodule_search_locations))) / 'jars'
root = Path(__file__).resolve().parents[1]
fat = args.jars / 'kotlin-jupyter-kernel-0.12.0-322-all.jar'
assert fat.is_file() and args.android_jar.is_file() and args.jbox2d.is_file()
jars = sorted(p for p in args.jars.glob('*.jar') if p != fat) + [fat]
classpath = os.pathsep.join(map(str, jars))
java = str(args.java_home / 'bin/java')
modules = ('engine-math', 'engine-core', 'engine-assets', 'engine-io', 'engine-animation', 'engine-physics', 'engine-render', 'editor-viewport')
with tempfile.TemporaryDirectory(prefix='world-engine-check-') as directory:
    work = Path(directory)
    # Load only the genuine serialization plugin from the bundled compiler;
    # avoid registering the notebook scripting plugin a second time.
    plugin = work / 'serialization-plugin.jar'
    with zipfile.ZipFile(plugin, 'w') as archive:
        archive.writestr('META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar', 'org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar\n')
        archive.writestr('META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor', 'org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginOptions\n')
    compiler = [java, '-cp', classpath, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-Xplugin=' + str(plugin)]
    artifacts = [str(args.jbox2d)]
    for module in modules:
        output = work / (module + '.jar')
        sources = sorted(str(p) for p in (root / module / 'src/main').rglob('*.kt'))
        target_classpath = os.pathsep.join([classpath, str(args.android_jar), *artifacts])
        subprocess.run([*compiler, '-classpath', target_classpath, '-d', str(output), *sources], check=True)
        artifacts.append(str(output))
        if args.keep_output:
            args.keep_output.mkdir(parents=True, exist_ok=True)
            shutil.copy2(output, args.keep_output / output.name)
        print('PASS: separate-module compile ' + module, flush=True)
    smoke = work / 'core-smoke.jar'
    runtime_classpath = os.pathsep.join([*artifacts, classpath])
    subprocess.run([*compiler, '-classpath', runtime_classpath, '-d', str(smoke), str(root / 'tools/CoreSmoke.kt'), str(root / 'tools/RuntimeSmoke.kt')], check=True)
    subprocess.run([java, '-cp', str(smoke) + os.pathsep + runtime_classpath, 'CoreSmokeKt'], check=True)
    subprocess.run([java, '-ea', '-cp', str(smoke) + os.pathsep + runtime_classpath + os.pathsep + str(args.android_jar), 'RuntimeSmokeKt'], check=True)
print('PASS: standalone engine compilation. This is NOT a Compose/app build or an APK test.')
