package com.awakened;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class VardorvisQteManager
{
	private static final int ICON_SIZE = 48;
	private static final int CONTAINER_WIDTH = 240;
	private static final int CONTAINER_HEIGHT = 144;
	private static final int CLUSTER_TOLERANCE = 8;
	private static final int MIN_GRID_STEP = ICON_SIZE / 2;

	private static final int[] ORIGINAL_MODEL_IDS = {
		InterfaceID.VardorvisQte.QTE_MODEL_2,
		InterfaceID.VardorvisQte.QTE_MODEL_3,
		InterfaceID.VardorvisQte.QTE_MODEL_4,
		InterfaceID.VardorvisQte.QTE_MODEL_5,
		InterfaceID.VardorvisQte.QTE_MODEL_6
	};

	private final Client client;
    private final ClientThread clientThread;
	private final AwakenedConfig config;

	private final List<Widget> spawnedWidgets = new ArrayList<>();

	@Inject
	public VardorvisQteManager(Client client, ClientThread clientThread, AwakenedConfig config)
	{
		this.client = client;
        this.clientThread = clientThread;
		this.config = config;
	}

	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if (widgetLoaded.getGroupId() != (InterfaceID.VardorvisQte.MAIN >>> 16))
		{
			return;
		}

        clientThread.invokeAtTickEnd(() -> {
            final Widget main = client.getWidget(InterfaceID.VardorvisQte.MAIN);
            if (main == null)
            {
                return;
            }

            resetForNewWidgetTree();
            hideUnlockWidget();
            ensureSpawnedChildren(main, getOriginalAnchorsIncludingHidden());
        });
	}

	public void onFakeWidgetClicked(Widget widget)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}
		if (!spawnedWidgets.contains(widget))
		{
			return;
		}

        // Click sound
        client.playSoundEffect(7104);

		widget.setHidden(true);
		widget.revalidate();
		if (!hasVisibleFakeWidgets())
		{
			final Widget unlockWidget = client.getWidget(InterfaceID.VardorvisQte.QTE_MODEL_1);
			if (unlockWidget != null)
			{
				unlockWidget.setHidden(false);
				unlockWidget.revalidate();
			}
		}
	}

	public void onLoadingReset()
	{
		resetForNewWidgetTree();
	}

	private void ensureSpawnedChildren(Widget main, List<Widget> anchors)
	{
		final int requested = Math.max(0, config.vardorvisExtraQteIcons());
		if (requested == 0 || anchors.isEmpty())
		{
			return;
		}

		final Widget template = anchors.get(0);
		final GridSpec grid = inferGrid(anchors, template);
		final List<Widget> blockingAnchors = getVisibleOriginalAnchors();
		final Set<String> occupiedCells = getOccupiedCells(blockingAnchors, grid);
		final List<Placement> placements = pickFreePlacements(grid, occupiedCells, blockingAnchors, requested);

		while (spawnedWidgets.size() < requested)
		{
			final int childIndex = main.getChildren() == null ? 0 : main.getChildren().length;
			final Widget child = main.createChild(childIndex, template.getType());
			copyTemplateFields(template, child);
			spawnedWidgets.add(child);
		}

		for (int i = 0; i < spawnedWidgets.size(); i++)
		{
			final Widget child = spawnedWidgets.get(i);
			if (child == null)
			{
				continue;
			}

			if (i >= placements.size())
			{
				child.setHidden(true);
				child.revalidate();
				continue;
			}

			final Placement placement = placements.get(i);

			child.setOriginalX(placement.x);
			child.setOriginalY(placement.y);
			child.setOriginalWidth(ICON_SIZE);
			child.setOriginalHeight(ICON_SIZE);
			child.setXPositionMode(template.getXPositionMode());
			child.setYPositionMode(template.getYPositionMode());
			child.setWidthMode(template.getWidthMode());
			child.setHeightMode(template.getHeightMode());
			child.setHidden(false);
			child.revalidate();
		}
	}

	private void copyTemplateFields(Widget template, Widget child)
	{
		child.setContentType(template.getContentType());
		child.setModelType(template.getModelType());
		child.setModelId(template.getModelId());
		child.setAnimationId(10374);
		child.setRotationX(template.getRotationX());
		child.setRotationY(template.getRotationY());
		child.setRotationZ(template.getRotationZ());
		child.setSize(ICON_SIZE, ICON_SIZE);
		child.setModelZoom(template.getModelZoom());
		child.setHasListener(true);
		child.setAction(0, "Destroy");
		child.setOnOpListener((JavaScriptCallback)(ev) -> onFakeWidgetClicked(child));
		child.setNoClickThrough(template.getNoClickThrough());
		child.setNoScrollThrough(template.getNoScrollThrough());
		child.setBorderType(template.getBorderType());
		child.setSpriteTiling(template.getSpriteTiling());
		child.revalidate();
	}

	private List<Widget> getOriginalAnchorsIncludingHidden()
	{
		final List<Widget> anchors = new ArrayList<>();
		for (int widgetId : ORIGINAL_MODEL_IDS)
		{
			final Widget widget = client.getWidget(widgetId);
			if (widget != null)
			{
				anchors.add(widget);
			}
		}
		return anchors;
	}

	private List<Widget> getVisibleOriginalAnchors()
	{
		final List<Widget> anchors = new ArrayList<>();
		for (int widgetId : ORIGINAL_MODEL_IDS)
		{
			final Widget widget = client.getWidget(widgetId);
			if (widget != null && !widget.isHidden())
			{
				anchors.add(widget);
			}
		}
		return anchors;
	}

	private void hideUnlockWidget()
	{
		final Widget unlockWidget = client.getWidget(InterfaceID.VardorvisQte.QTE_MODEL_1);
		if (unlockWidget != null)
		{
			unlockWidget.setHidden(true);
			unlockWidget.revalidate();
		}
	}

	private void resetForNewWidgetTree()
	{
		spawnedWidgets.clear();
	}

	private boolean hasVisibleFakeWidgets()
	{
		for (Widget widget : spawnedWidgets)
		{
			if (widget != null && !widget.isHidden())
			{
				return true;
			}
		}
		return false;
	}

	private GridSpec inferGrid(List<Widget> anchors, Widget template)
	{
		final List<Integer> xs = new ArrayList<>();
		final List<Integer> ys = new ArrayList<>();
		for (Widget anchor : anchors)
		{
			xs.add(anchor.getOriginalX());
			ys.add(anchor.getOriginalY());
		}

		Collections.sort(xs);
		Collections.sort(ys);

		final List<Integer> xCenters = clusterCenters(xs);
		final List<Integer> yCenters = clusterCenters(ys);

		final int stepX = inferStep(xCenters, Math.max(MIN_GRID_STEP, template.getOriginalWidth()));
		final int stepY = inferStep(yCenters, Math.max(MIN_GRID_STEP, template.getOriginalHeight()));
		final int originX = xCenters.get(0);
		final int originY = yCenters.get(0);
		final int maxCol = Math.max(0, (xs.get(xs.size() - 1) - originX) / stepX);
		final int maxRow = Math.max(0, (ys.get(ys.size() - 1) - originY) / stepY);
		return new GridSpec(originX, originY, stepX, stepY, 0, maxCol, 0, maxRow);
	}

	private int inferStep(List<Integer> sortedValues, int fallback)
	{
		int step = Integer.MAX_VALUE;
		for (int i = 1; i < sortedValues.size(); i++)
		{
			int diff = sortedValues.get(i) - sortedValues.get(i - 1);
			if (diff >= MIN_GRID_STEP && diff < step)
			{
				step = diff;
			}
		}
		return step == Integer.MAX_VALUE ? fallback : step;
	}

	private List<Integer> clusterCenters(List<Integer> sortedValues)
	{
		final List<Integer> centers = new ArrayList<>();
		if (sortedValues.isEmpty())
		{
			return centers;
		}

		int clusterStart = sortedValues.get(0);
		int clusterEnd = clusterStart;
		for (int i = 1; i < sortedValues.size(); i++)
		{
			final int value = sortedValues.get(i);
			if (value - clusterEnd <= CLUSTER_TOLERANCE)
			{
				clusterEnd = value;
				continue;
			}
			centers.add((clusterStart + clusterEnd) / 2);
			clusterStart = value;
			clusterEnd = value;
		}
		centers.add((clusterStart + clusterEnd) / 2);
		return centers;
	}

	private Set<String> getOccupiedCells(List<Widget> anchors, GridSpec grid)
	{
		final Set<String> occupied = new HashSet<>();
		for (Widget anchor : anchors)
		{
			final int col = Math.round((anchor.getOriginalX() - grid.originX) / (float) grid.stepX);
			final int row = Math.round((anchor.getOriginalY() - grid.originY) / (float) grid.stepY);
			occupied.add(cellKey(col, row));
		}
		return occupied;
	}

	private List<Placement> pickFreePlacements(GridSpec grid, Set<String> occupiedCells, List<Widget> anchors, int count)
	{
		final List<Placement> result = new ArrayList<>();
		final List<IntRect> occupiedRects = new ArrayList<>();
		final Set<String> usedPositions = new HashSet<>();
		for (Widget anchor : anchors)
		{
			occupiedRects.add(new IntRect(anchor.getOriginalX(), anchor.getOriginalY(), ICON_SIZE, ICON_SIZE));
		}
		int expansion = 0;

		while (result.size() < count && expansion <= 20)
		{
			final int minCol = grid.minCol - expansion;
			final int maxCol = grid.maxCol + expansion;
			final int minRow = grid.minRow - expansion;
			final int maxRow = grid.maxRow + expansion;

			for (int row = minRow; row <= maxRow && result.size() < count; row++)
			{
				for (int col = minCol; col <= maxCol && result.size() < count; col++)
				{
					final String key = cellKey(col, row);
					if (occupiedCells.contains(key))
					{
						continue;
					}

					final int x = grid.originX + (col * grid.stepX);
					final int y = grid.originY + (row * grid.stepY);
					if (!isInsideContainer(x, y))
					{
						continue;
					}
					final IntRect candidate = new IntRect(x, y, ICON_SIZE, ICON_SIZE);
					if (overlapsAny(candidate, occupiedRects))
					{
						continue;
					}

					occupiedCells.add(key);
					occupiedRects.add(candidate);
					usedPositions.add(cellKey(x, y));
					result.add(new Placement(x, y));
				}
			}
			expansion++;
		}

		final List<Placement> randomCandidates = new ArrayList<>();
		for (int y = 0; y <= CONTAINER_HEIGHT - ICON_SIZE; y += 8)
		{
			for (int x = 0; x <= CONTAINER_WIDTH - ICON_SIZE; x += 8)
			{
				randomCandidates.add(new Placement(x, y));
			}
		}
		Collections.shuffle(randomCandidates);

		// Fallback: random non-overlapping in-box fill.
		for (Placement candidatePos : randomCandidates)
		{
			if (result.size() >= count)
			{
				break;
			}
			final String posKey = cellKey(candidatePos.x, candidatePos.y);
			if (usedPositions.contains(posKey))
			{
				continue;
			}
			final IntRect candidate = new IntRect(candidatePos.x, candidatePos.y, ICON_SIZE, ICON_SIZE);
			if (overlapsAny(candidate, occupiedRects))
			{
				continue;
			}
			usedPositions.add(posKey);
			occupiedRects.add(candidate);
			result.add(candidatePos);
		}

		// Last resort: allow overlaps, but still choose random in-box positions.
		for (Placement candidatePos : randomCandidates)
		{
			if (result.size() >= count)
			{
				break;
			}
			final String posKey = cellKey(candidatePos.x, candidatePos.y);
			if (usedPositions.contains(posKey))
			{
				continue;
			}
			usedPositions.add(posKey);
			result.add(candidatePos);
		}

		return result;
	}

	private String cellKey(int col, int row)
	{
		return col + ":" + row;
	}

	private boolean isInsideContainer(int x, int y)
	{
		return x >= 0
			&& y >= 0
			&& x + ICON_SIZE <= CONTAINER_WIDTH
			&& y + ICON_SIZE <= CONTAINER_HEIGHT;
	}

	private boolean overlapsAny(IntRect candidate, List<IntRect> occupied)
	{
		for (IntRect rect : occupied)
		{
			if (candidate.intersects(rect))
			{
				return true;
			}
		}
		return false;
	}

	private static final class GridSpec
	{
		private final int originX;
		private final int originY;
		private final int stepX;
		private final int stepY;
		private final int minCol;
		private final int maxCol;
		private final int minRow;
		private final int maxRow;

		private GridSpec(int originX, int originY, int stepX, int stepY, int minCol, int maxCol, int minRow, int maxRow)
		{
			this.originX = originX;
			this.originY = originY;
			this.stepX = stepX;
			this.stepY = stepY;
			this.minCol = minCol;
			this.maxCol = maxCol;
			this.minRow = minRow;
			this.maxRow = maxRow;
		}
	}

	private static final class Placement
	{
		private final int x;
		private final int y;

		private Placement(int x, int y)
		{
			this.x = x;
			this.y = y;
		}
	}

	private static final class IntRect
	{
		private final int x;
		private final int y;
		private final int w;
		private final int h;

		private IntRect(int x, int y, int w, int h)
		{
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		private boolean intersects(IntRect other)
		{
			return x < other.x + other.w
				&& x + w > other.x
				&& y < other.y + other.h
				&& y + h > other.y;
		}
	}
}
