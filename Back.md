# The Back slot

One more place on every player's body, for things worn on the back. Code: `back/` (`Back` = the rules and the
attachment's value, `BackSlot` = the slot), `mixin/InventoryMenuMixin` (adds it to the inventory),
`mixin/CreativeBackSlotMixin` (places it in the creative inventory), `client/BjornLayer` (draws what's worn).

- **Where:** in your inventory, just above the offhand slot, to the right of your player (inventory slot 46, x 77 y 44).
  The vanilla inventory picture has no slot there, so its frame is drawn on (`client/BackSlotFrame`,
  `mixin/InventoryScreenMixin`); empty, it shows a backpack outline (`textures/gui/sprites/container/slot/back.png`) and
  hovering it says "Back: Wear a bag or an Animal Bjorn here". In the creative inventory it's left of the offhand slot.
- **What fits:** one item from the item tag **`capitalism:back_items`** (`data/capitalism/tags/item/back_items.json`). Now:
  the **Animal Bjorn** (`PetTrainer.md`) and the **Designer's bags** (Backpack, Food Satchel, Seed Satchel: `Designer.md`;
  a new bag goes in the tag too).
- **A worn bag adds slots** (a leather pouch beside the inventory, `designer/BagSlot`, `client/BagPanel`). Its things
  only stay in it while it's worn: taking it off **spills them on the ground** (`Back.set` and `BackSlot.remove` call
  `designer/Bags.spill`). Dying drops them and **destroys the bag**. `Back.peek` reads the slot without creating its data
  (the bag slots ask while a player is still being made).
- **Saved** with the player (`ModAttachments.BACK`) and **synced to everyone who can see them**, so what's on your back
  shows on you (`client/BjornLayer`).
- **Dying** drops it like the rest of your things; with keepInventory you keep it.

## Testing
`pets/PetTests` > `pet_bjorn`: the inventory has the Back slot (slot 46), only `back_items` fit, and dying drops it.
