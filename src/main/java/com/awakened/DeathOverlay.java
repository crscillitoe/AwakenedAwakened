package com.awakened;

import net.runelite.api.Client;
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

@Singleton
public class DeathOverlay extends Overlay
{
	private static final Font DEATH_FONT = new Font("Old English Text MT", Font.BOLD, 96);
	private static final String DEATH_TEXT = "YOU DIED LOL";
	private static final int FADE_DURATION_MS = 2000;
	private static final int MAX_OVERLAY_ALPHA = 180;

	private final Client client;
	private boolean active = false;
	private long showTimeMs = 0;

	@Inject
	public DeathOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	public void show()
	{
		if (!active)
		{
			showTimeMs = System.currentTimeMillis();
		}
		active = true;
	}

	public void hide()
	{
		active = false;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active)
		{
			return null;
		}

		int w = client.getCanvas().getWidth();
		int h = client.getCanvas().getHeight();

		long elapsed = System.currentTimeMillis() - showTimeMs;
		double fadeProgress = Math.min(1.0, (double) elapsed / FADE_DURATION_MS);

		// Reset transform so coordinates are canvas-absolute, not overlay-relative
		java.awt.geom.AffineTransform savedTransform = graphics.getTransform();
		graphics.setTransform(new java.awt.geom.AffineTransform());

		// Semi-transparent dark overlay with fade-in
		int overlayAlpha = (int) (MAX_OVERLAY_ALPHA * fadeProgress);
		graphics.setColor(new Color(0, 0, 0, overlayAlpha));
		graphics.fillRect(0, 0, w, h);

		// "YOU DIED LOL" text centered with fade-in
		int textAlpha = (int) (255 * fadeProgress);
		graphics.setFont(DEATH_FONT);
		FontMetrics fm = graphics.getFontMetrics();
		int textX = (w - fm.stringWidth(DEATH_TEXT)) / 2;
		int textY = (h + fm.getAscent() - fm.getDescent()) / 2;

		// Text shadow
		graphics.setColor(new Color(80, 0, 0, textAlpha));
		graphics.drawString(DEATH_TEXT, textX + 3, textY + 3);

		// Main text
		graphics.setColor(new Color(200, 0, 0, textAlpha));
		graphics.drawString(DEATH_TEXT, textX, textY);

		graphics.setTransform(savedTransform);
		return new Dimension(w, h);
	}
}
