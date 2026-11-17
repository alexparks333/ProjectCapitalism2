// Writes the Knowledge Creator / Knowledge Piece / Whisperer JSON assets and data. Run from the project root:
//   node tools/make_knowledge_assets.js
const fs = require('fs'); const path = require('path');
const R = 'src/main/resources/';
const A = R + 'assets/capitalism/'; const D = R + 'data/capitalism/';
const w = (p, o) => { fs.mkdirSync(path.dirname(p), { recursive: true }); fs.writeFileSync(p, JSON.stringify(o, null, 2) + '\n'); };

// a box with explicit uvs on every face (taken from where it sits in the block)
function box(from, to, tex, cull) {
  const [x1, y1, z1] = from, [x2, y2, z2] = to; const t = (k) => typeof tex === 'string' ? tex : tex[k];
  const f = {
    north: { uv: [16 - x2, 16 - y2, 16 - x1, 16 - y1], texture: t('north') }, south: { uv: [x1, 16 - y2, x2, 16 - y1], texture: t('south') },
    east: { uv: [16 - z2, 16 - y2, 16 - z1, 16 - y1], texture: t('east') }, west: { uv: [z1, 16 - y2, z2, 16 - y1], texture: t('west') },
    up: { uv: [x1, z1, x2, z2], texture: t('up') }, down: { uv: [x1, 16 - z2, x2, 16 - z1], texture: t('down') }
  };
  for (const k of Object.keys(f)) for (let i = 0; i < 4; i++) f[k].uv[i] = Math.max(0, Math.min(16, f[k].uv[i]));
  if (cull) for (const k of cull) f[k].cullface = k;
  return { from, to, faces: f };
}

// Knowledge Creator: a dark pedestal, a glass bell jar with a deepslate lid and an amethyst knob, amethyst on the corners
const v = {};
for (const [facing, y] of Object.entries({ north: 0, east: 90, south: 180, west: 270 })) {
  for (const working of ['false', 'true']) { const m = { model: 'capitalism:block/knowledge_creator' }; if (y) m.y = y; v[`facing=${facing},working=${working}`] = m; }
}
w(A + 'blockstates/knowledge_creator.json', { variants: v });
const corners = [[0.5, 0.5], [13.5, 0.5], [0.5, 13.5], [13.5, 13.5]];
w(A + 'models/block/knowledge_creator.json', {
  parent: 'minecraft:block/block',
  textures: {
    particle: 'capitalism:block/knowledge_creator_side', side: 'capitalism:block/knowledge_creator_side',
    top: 'capitalism:block/knowledge_creator_top', bottom: 'minecraft:block/polished_deepslate',
    glass: 'minecraft:block/glass', lid: 'minecraft:block/polished_deepslate', amethyst: 'minecraft:block/amethyst_block'
  },
  elements: [
    box([0, 0, 0], [16, 9, 16], { north: '#side', south: '#side', east: '#side', west: '#side', up: '#top', down: '#bottom' }, ['north', 'south', 'east', 'west', 'down']),
    box([3, 9, 3], [13, 15, 13], '#glass'),
    box([4, 15, 4], [12, 16, 12], '#lid'),
    box([7, 16, 7], [9, 17, 9], '#amethyst'),
    ...corners.map(([x, z]) => box([x, 9, z], [x + 2, 10.5, z + 2], '#amethyst'))
  ]
});
w(A + 'items/knowledge_creator.json', { model: { type: 'minecraft:model', model: 'capitalism:block/knowledge_creator' } });
w(D + 'loot_table/blocks/knowledge_creator.json', {
  type: 'minecraft:block', pools: [{ condition: { type: 'minecraft:survives_explosion' }, entries: [{ type: 'minecraft:item', name: 'capitalism:knowledge_creator' }], rolls: 1 }],
  random_sequence: 'capitalism:blocks/knowledge_creator'
});
w(D + 'recipe/knowledge_creator.json', {
  type: 'capitalism:job_shaped', job: 'librarian', category: 'misc',
  key: { B: 'minecraft:book', F: 'minecraft:feather', W: '#minecraft:planks', I: 'minecraft:iron_ingot' },
  pattern: ['BFB', 'WWW', 'III'], result: { id: 'capitalism:knowledge_creator' }
});
const tagPath = R + 'data/minecraft/tags/block/mineable/pickaxe.json';
const tag = fs.existsSync(tagPath) ? JSON.parse(fs.readFileSync(tagPath, 'utf8')) : { values: [] };
if (!tag.values.includes('capitalism:knowledge_creator')) tag.values.push('capitalism:knowledge_creator');
w(tagPath, tag);

// Knowledge Piece: its look by quality (the CUSTOM_MODEL_DATA string; plain = Faded)
for (const q of ['clear', 'bright', 'radiant']) {
  w(A + `models/item/knowledge_piece_${q}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `capitalism:item/knowledge_piece_${q}` } });
}
w(A + 'items/knowledge_piece.json', {
  model: {
    type: 'minecraft:select', property: 'minecraft:custom_model_data', index: 0,
    cases: ['clear', 'bright', 'radiant'].map(q => ({ when: q, model: { type: 'minecraft:model', model: `capitalism:item/knowledge_piece_${q}` } })),
    fallback: { type: 'minecraft:model', model: 'capitalism:item/knowledge_piece' }
  }
});

// no more Knowledge Pieces in chests
for (const p of [D + 'loot_modifiers/knowledge_pieces.json', D + 'loot_table/chests/knowledge_pieces.json']) if (fs.existsSync(p)) fs.unlinkSync(p);

const langPath = A + 'lang/en_us.json'; const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
Object.assign(lang, {
  'block.capitalism.knowledge_creator': 'Knowledge Creator',
  'entity.capitalism.whisperer': 'The Whisperer'
});
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log('ok');
