package com.awakened;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Slf4j
public class FakeAxe
{
	// ── Manager-level static state ────────────────────────────────────────────
	public static final int NPC_AXE_STATIC = 12225;

	private static final List<List<WorldPoint>> AXE_PATHS = new ArrayList<>();
	private static final Set<WorldPoint> SPAWN_TILES = new HashSet<>();
	private static final List<FakeAxe> ACTIVE = new ArrayList<>();
	private static boolean hasScaled = false;

	// ── Per-instance fields ───────────────────────────────────────────────────
	private final RuneLiteObject runeLiteObject;
	private final List<WorldPoint> path;
	private final boolean reversed;
	private int currentIndex;
	private boolean moving;
	private int ticksUntilThrow;

	public FakeAxe(RuneLiteObject runeLiteObject, List<WorldPoint> path, boolean reversed)
	{
		this.runeLiteObject = runeLiteObject;
		this.path = path;
		this.reversed = reversed;
	}

	// ── Getters / setters ─────────────────────────────────────────────────────
	public RuneLiteObject getRuneLiteObject() { return runeLiteObject; }
	public List<WorldPoint> getPath()         { return path; }
	public boolean isReversed()               { return reversed; }
	public int getCurrentIndex()              { return currentIndex; }
	public void setCurrentIndex(int i)        { currentIndex = i; }
	public boolean isMoving()                 { return moving; }
	public void setMoving(boolean m)          { moving = m; }
	public int getTicksUntilThrow()           { return ticksUntilThrow; }
	public void setTicksUntilThrow(int t)     { ticksUntilThrow = t; }

	// ── Static: path initialisation ───────────────────────────────────────────
	public static void initPaths()
	{
		AXE_PATHS.clear();
		SPAWN_TILES.clear();

		// White path
		AXE_PATHS.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 36, 26, 0),
			WorldPoint.fromRegion(4405, 38, 26, 0),
			WorldPoint.fromRegion(4405, 40, 26, 0),
			WorldPoint.fromRegion(4405, 42, 26, 0),
			WorldPoint.fromRegion(4405, 44, 26, 0),
			WorldPoint.fromRegion(4405, 45, 26, 0)
		));

		// Red path
		AXE_PATHS.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 36, 31, 0),
			WorldPoint.fromRegion(4405, 38, 29, 0),
			WorldPoint.fromRegion(4405, 40, 27, 0),
			WorldPoint.fromRegion(4405, 42, 25, 0),
			WorldPoint.fromRegion(4405, 44, 23, 0),
			WorldPoint.fromRegion(4405, 45, 22, 0)
		));

		// Cyan path
		AXE_PATHS.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 41, 21, 0),
			WorldPoint.fromRegion(4405, 41, 23, 0),
			WorldPoint.fromRegion(4405, 41, 25, 0),
			WorldPoint.fromRegion(4405, 41, 27, 0),
			WorldPoint.fromRegion(4405, 41, 29, 0),
			WorldPoint.fromRegion(4405, 41, 30, 0)
		));

		// Green path
		AXE_PATHS.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 46, 31, 0),
			WorldPoint.fromRegion(4405, 44, 29, 0),
			WorldPoint.fromRegion(4405, 42, 27, 0),
			WorldPoint.fromRegion(4405, 40, 25, 0),
			WorldPoint.fromRegion(4405, 38, 23, 0),
			WorldPoint.fromRegion(4405, 37, 22, 0)
		));

		// White path (reverse)
		AXE_PATHS.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 46, 26, 0),
			WorldPoint.fromRegion(4405, 44, 26, 0),
			WorldPoint.fromRegion(4405, 42, 26, 0),
			WorldPoint.fromRegion(4405, 40, 26, 0),
			WorldPoint.fromRegion(4405, 38, 26, 0),
			WorldPoint.fromRegion(4405, 37, 26, 0)
		));

		// Red path (reverse)
		AXE_PATHS.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 46, 21, 0),
			WorldPoint.fromRegion(4405, 44, 23, 0),
			WorldPoint.fromRegion(4405, 42, 25, 0),
			WorldPoint.fromRegion(4405, 40, 27, 0),
			WorldPoint.fromRegion(4405, 38, 29, 0),
			WorldPoint.fromRegion(4405, 37, 30, 0)
		));

		// Cyan path (reverse)
		AXE_PATHS.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 41, 31, 0),
			WorldPoint.fromRegion(4405, 41, 29, 0),
			WorldPoint.fromRegion(4405, 41, 27, 0),
			WorldPoint.fromRegion(4405, 41, 25, 0),
			WorldPoint.fromRegion(4405, 41, 23, 0),
			WorldPoint.fromRegion(4405, 41, 22, 0)
		));

		// Green path (reverse)
		AXE_PATHS.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 36, 21, 0),
			WorldPoint.fromRegion(4405, 38, 23, 0),
			WorldPoint.fromRegion(4405, 40, 25, 0),
			WorldPoint.fromRegion(4405, 42, 27, 0),
			WorldPoint.fromRegion(4405, 44, 29, 0),
			WorldPoint.fromRegion(4405, 45, 30, 0)
		));

		for (List<WorldPoint> path : AXE_PATHS)
		{
			SPAWN_TILES.add(path.get(0));
		}

		log.info("[FakeAxe] initPaths: spawnTiles={}", SPAWN_TILES);
	}

	// ── Static: NPC check ─────────────────────────────────────────────────────
	public static boolean isAtAnySpawnTile(Client client, NPC npc)
	{
		return SPAWN_TILES.stream().anyMatch(t -> npcIsAtTile(client, npc, t));
	}

	/**
	 * Given the current gamestate, determine if any damage should be dealt to the player
	 *
	 * Returns 0 if the player should take no damage on this tick.
	 */
	public static int getDamage(Client client)
	{
		// Player position in scene (local) coordinates — instance-space, not template-space
		LocalPoint playerLp = client.getLocalPlayer().getLocalLocation();
		if (playerLp == null) return 0;

		int total = 0;
		for (FakeAxe axe : ACTIVE)
		{
			// Idle axes sit on their spawn tile; moving axes use their current path index
			int posIndex = axe.moving ? axe.currentIndex : 0;

			// toLocalPoint converts template WorldPoint → instance LocalPoint
			LocalPoint axeLp = toLocalPoint(client, axe.path.get(posIndex));
			if (axeLp == null) continue;

			if (Math.abs(playerLp.getSceneX() - axeLp.getSceneX()) <= 1 &&
				Math.abs(playerLp.getSceneY() - axeLp.getSceneY()) <= 1)
			{
				total += randomBetween(30, 50);
			}
		}
		return total;
	}

	// ── Static: spawn ─────────────────────────────────────────────────────────
	public static void spawnMissing(Client client)
	{
		if (!hasScaled)
		{
			client.loadModel(49300).scale(192, 192, 192);
			client.loadModel(49304).scale(192, 192, 192);
			hasScaled = true;
		}

		// Collect which real axe NPC is at each spawn tile (by local coordinate comparison)
		Map<WorldPoint, NPC> realAxeAtSpawn = new HashMap<>();
		int totalRealAxes = 0;
		for (NPC npc : client.getNpcs())
		{
			if (npc.getId() == NPC_AXE_STATIC)
			{
				totalRealAxes++;
				for (WorldPoint spawnTile : SPAWN_TILES)
				{
					if (npcIsAtTile(client, npc, spawnTile))
					{
						log.debug("[FakeAxe] Real axe id={} matched spawnTile={}", npc.getId(), spawnTile);
						realAxeAtSpawn.put(spawnTile, npc);
						break;
					}
				}
			}
		}

		log.debug("[FakeAxe] Found {} real axes, {} at spawn tiles", totalRealAxes, realAxeAtSpawn.size());

		WorldPoint center = WorldPoint.fromRegion(4405, 41, 26, 0);
		Map<WorldPoint, FakeAxe> newFakes = new HashMap<>();

		for (List<WorldPoint> path : AXE_PATHS)
		{
			WorldPoint spawnTile = path.get(0);
			if (realAxeAtSpawn.containsKey(spawnTile))
			{
				log.debug("[FakeAxe] Skipping {} — real axe present", spawnTile);
				continue;
			}

			LocalPoint lp = toLocalPoint(client, spawnTile);
			if (lp == null)
			{
				log.debug("[FakeAxe] LocalPoint null for {} — skipping", spawnTile);
				continue;
			}

			RuneLiteObject obj = client.createRuneLiteObject();
			obj.setModel(client.loadModel(49300));
			obj.setAnimation(client.loadAnimation(10364));
			obj.setOrientation(calculateOrientation(spawnTile, center));
			obj.setLocation(lp, client.getPlane());
			obj.setActive(true);

			FakeAxe fake = new FakeAxe(obj, path, false);
			fake.setCurrentIndex(0);
			fake.setTicksUntilThrow(randomBetween(2, 5));
			ACTIVE.add(fake);
			newFakes.put(spawnTile, fake);
			log.debug("[FakeAxe] Spawned fake at {} idx=0 reversed=false", spawnTile);
		}

		log.debug("[FakeAxe] Created {} new fakes, total={}", newFakes.size(), ACTIVE.size());
	}

	// ── Static: cleanup ───────────────────────────────────────────────────────
	public static void cleanupAll()
	{
		for (FakeAxe fake : ACTIVE)
		{
			fake.runeLiteObject.setActive(false);
		}
		ACTIVE.clear();
	}

	// ── Static: tick all ─────────────────────────────────────────────────────
	public static void tickAll(Client client)
	{
		ACTIVE.removeIf(fake -> fake.tick(client));
	}

	// ── Instance: per-tick state machine ─────────────────────────────────────
	/**
	 * Advances this axe by one game tick.
	 * @return true if the axe is exhausted and should be removed from ACTIVE
	 */
	public boolean tick(Client client)
	{
		if (!moving)
		{
			int ticks = ticksUntilThrow - 1;
			ticksUntilThrow = ticks;
			if (ticks > 1) {
				runeLiteObject.setModel(client.loadModel(49300));
				// Idle animation
				runeLiteObject.setAnimation(client.loadAnimation(10363));
			}
			if (ticks == 1)
			{
				// Pulling back to throw animation
				runeLiteObject.setModel(client.loadModel(49300));
				runeLiteObject.setAnimation(client.loadAnimation(10365));
				WorldPoint center = WorldPoint.fromRegion(4405, 41, 26, 0);
				runeLiteObject.setOrientation(calculateOrientation(path.get(0), center));
			}
			if (ticks <= 0)
			{
				moving = true;
			}
			return false;
		}

		// Thrown axe spinning through the air
		runeLiteObject.setModel(client.loadModel(49304));
		runeLiteObject.setAnimation(client.loadAnimation(10366));

		int idx = currentIndex + 1;
		if (idx < 0 || idx >= path.size())
		{
			runeLiteObject.setActive(false);
			runeLiteObject.setModel(null);
			return true;
		}

		currentIndex = idx;
		LocalPoint lp = toLocalPoint(client, path.get(idx));
		if (lp != null)
		{
			runeLiteObject.setLocation(lp, client.getPlane());
		}
		return false;
	}

	// ── Private static helpers ────────────────────────────────────────────────
	private static int randomBetween(int min, int max)
	{
		return new Random().nextInt(max - min + 1) + min;
	}

	private static int calculateOrientation(WorldPoint spawn, WorldPoint center)
	{
		int sx = spawn.getX(), sy = spawn.getY();
		int cx = center.getX(), cy = center.getY();
		if      (sx == cx && sy < cy) return 1024;
		else if (sx <  cx && sy < cy) return 1280;
		else if (sx <  cx && sy == cy) return 1536;
		else if (sx <  cx && sy > cy) return 1792;
		else if (sx == cx && sy > cy) return 0;
		else if (sx >  cx && sy > cy) return 256;
		else if (sx >  cx && sy == cy) return 512;
		else if (sx >  cx && sy < cy) return 768;
		return 0;
	}

	/**
	 * Converts a template WorldPoint to a LocalPoint, handling instanced regions.
	 * In an instanced region, WorldPoint.fromRegion gives template coords which
	 * LocalPoint.fromWorld can't resolve (scene base is in instance space).
	 * WorldPoint.toLocalInstance maps template → instance world coords first.
	 */
	private static LocalPoint toLocalPoint(Client client, WorldPoint templateWp)
	{
		Collection<WorldPoint> instances = WorldPoint.toLocalInstance(client, templateWp);
		if (!instances.isEmpty())
		{
			return LocalPoint.fromWorld(client, instances.iterator().next());
		}
		return LocalPoint.fromWorld(client, templateWp);
	}

	/**
	 * Compares an NPC's local position to a template tile's local position.
	 * Using scene (local) coordinates avoids the template vs instance world coord mismatch.
	 */
	private static boolean npcIsAtTile(Client client, NPC npc, WorldPoint templateTile)
	{
		LocalPoint npcLocal = npc.getLocalLocation();
		LocalPoint tileLocal = toLocalPoint(client, templateTile);
		return npcLocal != null && tileLocal != null
			&& npcLocal.getSceneX() == tileLocal.getSceneX()
			&& npcLocal.getSceneY() == tileLocal.getSceneY();
	}
}
