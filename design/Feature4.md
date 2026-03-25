# Summary

You are a developer tasked with implementing a Runescape plugin for the popular Runescape client, Runelite.

You can find the Runelite API documentation here: https://static.runelite.net/runelite-api/apidocs/

We are creating a plugin which will enhance boss fights in the game by adding in additional mechanics.

The fourth feature we are going to implement is spawning additional axes.

During the Vardorvis boss fight, axes can spawn randomly in 3-5 random positions, chosen from 8 possible
    starting locations. The file `tiles.json` contains relevant coordinate and label information which will
    be explained here.

In the json, a tile with the label `*` indicates a tile that an axe can spawn on. When an axe spawns,
    it will spend 3 ticks performing an animation of 'winding up', and then it will be thrown towards
    the other `*` tile that has the same color. Once it has been thrown, the axe will teleport to the
    next closest similarly colored tile in the `tiles.json` map once per tick.

These axes have two different NPC ID's, `12225` for the first 3 ticks when they appear and are static,
    and then `12227` when they are moving.

Once an axe reaches it's end tile (the `*` tile opposite of its' spawn), it will despawn.

# Success Criteria

1. When axes are spawned in, our plugin should fill all of the remaining unoccupied `*` tiles with
    fake axes. These fake axes should be visually identical to the real thing so the player can't
    distinguish the fake and real axes.
2. When the axes are thrown, our plugin should also cause the fake axes to be thrown.
3. When the axes reach their final destination and despawn, our plugin should also despawn the fake axes.
