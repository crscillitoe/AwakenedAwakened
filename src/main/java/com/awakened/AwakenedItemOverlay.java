package com.awakened;

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

		BufferedImage raw = ImageUtil.loadImageResource(
			AwakenedItemOverlay.class,
			"/com/awakened/assets/awakeners_cube.png"
		);

		cubeImage = cleanAndTrim(raw);
	}

	private static BufferedImage cleanAndTrim(BufferedImage image)
	{
		if (image == null)
		{
			return null;
		}

		int w = image.getWidth();
		int h = image.getHeight();
		int alphaThreshold = 64;
		int minX = w, minY = h, maxX = 0, maxY = 0;

		// Clean semi-transparent pixels: make them fully opaque or fully transparent
		BufferedImage cleaned = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int argb = image.getRGB(x, y);
				int alpha = (argb >> 24) & 0xFF;

				if (alpha < alphaThreshold)
				{
					cleaned.setRGB(x, y, 0x00000000);
				}
				else
				{
					cleaned.setRGB(x, y, argb | 0xFF000000);
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}

		if (maxX < minX || maxY < minY)
		{
			return cleaned;
		}

		return cleaned.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
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

		graphics.drawImage(cubeImage, bounds.x, bounds.y, bounds.width, bounds.height, null);
	}
}
