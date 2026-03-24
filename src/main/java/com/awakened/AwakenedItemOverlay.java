package com.awakened;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ImageUtil;

public class AwakenedItemOverlay extends WidgetItemOverlay
{
	private final BufferedImage cubeImage;

	@Inject
	AwakenedItemOverlay()
	{
		showOnInventory();
		showOnEquipment();
		showOnBank();
		cubeImage = ImageUtil.loadImageResource(
			AwakenedItemOverlay.class,
			"/com/awakened/assets/awakeners_cube.png"
		);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (itemId != ItemID.AWAKENERS_ORB || cubeImage == null)
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		if (bounds == null)
		{
			return;
		}

		Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
		graphics.drawImage(cubeImage, bounds.x, bounds.y, bounds.width, bounds.height, null);
		graphics.setComposite(original);
	}
}
