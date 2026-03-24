# Summary

You are a developer tasked with implementing a Runescape plugin for the popular Runescape client, Runelite.

You can find the Runelite API documentation here: https://static.runelite.net/runelite-api/apidocs/

We are creating a plugin which will enhance boss fights in the game by adding in additional mechanics.

The second feature we are going to implement is a modification to the in game warning before fighting a boss
    with an Awakener's Orb (which we have now renamed to the `Awakaner's Cube`.

The first boss we are going to modify is `Vardorvis`. When clicking on the rubble of rocks to enter the
    Vardorvis arena, the game checks for an `Awakener's Orb` in the player's inventory.

If an orb is detected, the following message is shown:

<RED TEXT> Consume the awakener's orb to awaken Vardorvis? <END RED TEXT>

            Yes.
            No.

# Success Criteria

1. The orb detection message has been replaced with the following:

<RED TEXT> Consume the <RAINBOW TEXT> AWAKENER'S CUBE <END RAINBOW TEXT> to awaken <RAINBOW TEXT> MEGA VARDORVIS <END RAINBOW TEXT> ? <END RED TEXT>

            Yes. moo.
            No. omg.
