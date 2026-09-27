#!/usr/bin/env python3
"""Execute the production schema with host SQLite; not an Android SQLite/device test."""
from pathlib import Path
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
source = (root / 'engine-assets/src/main/kotlin/world/engine/assets/AssetDatabase.kt').read_text()
statements = re.findall(r'db\.execSQL\("(CREATE [^"]+)"\)', source)
assert len(statements) == 6
connection = sqlite3.connect(':memory:')
connection.execute('PRAGMA foreign_keys=ON')
for statement in statements:
    connection.execute(statement)

def add(identity, kind='IMAGE', recipe=None):
    connection.execute('INSERT INTO assets VALUES(?,?,?,?,?,?,?,?,?,?,?)', (identity, 'name', 'folder', kind, identity + '.png', 1, 64, 64, '[]', 0, recipe))

add('image', recipe='Vehicle:1')
add('prefab', 'PREFAB')
connection.execute('INSERT INTO edges VALUES(?,?)', ('prefab', 'image'))
connection.execute('INSERT INTO sheets VALUES(?,?)', ('image', '[]'))
connection.execute('UPDATE assets SET name=?,folder=?,favorite=? WHERE id=?', ('Renamed', 'Moved/Nested', 1, 'image'))
assert connection.execute('SELECT target FROM edges WHERE owner=?', ('prefab',)).fetchone() == ('image',)
connection.commit()
try:
    connection.execute('DELETE FROM assets WHERE id=?', ('image',))
except sqlite3.IntegrityError:
    connection.rollback()
else:
    raise AssertionError('Referenced asset deletion was not blocked')
try:
    add('duplicate-recipe', recipe='Vehicle:1')
except sqlite3.IntegrityError:
    connection.rollback()
else:
    raise AssertionError('Recipe uniqueness was not enforced')
connection.execute('DELETE FROM assets WHERE id=?', ('prefab',))
assert connection.execute('SELECT count(*) FROM edges').fetchone()[0] == 0
connection.execute('DELETE FROM assets WHERE id=?', ('image',))
assert connection.execute('SELECT count(*) FROM sheets').fetchone()[0] == 0
connection.execute('INSERT OR IGNORE INTO tombstones VALUES(?)', ('image',))
connection.execute('INSERT OR IGNORE INTO tombstones VALUES(?)', ('image',))
assert connection.execute('SELECT count(*) FROM tombstones').fetchone()[0] == 1
print('PASS: actual schema creation, stable UUID edges on metadata moves, reference-restricted deletion, recipe uniqueness, cascading cleanup and tombstones (host SQLite).')
