package com.awakened;

import lombok.Data;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.WorldPoint;
import java.util.List;

@Data
public class FakeAxe
{
	private final RuneLiteObject runeLiteObject;
	private final List<WorldPoint> path;
	private int currentIndex;
	private final boolean reversed;
	private boolean moving;
	private int ticksUntilThrow;
}
