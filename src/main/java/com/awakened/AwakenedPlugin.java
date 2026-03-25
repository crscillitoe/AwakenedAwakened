package com.awakened;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostItemComposition;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "Awakener's Cube"
)
public class AwakenedPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private AwakenedConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AwakenedItemOverlay itemOverlay;

	@Inject
	private ChatMessageManager chatMessageManager;

	private static final String[] RAINBOW_COLORS = {
		"ff0000", "ffff00", "00ff00", "00ffff", "0000ff"
	};

	private static final int NPC_AXE_STATIC = 12225;
	private static final int NPC_AXE_MOVING = 12227;

	private boolean dialogActive = false;
	private int rainbowOffset = 0;

	private final List<List<WorldPoint>> axePaths = new ArrayList<>();
	private final Set<WorldPoint> spawnTiles = new HashSet<>();
	private final List<FakeAxe> fakes = new ArrayList<>();
	private boolean pendingFakeSpawn = false;
	private Model movingAxeModel = null;
	private int movingAxeAnim = -1;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Example started!");
		overlayManager.add(itemOverlay);
		initPaths();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Example stopped!");
		overlayManager.remove(itemOverlay);
		cleanupAllFakes();
		axePaths.clear();
		spawnTiles.clear();
	}

	private void initPaths()
	{
		axePaths.clear();
		spawnTiles.clear();

		// White path
		axePaths.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 36, 26, 0),
			WorldPoint.fromRegion(4405, 38, 26, 0),
			WorldPoint.fromRegion(4405, 40, 26, 0),
			WorldPoint.fromRegion(4405, 42, 26, 0),
			WorldPoint.fromRegion(4405, 44, 26, 0),
			WorldPoint.fromRegion(4405, 46, 26, 0)
		));

		// Red path
		axePaths.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 36, 31, 0),
			WorldPoint.fromRegion(4405, 38, 29, 0),
			WorldPoint.fromRegion(4405, 40, 27, 0),
			WorldPoint.fromRegion(4405, 42, 25, 0),
			WorldPoint.fromRegion(4405, 44, 23, 0),
			WorldPoint.fromRegion(4405, 46, 21, 0)
		));

		// Cyan path
		axePaths.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 41, 21, 0),
			WorldPoint.fromRegion(4405, 41, 23, 0),
			WorldPoint.fromRegion(4405, 41, 25, 0),
			WorldPoint.fromRegion(4405, 41, 27, 0),
			WorldPoint.fromRegion(4405, 41, 29, 0),
			WorldPoint.fromRegion(4405, 41, 31, 0)
		));

		// Green path
		axePaths.add(Arrays.asList(
			WorldPoint.fromRegion(4405, 46, 31, 0),
			WorldPoint.fromRegion(4405, 44, 29, 0),
			WorldPoint.fromRegion(4405, 42, 27, 0),
			WorldPoint.fromRegion(4405, 40, 25, 0),
			WorldPoint.fromRegion(4405, 38, 23, 0),
			WorldPoint.fromRegion(4405, 36, 21, 0)
		));

		for (List<WorldPoint> path : axePaths)
		{
			spawnTiles.add(path.get(0));
			spawnTiles.add(path.get(5));
		}

		log.info("[FakeAxe] initPaths: spawnTiles={}", spawnTiles);
	}

	private void dbg(String msg)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage("<col=ff6600>[FakeAxe] " + msg + "</col>")
			.build());
	}

	/**
	 * Converts a template WorldPoint to a LocalPoint, handling instanced regions.
	 * In an instanced region, WorldPoint.fromRegion gives template coords which
	 * LocalPoint.fromWorld can't resolve (scene base is in instance space).
	 * WorldPoint.toLocalInstance maps template → instance world coords first.
	 */
	private LocalPoint toLocalPoint(WorldPoint templateWp)
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
	private boolean npcIsAtTile(NPC npc, WorldPoint templateTile)
	{
		LocalPoint npcLocal = npc.getLocalLocation();
		LocalPoint tileLocal = toLocalPoint(templateTile);
		return npcLocal != null && tileLocal != null
			&& npcLocal.getSceneX() == tileLocal.getSceneX()
			&& npcLocal.getSceneY() == tileLocal.getSceneY();
	}

	private void cleanupAllFakes()
	{
		for (FakeAxe fake : fakes)
		{
			fake.getRuneLiteObject().setActive(false);
		}
		fakes.clear();
		pendingFakeSpawn = false;
		movingAxeModel = null;
		movingAxeAnim = -1;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		int id = npc.getId();

		if (id == NPC_AXE_STATIC)
		{
			boolean atSpawn = spawnTiles.stream().anyMatch(t -> npcIsAtTile(npc, t));

			if (atSpawn)
			{
				spawnMissingFakes();
			}
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		int oldId = event.getOld().getId();
		int newId = npc.getId();

		// Capture the moving axe model/anim once for use when fakes start moving
		if (newId == NPC_AXE_MOVING)
		{
			movingAxeModel = npc.getModel();
			movingAxeAnim = npc.getAnimation();
			dbg("Captured moving axe model/anim=" + movingAxeAnim);
		}
	}

	private void startMovingFake(FakeAxe fake)
	{
		NPC target = null;
		for (NPC npc : client.getNpcs())
		{
			if (npc.getId() == NPC_AXE_MOVING)
			{
				target = npc;
				break;
			}
			if (npc.getId() == NPC_AXE_STATIC && target == null)
			{
				target = npc;
			}
		}

		if (target != null)
		{
			Model model = buildNpcModel(target);
			if (model != null)
			{
				fake.getRuneLiteObject().setModel(model);
			}
			int animId = target.getAnimation();
			if (animId != -1)
			{
				fake.getRuneLiteObject().setAnimation(client.loadAnimation(animId));
			}
		}
		fake.setMoving(true);
	}

	private Model buildNpcModel(NPC npc)
	{
		NPCComposition comp = npc.getTransformedComposition();
		if (comp == null)
		{
			comp = npc.getComposition();
		}
		if (comp == null)
		{
			return null;
		}

		int[] modelIds = comp.getModels();
		if (modelIds == null || modelIds.length == 0)
		{
			return null;
		}

		ModelData[] datas = new ModelData[modelIds.length];
		for (int i = 0; i < modelIds.length; i++)
		{
			datas[i] = client.loadModelData(modelIds[i]);
		}

		ModelData merged = client.mergeModels(datas, datas.length);

		short[] colorToReplace = comp.getColorToReplace();
		short[] colorToReplaceWith = comp.getColorToReplaceWith();
		if (colorToReplace != null && colorToReplaceWith != null && colorToReplace.length > 0)
		{
			merged = merged.cloneColors();
			for (int i = 0; i < colorToReplace.length; i++)
			{
				merged.recolor(colorToReplace[i], colorToReplaceWith[i]);
			}
		}

		return merged.light();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		moveAxes();
		rainbowDialog();
	}

	private void rainbowDialog()
	{
		if (!dialogActive)
		{
			return;
		}

		Widget container = client.getWidget(InterfaceID.DIALOG_OPTION, 1);
		if (container == null || container.isHidden())
		{
			dialogActive = false;
			return;
		}

		Widget[] nested = container.getChildren();
		if (nested == null || nested.length < 3)
		{
			dialogActive = false;
			return;
		}

		rainbowOffset++;
		applyDialogText(nested);
	}

	private void moveAxes()
	{
		List<FakeAxe> toRemove = new ArrayList<>();

		for (FakeAxe fake : fakes)
		{
			if (!fake.isMoving())
			{
				int ticks = fake.getTicksUntilThrow() - 1;
				fake.setTicksUntilThrow(ticks);
				if (ticks <= 0)
				{
					startMovingFake(fake);
				}
				continue;
			}

			List<WorldPoint> path = fake.getPath();
			int idx = fake.getCurrentIndex();
			idx = fake.isReversed() ? idx - 1 : idx + 1;

			if (idx < 0 || idx >= path.size())
			{
				fake.getRuneLiteObject().setActive(false);
				fake.getRuneLiteObject().setModel(null);
				toRemove.add(fake);
				continue;
			}

			fake.setCurrentIndex(idx);
			LocalPoint lp = toLocalPoint(path.get(idx));
			if (lp != null)
			{
				fake.getRuneLiteObject().setLocation(lp, client.getPlane());
			}
		}

		if (!toRemove.isEmpty())
		{
			fakes.removeAll(toRemove);
		}
	}

	private void spawnMissingFakes()
	{
		dbg("spawnMissingFakes called — instanced=" + client.isInInstancedRegion());

		if (!fakes.isEmpty())
		{
			// Already been called this axe iteration.
			return;
		}

		// Collect which real axe NPC is at each spawn tile (by local coordinate comparison)
		Map<WorldPoint, NPC> realAxeAtSpawn = new HashMap<>();
		int totalRealAxes = 0;
		for (NPC npc : client.getNpcs())
		{
			int id = npc.getId();
			if (id == NPC_AXE_STATIC)
			{
				totalRealAxes++;
				for (WorldPoint spawnTile : spawnTiles)
				{
					if (npcIsAtTile(npc, spawnTile))
					{
						dbg("Real axe id=" + id + " matched spawnTile=" + spawnTile);
						realAxeAtSpawn.put(spawnTile, npc);
						break;
					}
				}
			}
		}

		dbg("Found " + totalRealAxes + " real axes, " + realAxeAtSpawn.size() + " at spawn tiles");

		// Find a static axe NPC to copy model/animation from
		NPC staticAxeNpc = null;
		for (NPC npc : client.getNpcs())
		{
			if (npc.getId() == NPC_AXE_STATIC)
			{
				staticAxeNpc = npc;
				break;
			}
		}

		// Spawn fakes at every unoccupied spawn tile
		Map<WorldPoint, FakeAxe> newFakes = new HashMap<>();
		for (List<WorldPoint> path : axePaths)
		{
			for (int spawnIdx : new int[]{0, 5})
			{
				WorldPoint spawnTile = path.get(spawnIdx);
				boolean hasReal = realAxeAtSpawn.containsKey(spawnTile);
				if (hasReal)
				{
					dbg("Skipping " + spawnTile + " real=" + hasReal);
					continue;
				}

				// Use toLocalPoint to handle instanced regions
				LocalPoint lp = toLocalPoint(spawnTile);
				if (lp == null)
				{
					dbg("LocalPoint null for " + spawnTile + " — skipping");
					continue;
				}

				RuneLiteObject obj = client.createRuneLiteObject();

				if (staticAxeNpc != null)
				{
					Model staticModel = buildNpcModel(staticAxeNpc);
				if (staticModel != null)
				{
					obj.setModel(staticModel);
				}
					int animId = staticAxeNpc.getAnimation();
					if (animId != -1)
					{
						// obj.setAnimation(client.loadAnimation(animId));
					}
				}

				obj.setLocation(lp, client.getPlane());
				obj.setActive(true);

				// reversed=true when spawning at index 5 (moves 5→0), false at index 0 (moves 0→5)
				boolean reversed = (spawnIdx == 5);
				FakeAxe fake = new FakeAxe(obj, path, reversed);
				fake.setCurrentIndex(spawnIdx);
				fake.setTicksUntilThrow(3);
				fakes.add(fake);
				newFakes.put(spawnTile, fake);
				dbg("Spawned fake at " + spawnTile + " idx=" + spawnIdx + " reversed=" + reversed);
			}
		}

		dbg("Created " + newFakes.size() + " new fakes, total=" + fakes.size());
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != InterfaceID.DIALOG_OPTION)
		{
			return;
		}

		clientThread.invokeLater(() -> inspectAndMutateDialog());
	}

	private void inspectAndMutateDialog()
	{
		Widget container = client.getWidget(InterfaceID.DIALOG_OPTION, 1);
		if (container == null)
		{
			return;
		}

		Widget[] nested = container.getChildren();
		if (nested == null || nested.length < 3)
		{
			return;
		}

		String text = nested[0].getText();
		if (text == null
			|| !text.toLowerCase().contains("awakener")
			|| !text.toLowerCase().contains("vardorvis"))
		{
			return;
		}

		rainbowOffset = 0;
		dialogActive = true;
		applyDialogText(nested);
	}

	private void applyDialogText(Widget[] nested)
	{
		nested[0].setText("Consume the awakener's " + rainbow("CUBE") + "?");
		nested[1].setText("Yes. I want to fight " + rainbow("MEGA") + " Vardorvis.");
		nested[2].setText("No. omg.");
	}

	private String rainbow(String text)
	{
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (char c : text.toCharArray())
		{
			sb.append("<col=").append(RAINBOW_COLORS[(i + rainbowOffset) % RAINBOW_COLORS.length]).append(">")
			  .append(c)
			  .append("</col>");
			i++;
		}
		return sb.toString();
	}

	@Subscribe
	public void onPostItemComposition(PostItemComposition event)
	{
		ItemComposition composition = event.getItemComposition();
		if (composition.getId() == ItemID.AWAKENERS_ORB)
		{
			composition.setName("Awakener's Cube");
		}
	}

	@Provides
	AwakenedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AwakenedConfig.class);
	}
}
