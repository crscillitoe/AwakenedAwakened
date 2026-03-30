package com.awakened;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostItemComposition;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

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
	private HpBarOverlay hpBarOverlay;

	@Inject
	private DeathOverlay deathOverlay;

	private int spawnTickCount = 0;

	public static final int MAX_FAKE_HP = 99;
	@Getter
    private int fakeHp = MAX_FAKE_HP;

	private boolean poisonActive = false;

	private static final int VARDORVIS_REGION = 4405;
	private static final String[] RAINBOW_COLORS = {
		"ff0000", "ffff00", "00ff00", "00ffff", "0000ff"
	};

	private boolean dialogActive = false;
	private int rainbowOffset = 0;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Example started!");
		overlayManager.add(itemOverlay);
		overlayManager.add(hpBarOverlay);
		overlayManager.add(deathOverlay);
		FakeAxe.initPaths();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Example stopped!");
		overlayManager.remove(itemOverlay);
		overlayManager.remove(hpBarOverlay);
		overlayManager.remove(deathOverlay);
		FakeAxe.cleanupAll();
		PoisonTile.cleanupAll();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			FakeAxe.cleanupAll();
			PoisonTile.cleanupAll();
			fakeHp = MAX_FAKE_HP;
			poisonActive = false;
			deathOverlay.hide();
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		int id = event.getNpc().getId();
		if (id == 12223)
		{
			int healthRatio = event.getNpc().getHealthRatio();
			int healthScale = event.getNpc().getHealthScale();

			double ratio = (double)healthRatio / (double)healthScale;
			if (ratio <= 0.5)
			{
				poisonActive = true;
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!isInVardorvisInstance())
		{
			return;
		}
		NPC npc = event.getNpc();
		if (npc.getId() == FakeAxe.NPC_AXE_STATIC && FakeAxe.isAtAnySpawnTile(client, npc) && spawnTickCount == 0)
		{
			FakeAxe.spawnMissing(client);
			spawnTickCount = 3;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		rainbowDialog();

		if (!isInVardorvisInstance())
		{
			return;
		}
		handleDamage();

		FakeAxe.tickAll(client);

		if (poisonActive)
		{
			PoisonTile.tickAll(client, config.poisonTileDuration());
		}


		if (spawnTickCount > 0) {
			spawnTickCount--;
		}
	}

	public void handleDamage()
	{
		int damageTaken = PoisonTile.getDamage(client) + FakeAxe.getDamage(client);
		fakeHp = Math.max(0, fakeHp - damageTaken);
		if (fakeHp == 0)
		{
			deathOverlay.show();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (isInVardorvisInstance() && fakeHp == 0)
		{
			client.setMenuEntries(new MenuEntry[0]);
		}
	}

	boolean isInVardorvisInstance()
	{
		if (!client.isInInstancedRegion())
		{
			return false;
		}
		for (int region : client.getMapRegions())
		{
			if (region == VARDORVIS_REGION)
			{
				return true;
			}
		}
		return false;
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
