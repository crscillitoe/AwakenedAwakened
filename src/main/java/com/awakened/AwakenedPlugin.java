package com.awakened;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
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

	@Inject
	private FakeHitsplatOverlay fakeHitsplatOverlay;

	@Inject
	private FakeAxeOverlay fakeAxeOverlay;

    @Inject
    private EventBus eventBus;

    @Inject
    private FakeHead fakeHead;

	@Inject
	private VardorvisQteManager vardorvisQteManager;

	private int spawnTickCount = 0;

	@Getter
    private int fakeHp;

	private boolean poisonActive = false;
	private boolean tookAxeDamage = false;
	private int pendingDisplay = 0;
	private NPC acidTextNpc = null;
	private int acidTextTicksRemaining = 0;

	private static final int VARDORVIS_REGION = 4405;
	private static final String[] RAINBOW_COLORS = {
		"ff0000", "ffff00", "00ff00", "00ffff", "0000ff"
	};

	private boolean dialogActive = false;
	private int rainbowOffset = 0;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(itemOverlay);
		// overlayManager.add(hpBarOverlay);
		overlayManager.add(deathOverlay);
		overlayManager.add(fakeHitsplatOverlay);
		overlayManager.add(fakeAxeOverlay);
		fakeHp = config.maxDoom();
		FakeAxe.initPaths();

	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(itemOverlay);
		// overlayManager.remove(hpBarOverlay);
		overlayManager.remove(deathOverlay);
		overlayManager.remove(fakeHitsplatOverlay);
		overlayManager.remove(fakeAxeOverlay);
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
			vardorvisQteManager.onLoadingReset();
			fakeHp = config.maxDoom();
			poisonActive = false;
			pendingDisplay = 0;
			deathOverlay.hide();
			fakeHitsplatOverlay.reset();
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
		if (!poisonActive)
		{
			checkBossHealth();
		}
		fakeHitsplatOverlay.tick();
		FakeAxe.tickAll(client);

		handleDamage();


		if (poisonActive)
		{
			PoisonTile.tickAll(client, config.poisonTileDuration());
		}


		if (spawnTickCount > 0) {
			spawnTickCount--;
		}

		if (acidTextTicksRemaining > 0 && --acidTextTicksRemaining == 0 && acidTextNpc != null)
		{
			acidTextNpc.setOverheadText("");
			acidTextNpc = null;
		}

        fakeHead.onGameTick(event);
	}

	private void checkBossHealth()
	{
		for (NPC npc : client.getNpcs())
		{
			if (npc.getId() != net.runelite.api.gameval.NpcID.VARDORVIS)
			{
				continue;
			}
			int healthRatio = npc.getHealthRatio();
			int healthScale = npc.getHealthScale();
			if (healthRatio == -1 || healthScale == 0)
			{
				continue;
			}
			if ((double) healthRatio / healthScale <= config.acidPhaseHpPercent() / 100.0)
			{
				poisonActive = true;
				String text = config.acidPhaseText();
				if (text != null && !text.isEmpty())
				{
					npc.setOverheadText(text);
					acidTextNpc = npc;
					acidTextTicksRemaining = 6;
				}
			}
		}
	}

	public void handleDamage()
	{
		// Display effects for the damage calculated last tick
		if (pendingDisplay > 0)
		{
			if (tookAxeDamage)
			{
				client.playSoundEffect(7083);
				tookAxeDamage = false;
			}

			for (int i = 0 ; i < pendingDisplay ; i++) {
				fakeHitsplatOverlay.addHitsplat();
			}

			if (fakeHp == 0)
			{
				if (config.showDeathScreen())
				{
					deathOverlay.show();
				}
				client.getLocalPlayer().setAnimation(836);
				client.getLocalPlayer().setAnimationFrame(0);
			}
		}

		int poisonDamageTaken = PoisonTile.getDamage(client);
		int axeDamageTaken = FakeAxe.getDamage(client);
        int headDamageTaken = FakeHead.getDamage(client);

		if (axeDamageTaken > 0)
		{
			tookAxeDamage = true;
		}

		// Head damage hitsplats display immediately (1 tick earlier than axe/poison)
		if (headDamageTaken > 0)
		{
			for (int i = 0; i < headDamageTaken; i++)
			{
				fakeHitsplatOverlay.addHitsplat();
			}
		}

		int damageTaken = poisonDamageTaken + axeDamageTaken + headDamageTaken;
		fakeHp = Math.max(0, fakeHp - damageTaken);
		pendingDisplay = poisonDamageTaken + axeDamageTaken;
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			fakeHitsplatOverlay.trackRealHitsplat(event.getHitsplat().getDisappearsOnGameCycle());
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (isInVardorvisInstance() && fakeHp == 0 && config.showDeathScreen())
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
		vardorvisQteManager.onWidgetLoaded(event);

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
