// Writes the Pet Trainer's block assets and data (PetTrainer.md): the Small / Medium / Large Food Trough (copper, iron,
// Compacted Iron; empty / filled, 4 facings) and Water Bowl (water rising as it fills) block models and blockstates,
// their item definitions (the block itself, like any block in the inventory), loot tables, mining tags, lang names and
// job-only recipes. Textures: tools/make_pet_textures.ps1. Run from the project folder: node tools/make_pet_assets.js
const fs = require('fs'); const path = require('path');
const R = 'src/main/resources/';
const A = R + 'assets/capitalism/';
const D = R + 'data/';
const w = (p, o) => { fs.mkdirSync(path.dirname(p), { recursive: true }); fs.writeFileSync(p, JSON.stringify(o, null, 2) + '\n'); };
const rm = p => { if (fs.existsSync(p)) fs.unlinkSync(p); };
const box = (from, to, tex, faces = ['north', 'south', 'east', 'west', 'up', 'down']) =>
  ({ from, to, faces: Object.fromEntries(faces.map(f => [f, { texture: tex, ...(f === 'down' ? { cullface: 'down' } : {}) }])) });

// size: [title, metal texture, recipe material, trough box (x0, x1, z0, z1, height, wall), bowl box (from, to, height)]
const SIZES = {
  small: ['Small', 'feeder_copper', 'minecraft:copper_ingot', [3, 13, 4, 12, 5, 1], [4, 12, 4]],
  medium: ['Medium', 'feeder_iron', 'minecraft:iron_ingot', [1, 15, 3, 13, 6, 1], [3, 13, 5]],
  large: ['Large', 'feeder_compacted_iron', 'capitalism:compacted_iron', [0, 16, 1, 15, 8, 1.5], [1, 15, 6]],
};
const turn = { north: 0, east: 90, south: 180, west: 270 };

for (const [size, [title, metal, material, t, b]] of Object.entries(SIZES)) {
  // ── Food Trough: a long metal box along x, open on top; filled: a heap of food inside ──
  const [x0, x1, z0, z1, h, wall] = t;
  const trough = [
    box([x0 + wall, 0, z0 + wall], [x1 - wall, 1, z1 - wall], '#metal'),
    box([x0, 0, z0], [x1, h, z0 + wall], '#metal'),
    box([x0, 0, z1 - wall], [x1, h, z1], '#metal'),
    box([x0, 0, z0 + wall], [x0 + wall, h, z1 - wall], '#metal'),
    box([x1 - wall, 0, z0 + wall], [x1, h, z1 - wall], '#metal'),
  ];
  const food = box([x0 + wall, 1, z0 + wall], [x1 - wall, h - 1.5, z1 - wall], '#food', ['up', 'north', 'south', 'east', 'west']);
  const troughTex = { metal: `capitalism:block/${metal}`, food: 'capitalism:block/food_trough_food', particle: `capitalism:block/${metal}` };
  const id = `${size}_food_trough`;
  w(A + `models/block/${id}.json`, { parent: 'minecraft:block/block', textures: troughTex, elements: trough });
  w(A + `models/block/${id}_filled.json`, { parent: 'minecraft:block/block', textures: troughTex, elements: [...trough, food] });
  const troughStates = {};
  for (const [facing, y] of Object.entries(turn)) for (const filled of [false, true]) {
    troughStates[`facing=${facing},filled=${filled}`] = { model: `capitalism:block/${id}` + (filled ? '_filled' : ''), ...(y ? { y } : {}) };
  }
  w(A + `blockstates/${id}.json`, { variants: troughStates });
  w(A + `items/${id}.json`, { model: { type: 'minecraft:model', model: `capitalism:block/${id}_filled` } });

  // ── Water Bowl: a round metal bowl (traced from a circle on the 16x16 grid, one row of boxes per pixel row) with a
  // lip round its top, the water rising with how full it is (LEVEL 0-3: empty, a third, two thirds, full) ──
  const [f0, f1, bh] = b;
  const radius = (f1 - f0) / 2;
  const dist = (x, z) => Math.hypot(x + 0.5 - 8, z + 0.5 - 8);
  const inside = (x, z) => dist(x, z) < radius - 1.1;
  const side = (x, z) => !inside(x, z) && dist(x, z) < radius;
  const lip = (x, z) => dist(x, z) >= radius && dist(x, z) < radius + 0.7;
  // every run of cells in a row that pass the test, as boxes from y0 to y1
  const rows = (test, y0, y1, tex, faces) => {
    const out = [];
    for (let z = 0; z < 16; z++) {
      for (let x = 0; x < 16; x++) {
        if (!test(x, z) || (x > 0 && test(x - 1, z))) continue;
        let end = x;
        while (end + 1 < 16 && test(end + 1, z)) end++;
        out.push(box([x, y0, z], [end + 1, y1, z + 1], tex, faces));
      }
    }
    return out;
  };
  const water = top => rows(inside, 1, top, '#water', ['up']);
  const bowl = [
    ...rows(inside, 0, 1, '#metal'),
    ...rows(side, 0, bh, '#metal'),
    ...rows(lip, bh - 1, bh, '#metal'),
  ];
  const bowlTex = { metal: `capitalism:block/${metal}`, water: 'capitalism:block/water_bowl_water', particle: `capitalism:block/${metal}` };
  const bowlId = `${size}_water_bowl`;
  for (let step = 0; step <= 3; step++) {
    const top = 1 + (bh - 1.5) * step / 3;
    const elements = step === 0 ? bowl : [...bowl, ...water(top)];
    w(A + `models/block/${bowlId}_${step}.json`, { parent: 'minecraft:block/block', textures: bowlTex, elements });
  }
  const bowlStates = {};
  for (let level = 0; level <= 3; level++) bowlStates[`level=${level}`] = { model: `capitalism:block/${bowlId}_${level}` };
  w(A + `blockstates/${bowlId}.json`, { variants: bowlStates });
  w(A + `items/${bowlId}.json`, { model: { type: 'minecraft:model', model: `capitalism:block/${bowlId}_2` } });

  // ── Recipes (Pet Trainers only): a trough is upside-down pants with a Hay Bale in the middle; a bowl the same without
  // the top corners, with a Water Bucket in the middle (the bucket comes back empty) ──
  w(D + `capitalism/recipe/${id}.json`, {
    type: 'capitalism:job_shaped', job: 'pet_trainer', category: 'misc',
    key: { M: material, H: 'minecraft:hay_block' },
    pattern: ['M M', 'MHM', 'MMM'],
    result: { id: `capitalism:${id}` }
  });
  w(D + `capitalism/recipe/${bowlId}.json`, {
    type: 'capitalism:job_shaped', job: 'pet_trainer', category: 'misc',
    key: { M: material, W: 'minecraft:water_bucket' },
    pattern: ['MWM', 'MMM'],
    result: { id: `capitalism:${bowlId}` }
  });

  // ── Drops ──
  for (const block of [id, bowlId]) {
    w(D + `capitalism/loot_table/blocks/${block}.json`, {
      type: 'minecraft:block',
      pools: [{ condition: { type: 'minecraft:survives_explosion' }, entries: [{ type: 'minecraft:item', name: `capitalism:${block}` }], rolls: 1 }],
      random_sequence: `capitalism:blocks/${block}`
    });
  }
}

// ── The first single-size versions are gone ──
for (const old of ['food_trough', 'water_bowl']) {
  rm(A + `blockstates/${old}.json`); rm(A + `items/${old}.json`);
  rm(D + `capitalism/loot_table/blocks/${old}.json`); rm(D + `capitalism/recipe/${old}.json`);
}
for (const m of ['food_trough', 'food_trough_filled', 'water_bowl_level_0', 'water_bowl_level_1', 'water_bowl_level_2', 'water_bowl_level_3']) rm(A + `models/block/${m}.json`);

// ── Mining: all metal, pickaxe ──
for (const tool of ['axe', 'pickaxe']) {
  const p = D + `minecraft/tags/block/mineable/${tool}.json`;
  const tag = JSON.parse(fs.readFileSync(p, 'utf8'));
  tag.values = tag.values.filter(v => !/^capitalism:([a-z]+_)?(food_trough|water_bowl)$/.test(v));
  if (tool === 'pickaxe') for (const size of Object.keys(SIZES)) tag.values.push(`capitalism:${size}_food_trough`, `capitalism:${size}_water_bowl`);
  w(p, tag);
}

// ── Names ──
const langPath = A + 'lang/en_us.json'; const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
delete lang['block.capitalism.food_trough']; delete lang['block.capitalism.water_bowl'];
for (const [size, [title]] of Object.entries(SIZES)) {
  lang[`block.capitalism.${size}_food_trough`] = `${title} Food Trough`;
  lang[`block.capitalism.${size}_water_bowl`] = `${title} Water Bowl`;
}
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log('ok');
