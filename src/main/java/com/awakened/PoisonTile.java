package com.awakened;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PoisonTile
{
	// TODO: Replace with confirmed ground blob model ID
	private static final int POISON_MODEL_ID = 57726;

	// ── Static manager state ──────────────────────────────────────────────────
	private static final List<PoisonTile> ACTIVE = new ArrayList<>();
	private static WorldPoint lastPlayerTile = null;

	// ── Per-instance fields ───────────────────────────────────────────────────
	private final RuneLiteObject obj;
	private final WorldPoint tile;
	private int ticksRemaining;

	private static boolean hasScaled = false;

	private PoisonTile(RuneLiteObject obj, WorldPoint tile, int duration)
	{
		this.obj = obj;
		this.tile = tile;
		this.ticksRemaining = duration;
	}

	// ── Public static API ─────────────────────────────────────────────────────

	/**
	 * Called every game tick. Detects player movement, spawns a blob on the
	 * vacated tile, then advances all existing blob countdowns.
	 */
	public static void tickAll(Client client, int duration)
	{
		WorldPoint current = client.getLocalPlayer().getWorldLocation();

		if (lastPlayerTile != null && !current.equals(lastPlayerTile))
		{
			spawnAt(client, lastPlayerTile, duration);
		}
		lastPlayerTile = current;

		ACTIVE.removeIf(PoisonTile::tick);

		if (!hasScaled)
		{
			client.loadModel(POISON_MODEL_ID).scale(64, 64, 64);
			hasScaled = true;
		}
	}

	/**
	 * Deactivates and clears all active blobs. Call from plugin shutDown.
	 */
	public static void cleanupAll()
	{
		for (PoisonTile pt : ACTIVE)
		{
			pt.obj.setActive(false);
		}
		ACTIVE.clear();
		lastPlayerTile = null;
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	private static void spawnAt(Client client, WorldPoint tile, int duration)
	{
		// If a blob already exists at this tile, reset its duration instead of stacking.
		for (PoisonTile pt : ACTIVE)
		{
			if (pt.tile.equals(tile))
			{
				pt.ticksRemaining = duration;
				log.debug("[PoisonTile] Reset duration at {}", tile);
				return;
			}
		}

		LocalPoint lp = LocalPoint.fromWorld(client, tile);
		if (lp == null)
		{
			log.debug("[PoisonTile] LocalPoint null for {} — skipping", tile);
			return;
		}

		RuneLiteObject obj = client.createRuneLiteObject();
		obj.setModel(client.loadModel(POISON_MODEL_ID));
		obj.setLocation(lp, client.getPlane());
		obj.setActive(true);

		ACTIVE.add(new PoisonTile(obj, tile, duration));
		log.debug("[PoisonTile] Spawned at {} duration={}", tile, duration);
	}

	/**
	 * Advances this tile's countdown by one tick.
	 * @return true when expired (caller should remove from ACTIVE)
	 */
	private boolean tick()
	{
		ticksRemaining--;
		if (ticksRemaining <= 0)
		{
			obj.setActive(false);
			log.debug("[PoisonTile] Expired at {}", tile);
			return true;
		}
		return false;
	}
}
