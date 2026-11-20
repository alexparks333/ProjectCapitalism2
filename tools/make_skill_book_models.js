// Writes items/skill_book.json (picks each book's own cover by its first CUSTOM_MODEL_DATA string; a lesson's second
// string, "i" or "ii", adds its level numeral in the corner: textures by tools/make_book_numerals.ps1), the per-book models,
// and lang names for every lesson effect. Run from src/main/resources. Keep the list matching librarian/Skill.
const fs = require('fs'); const path = require('path');
const w = (p, o) => { fs.mkdirSync(path.dirname(p), { recursive: true }); fs.writeFileSync(p, JSON.stringify(o, null, 2) + '\n'); };
const A = 'assets/capitalism/';
const lessons = {
  running_club: 'Running Club', mining_monthly: 'Mining Monthly', swimming_lessons: 'Swimming Lessons', soft_landings: 'Soft Landings',
  holding_your_breath: 'Holding Your Breath', anglers_almanac: 'The Angler’s Almanac', water_rationing: 'Water Rationing',
  butchery_basics: 'Butchery Basics', lumberjacks_log: 'Lumberjack’s Log', archery_drills: 'Archery Drills',
  horse_whispering: 'Horse Whispering', quiet_steps: 'Quiet Steps',
  fishermans_patience: 'Fisherman’s Patience', stonemasons_guide: 'Stonemason’s Guide', first_aid_basics: 'First Aid Basics',
  trail_rations: 'Trail Rations', beekeepers_notes: 'Beekeeper’s Notes', lucky_librarian: 'Lucky Librarian',
  merchants_ledger: 'Merchant’s Ledger', brawlers_handbook: 'The Brawler’s Handbook', reaching_further: 'Reaching Further',
  ice_in_the_pants: 'Ice In The Pants', hot_pepper: 'Hot Pepper'
};
const traits = ['iron_stomach', 'nimble', 'night_owl', 'bookworm', 'steady_aim', 'sure_footed', 'light_footed', 'deep_lungs',
  'born_miner', 'lucky_star', 'fast_learner', 'camel', 'used_to_hell', 'winter_survival'];
// Blood Moon Books: an effect (one night long), but no level numeral (the same from a Rough or a Delicate Book)
const blood = {
  revenants_bane: 'Revenant’s Bane', witchs_bane: 'Witch’s Bane', wolfsbane: 'Wolfsbane', blood_fury: 'Blood Fury', blood_ward: 'Blood Ward'
};
// instant books: no effect, no level numeral
const instants = ['fresh_start'];
const all = [...Object.keys(lessons), ...Object.keys(blood), ...instants, ...traits];
for (const id of all) w(A + `models/item/skill_books/${id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `capitalism:item/skill_books/${id}` } });
for (const id of Object.keys(lessons)) for (const n of ['i', 'ii']) w(A + `models/item/skill_books/${id}_${n}.json`,
  { parent: 'minecraft:item/generated', textures: { layer0: `capitalism:item/skill_books/${id}`, layer1: `capitalism:item/skill_books/numeral_${n}` } });
const model = id => ({ type: 'minecraft:model', model: `capitalism:item/skill_books/${id}` });
// a lesson: its numeral by the second string (books written before the numerals have none: plain cover)
const lessonModel = id => ({ type: 'minecraft:select', property: 'minecraft:custom_model_data', index: 1,
  cases: ['i', 'ii'].map(n => ({ when: n, model: model(`${id}_${n}`) })), fallback: model(id) });
w(A + 'items/skill_book.json', {
  model: {
    type: 'minecraft:select', property: 'minecraft:custom_model_data', index: 0,
    cases: all.map(id => ({ when: id, model: id in lessons ? lessonModel(id) : model(id) })),
    fallback: {
      type: 'minecraft:model', model: 'capitalism:item/skill_book',
      tints: [{ type: 'minecraft:custom_model_data', index: 0, default: 9067346 }, { type: 'minecraft:constant', value: 16777215 }]
    }
  }
});
const langPath = A + 'lang/en_us.json'; const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
for (const [id, name] of Object.entries({ ...lessons, ...blood })) lang[`effect.capitalism.book_${id}`] = name;
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log('ok ' + all.length);
