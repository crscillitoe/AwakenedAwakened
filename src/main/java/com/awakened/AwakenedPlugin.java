package com.awakened;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.events.GameTick;
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
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Example stopped!");
		overlayManager.remove(itemOverlay);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != InterfaceID.DIALOG_OPTION)
		{
			return;
		}

		// Defer one tick so children are populated before we read/mutate them
		clientThread.invokeLater(() -> inspectAndMutateDialog());
	}

	@Subscribe
	public void onGameTick(GameTick event)
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
