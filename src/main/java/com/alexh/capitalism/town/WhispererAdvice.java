package com.alexh.capitalism.town;

import java.util.List;

// The Whisperer's "pieces of advice" (Town.md): for 1 gold he tells you one of these, in his own voice. Never the same
// one twice in a row. Right now they're all riddles about Molten Socket gems (Sockets.md): hints, never plain answers,
// no numbers. More kinds get added over time. Both sides use this list (the menu syncs which one).
public final class WhispererAdvice {

    public static final long PRICE_NUGGETS = 9;   // 1 gold

    public static final List<String> TIPS = List.of(
        "The strongest beasts sometimes drop a little molten heart. A smith who knows your gear can set it.",
        "Sink the sea-blue stone into a blade, and every foe you fell teaches you a little more.",
        "A miner who carries the sea-blue stone learns something from every rock he breaks.",
        "Red dust in a blade's heart... the arm forgets how to tire between swings.",
        "The red dust hums in whatever holds it. Feet hurry. Picks hurry.",
        "Wear the merchant's green against your skin, and the town folk soften their prices.",
        "A green-eyed blade has a nose for the rarest spoils.",
        "Green in the pick, and now and then the ore gives up something... denser.",
        "White stone from the Nether makes a lucky strike land like a falling anvil.",
        "Stone gives way quicker to a pick that remembers the Nether.",
        "The ocean's shard wakes a blade up the moment it goes under the waves.",
        "Miners who dig beneath the water swear by a shard of the sea.",
        "Wear the sea, and the sea carries you.",
        "A helm that listens to the deep dark hears the monsters breathing behind the walls.",
        "Bone remembers the arrow. Wear it, and arrows forget you a little.",
        "A butcher's blade with bone in its heart always finds the best cut.",
        "A blade with winter inside... sometimes the cold stays in the wound.",
        "Carry winter on your back and the sun can't find you. Neither can the warmth.",
        "A tool with fire in its heart never walks into the dark alone.",
        "Fire in an axe, and the forest comes down already burnt.",
        "Wear the Nether's fire and frost forgets your name. The desert won't.",
        "The purple shard sips from every life your blade ends, and shares a little with you.",
        "Wrap yourself in purple crystal, and even the hardest blow barely moves you.",
        "Strike with a sparkling blade, and your quarry can't hide in any shadow.",
        "The sorrow of the Nether bites hardest at the Nether's own children.",
        "Slime in your boots, and the ground forgives every fall.",
        "An old charm sewn into your armor... the river starts handing you its rarest fish.",
        "A diamond edge doesn't just fell a tree. It does the carpenter's work too.",
        "Old flint in an axe leaves wounds that keep on weeping."
    );

    private WhispererAdvice() {
    }
}
