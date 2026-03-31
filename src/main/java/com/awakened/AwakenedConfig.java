package com.awakened;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup("awakened")
public interface AwakenedConfig extends Config
{
	@Range(min = 1, max = 50)
	@ConfigItem(
		keyName = "poisonTileDuration",
		name = "Poison Tile Duration",
		description = "Number of ticks a poison tile remains after the player leaves",
		position = 0
	)
	default int poisonTileDuration()
	{
		return 10;
	}

	@Range(min = 1, max = 50)
	@ConfigItem(
			keyName = "maxDoom",
			name = "Maximum Doom",
			description = "Maximum amount of doom you can receive before death",
			position = 0
	)
	default int maxDoom()
	{
		return 15;
	}

	@Alpha
	@ConfigItem(
		keyName = "axeFillColor",
		name = "Axe Fill",
		description = "Fill colour of the 3x3 axe danger zone overlay",
		position = 2
	)
	default Color axeFillColor()
	{
		return new Color(207, 138, 253, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "axeBorderColor",
		name = "Axe Border",
		description = "Border colour of the 3x3 axe danger zone overlay",
		position = 3
	)
	default Color axeBorderColor()
	{
		return new Color(185, 203, 237, 255);
	}
}
