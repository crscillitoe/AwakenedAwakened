package com.awakened;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FakeHitsplatOverlay extends Overlay
{
	private static final int LIFETIME_TICKS = 6;
	private static final int FLOAT_PX_PER_TICK = 3;
	private static final int SPLAT_W = 40;
	private static final int SPLAT_H = 20;
	private static final Font SPLAT_FONT = new Font("Arial", Font.BOLD, 12);

	private final Client client;
	private final List<ActiveHitsplat> active = new ArrayList<>();

	@Inject
	public FakeHitsplatOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/**
	 * Spawns a hitsplat at the player's current screen position.
	 * Call this when fake damage is dealt.
	 */
	public void addHitsplat(int amount)
	{
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		if (lp == null)
		{
			return;
		}
		// Height 100 places the splat roughly at waist level above the tile
		Point p = Perspective.localToCanvas(client, lp, client.getPlane(), 100);
		if (p == null)
		{
			return;
		}
		active.add(new ActiveHitsplat(amount, p));
	}

	/**
	 * Advances all active hitsplats by one tick and removes expired ones.
	 * Call once per game tick.
	 */
	public void tick()
	{
		active.removeIf(h -> ++h.ticksElapsed >= LIFETIME_TICKS);
	}

	/** Clears all active hitsplats without animating them out. */
	public void reset()
	{
		active.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (active.isEmpty())
		{
			return null;
		}

		// Reset transform so coordinates are canvas-absolute
		java.awt.geom.AffineTransform savedTransform = graphics.getTransform();
		graphics.setTransform(new java.awt.geom.AffineTransform());

		for (ActiveHitsplat h : active)
		{
			int x = h.screenPos.getX() - SPLAT_W / 2;
			int y = h.screenPos.getY() - h.ticksElapsed * FLOAT_PX_PER_TICK;

			// Black filled oval
			graphics.setColor(Color.BLACK);
			graphics.fillOval(x, y, SPLAT_W, SPLAT_H);

			// Dark grey border
			graphics.setColor(Color.DARK_GRAY);
			graphics.drawOval(x, y, SPLAT_W, SPLAT_H);

			// White number centered inside oval
			graphics.setFont(SPLAT_FONT);
			graphics.setColor(Color.WHITE);
			FontMetrics fm = graphics.getFontMetrics();
			String text = String.valueOf(h.amount);
			int tx = x + (SPLAT_W - fm.stringWidth(text)) / 2;
			int ty = y + (SPLAT_H + fm.getAscent() - fm.getDescent()) / 2;
			graphics.drawString(text, tx, ty);
		}

		graphics.setTransform(savedTransform);
		return null;
	}

	private static class ActiveHitsplat
	{
		final int amount;
		final Point screenPos;
		int ticksElapsed = 0;

		ActiveHitsplat(int amount, Point screenPos)
		{
			this.amount = amount;
			this.screenPos = screenPos;
		}
	}
}