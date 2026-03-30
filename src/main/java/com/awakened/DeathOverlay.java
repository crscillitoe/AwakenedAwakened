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
	private static final Font DEATH_FONT = new Font("Arial", Font.BOLD, 96);
	private static final String DEATH_TEXT = "YOU DIED";

	private final Client client;
	private boolean active = false;

	@Inject
	public DeathOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	public void show() { active = true; }
	public void hide() { active = false; }

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active)
		{
			return null;
		}

		int w = client.getCanvas().getWidth();
		int h = client.getCanvas().getHeight();

		// Reset transform so coordinates are canvas-absolute, not overlay-relative
		java.awt.geom.AffineTransform savedTransform = graphics.getTransform();
		graphics.setTransform(new java.awt.geom.AffineTransform());

		// Semi-transparent dark overlay
		graphics.setColor(new Color(0, 0, 0, 180));
		graphics.fillRect(0, 0, w, h);

		// "YOU DIED" text centered
		graphics.setFont(DEATH_FONT);
		graphics.setColor(Color.RED);

		graphics.drawString(DEATH_TEXT, 100, 200);

		graphics.setTransform(savedTransform);
		return new Dimension(w, h);
	}
}
