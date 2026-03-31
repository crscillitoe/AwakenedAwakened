package com.awakened;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

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
}
