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
			position = 1
	)
	default int maxDoom()
	{
		return 15;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "acidPhaseHpPercent",
		name = "Acid Phase HP %",
		description = "Boss HP percentage at which the acid phase begins",
		position = 2
	)
	default int acidPhaseHpPercent()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "acidPhaseText",
		name = "Acid Phase Text",
		description = "Text displayed above the boss when acid phase begins",
		position = 2
	)
	default String acidPhaseText()
	{
		return "moo! moo!";
	}

	@ConfigItem(
		keyName = "showDeathScreen",
		name = "Show Death Screen",
		description = "Show the 'YOU DIED' overlay and block interactions on death",
		position = 2
	)
	default boolean showDeathScreen() {
        return true;
    }

	@Range(min = 0, max = 24)
	@ConfigItem(
		keyName = "vardorvisExtraQteIcons",
		name = "Vardorvis Extra QTE Icons",
		description = "Additional QTE icons to spawn in Vardorvis.",
		position = 1
	)
	default int vardorvisExtraQteIcons()
	{
		return 4;
	}

	@Alpha
	@ConfigItem(
		keyName = "axeFillColor",
		name = "Axe Fill",
		description = "Fill colour of the 3x3 axe danger zone overlay",
		position = 3
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
		position = 4
	)
	default Color axeBorderColor()
	{
		return new Color(185, 203, 237, 255);
	}
}
