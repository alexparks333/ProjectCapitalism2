# Mailbox

A mailbox anyone can make and put outside their home, so other players can leave them things.

## Making one
- Crafting table, anyone: a **Chest** on top of **two wooden fences** (any wood), in one column
  (`data/capitalism/recipe/mailbox.json`).

## Using it (`mail/MailboxBlock`, `MailboxBlockEntity`, `MailboxMenu`)
- Whoever puts it down **owns** it. A mailbox with no owner (from a command) belongs to the first player to open it.
- **The owner** opens it like a chest: 27 slots, take mail out, put things in.
- **Anyone else** gets the **mail slot**: three slots, and a red **Drop Mail In** button under them (grey while the
  slot is empty). Nothing is sent until they press it; then it all goes into the box and they can't see or take the
  owner's mail. It says how much room is left; what doesn't fit stays in the slot, and anything not sent goes back
  to them when they close it. The owner is told the moment mail is dropped in.
- When someone leaves mail, the owner (if online) gets a chat message ("✉ Sam left you mail (3 items).") and a chime.
- **The red flag** goes up while there's anything in the box and drops when it's empty (everyone sees it).
- **The count:** when the owner walks up (within 8 blocks) a number floats over it, "✉ 5": how many slots have
  something in them. Only the owner sees it.
- Only the owner can break it (`mail/MailEvents`): not other players, not even ops in survival; only an op in creative
  can take someone else's down. What's inside drops. Hoppers can't take from it or put in.

## The Town Hall Mailbox (`town_mailbox`, same classes: `MailboxBlockEntity.town()`)
- White enamel and gold on a marble plinth and fluted white post, gold bands, a gold finial, a crimson flag
  (`MailboxModel.TOWN_LAYER`, "town_mailbox" in `tools/BoatTex.cs`).
- Not craftable: an op in creative puts it down (in front of the Town Hall); nothing breaks it in survival.
- It belongs to the **Mayor and Vice Mayor** (`town/TownHall`), whoever holds those offices right now: they open
  it like a chest ("Town Hall Mail"), both are told when mail comes, and both see the count over it. Everyone else gets
  the mail slot ("Mail for the Town Hall").
- It's the town's, so it isn't locked to the op who placed it and its gold counts for no one on the leaderboard.

## Look (`client/MailboxModel`, `MailboxRenderer`, `MailboxScreen`)
- A navy, ribbed metal country mailbox with a rounded top on a wooden post, a door with a brass handle at the front,
  a brass name plate, and the red flag. Textures from `tools/BoatTex.cs` ("mailbox").
- The menu looks like an airmail envelope: red and blue striped edge, a stamp with a postmark.
- Photos: `gradlew runPhoto -Pphoto=mail`.
