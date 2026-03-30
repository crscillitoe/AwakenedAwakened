package com.awakened;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

@Singleton
public class HpBarOverlay extends Overlay
{
	private static final int BAR_WIDTH  = 300;
	private static final int BAR_HEIGHT = 24;

	private final AwakenedPlugin plugin;

	@Inject
	public HpBarOverlay(AwakenedPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInVardorvisInstance())
		{
			return null;
		}

		double fraction = (double) plugin.getFakeHp() / AwakenedPlugin.MAX_FAKE_HP;
		int fillWidth = (int) (BAR_WIDTH * fraction);

		// Background
		graphics.setColor(Color.DARK_GRAY);
		graphics.fillRect(0, 0, BAR_WIDTH, BAR_HEIGHT);

		// HP fill
		graphics.setColor(Color.RED);
		graphics.fillRect(0, 0, fillWidth, BAR_HEIGHT);

		// Border
		graphics.setColor(Color.BLACK);
		graphics.drawRect(0, 0, BAR_WIDTH - 1, BAR_HEIGHT - 1);

		return new Dimension(BAR_WIDTH, BAR_HEIGHT);
	}
}
