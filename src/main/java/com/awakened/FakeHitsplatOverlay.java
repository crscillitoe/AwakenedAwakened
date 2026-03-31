package com.awakened;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FakeHitsplatOverlay extends Overlay
{
	private static final int LIFETIME_TICKS = 1;
	private static final int FLOAT_PX_PER_TICK = 3;
	private static final Font SPLAT_FONT = FontManager.getRunescapeSmallFont();

	private static final int REAL_HITSPLAT_OFFSET = 12;
	private static final int REFERENCE_ZOOM = 512;

	private final Client client;
	private final SpriteManager spriteManager;
	private final List<ActiveHitsplat> active = new ArrayList<>();
	private final List<Integer> realHitsplatExpiryCycles = new ArrayList<>();
	private int hitCounter = 0;

	@Inject
	public FakeHitsplatOverlay(Client client, SpriteManager spriteManager)
	{
		this.client = client;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/**
	 * Spawns a hitsplat at the player's current screen position.
	 * The displayed number increments by 1 each call (1, 2, 3, …).
	 * Any existing hitsplat with a lower amount is removed so only the highest shows.
	 */
	public void addHitsplat()
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
		int newAmount = ++hitCounter;
		int yOffset = getActiveRealHitsplatCount() * REAL_HITSPLAT_OFFSET;
		active.removeIf(h -> h.amount < newAmount);
		active.add(new ActiveHitsplat(newAmount, p, yOffset));
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
		realHitsplatExpiryCycles.clear();
		hitCounter = 0;
	}

	/**
	 * Tracks a real hitsplat applied to the local player so we can offset
	 * custom hitsplats to avoid overlapping.
	 */
	public void trackRealHitsplat(int disappearsOnGameCycle)
	{
		realHitsplatExpiryCycles.add(disappearsOnGameCycle);
	}

	private int getActiveRealHitsplatCount()
	{
		int currentCycle = client.getGameCycle();
		realHitsplatExpiryCycles.removeIf(cycle -> cycle <= currentCycle);
		return realHitsplatExpiryCycles.size();
	}

	private BufferedImage buildSplatImage(int amount)
	{
		BufferedImage rawSplat = spriteManager.getSprite(SpriteID.Hitmark.COLOSSEUM_DOOM, 0);
		if (rawSplat == null)
		{
			return null;
		}

		BufferedImage splat = new BufferedImage(
			rawSplat.getColorModel(),
			rawSplat.copyData(null),
			rawSplat.getColorModel().isAlphaPremultiplied(),
			null);

		Graphics2D g = splat.createGraphics();
		g.setFont(SPLAT_FONT);

		FontMetrics fm = g.getFontMetrics();
		String text = String.valueOf(amount);
		int x = (splat.getWidth() - fm.stringWidth(text)) / 2;
		int y = (splat.getHeight() - fm.getAscent()) / 2 + fm.getAscent();

		// Text shadow
		g.setColor(Color.BLACK);
		g.drawString(text, x + 1, y + 1);
		// White number
		g.setColor(Color.WHITE);
		g.drawString(text, x, y);

		g.dispose();
		return splat;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (active.isEmpty())
		{
			return null;
		}

		// Certified Zoink-Wizard code genius moment
		Point p = Perspective.localToCanvas(client, client.getLocalPlayer().getLocalLocation(), client.getPlane(), 100);

		java.awt.geom.AffineTransform savedTransform = graphics.getTransform();
		graphics.setTransform(new java.awt.geom.AffineTransform());

		double scale = (double) client.get3dZoom() / REFERENCE_ZOOM;

		for (ActiveHitsplat h : active)
		{
			BufferedImage splatImage = buildSplatImage(h.amount);
			if (splatImage == null)
			{
				continue;
			}

			int drawW = (int) (splatImage.getWidth() * scale);
			int drawH = (int) (splatImage.getHeight() * scale);
			int x = p.getX() - drawW / 2;
			int y = p.getY() - (int) (h.ticksElapsed * FLOAT_PX_PER_TICK * scale) - drawH / 2 + (int) (h.realSplatOffset * scale);

			graphics.drawImage(splatImage, x, y, drawW, drawH, null);
		}

		graphics.setTransform(savedTransform);
		return null;
	}

	private static class ActiveHitsplat
	{
		final int amount;
		final Point screenPos;
		final int realSplatOffset;
		int ticksElapsed = 0;

		ActiveHitsplat(int amount, Point screenPos, int realSplatOffset)
		{
			this.amount = amount;
			this.screenPos = screenPos;
			this.realSplatOffset = realSplatOffset;
		}
	}
}
