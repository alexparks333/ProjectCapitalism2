// Writes data/capitalism/structure/empty.nbt: an empty 5x5x5 structure for the game tests (librarian/SkillBookTests).
// Run from the project root: node tools/make_test_structure.js
const fs = require('fs'); const zlib = require('zlib'); const path = require('path');
const DATA_VERSION = 5023; // Minecraft 26.3 (version.json "world_version")
const parts = [];
const u8 = (v) => parts.push(Buffer.from([v]));
const i16 = (v) => { const b = Buffer.alloc(2); b.writeInt16BE(v); parts.push(b); };
const i32 = (v) => { const b = Buffer.alloc(4); b.writeInt32BE(v); parts.push(b); };
const str = (s) => { const b = Buffer.from(s, 'utf8'); i16(b.length); parts.push(b); };
const tag = (type, name) => { u8(type); str(name); };
// root compound
tag(10, '');
tag(3, 'DataVersion'); i32(DATA_VERSION);
tag(9, 'size'); u8(3); i32(3); i32(5); i32(5); i32(5);
tag(9, 'palette'); u8(10); i32(1); tag(8, 'Name'); str('minecraft:air'); u8(0);
tag(9, 'blocks'); u8(10); i32(0);
tag(9, 'entities'); u8(10); i32(0);
u8(0);
const out = path.join(__dirname, '..', 'src', 'main', 'resources', 'data', 'capitalism', 'structure', 'empty.nbt');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, zlib.gzipSync(Buffer.concat(parts)));
console.log('wrote ' + out);
