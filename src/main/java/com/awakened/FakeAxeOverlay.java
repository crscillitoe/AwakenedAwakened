package com.awakened;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.Collection;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;

@Singleton
public class FakeAxeOverlay extends Overlay
{
	private static final BasicStroke BORDER_STROKE = new BasicStroke(1);

	private final Client client;
	private final AwakenedPlugin plugin;
	private final AwakenedConfig config;

	@Inject
	public FakeAxeOverlay(Client client, AwakenedPlugin plugin, AwakenedConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInVardorvisInstance())
		{
			return null;
		}

		for (FakeAxe axe : FakeAxe.getActiveAxes())
		{
			LocalPoint axeLp = axe.getCurrentLocalPoint(client);
			drawZone(graphics, axeLp);
		}

		for (NPC npc : client.getNpcs())
		{
			if (npc.getId() != FakeAxe.NPC_AXE_STATIC && npc.getId() != FakeAxe.NPC_AXE_FLYING)
			{
				continue;
			}
			WorldPoint npcLocation = npc.getWorldLocation();
			WorldPoint worldPoint = new WorldPoint(npcLocation.getX() + 1, npcLocation.getY() + 1, npcLocation.getPlane());
			drawZone(graphics, toLocalPoint(worldPoint));
		}

		return null;
	}

	// getWorldLocation() returns template coords via fromLocalInstance; we must
	// map template → instance world coords before converting to LocalPoint.
	private LocalPoint toLocalPoint(WorldPoint templateWp)
	{
		Collection<WorldPoint> instances = WorldPoint.toLocalInstance(client, templateWp);
		if (!instances.isEmpty())
		{
			return LocalPoint.fromWorld(client, instances.iterator().next());
		}
		return LocalPoint.fromWorld(client, templateWp);
	}

	private void drawZone(Graphics2D graphics, LocalPoint lp)
	{
		if (lp == null)
		{
			return;
		}
		Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, 3);
		if (poly == null)
		{
			return;
		}
		graphics.setColor(config.axeFillColor());
		graphics.fill(poly);
		graphics.setColor(config.axeBorderColor());
		graphics.setStroke(BORDER_STROKE);
		graphics.draw(poly);
	}
}
