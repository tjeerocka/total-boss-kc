package com.bosshighscore;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(BossHighscoreConfig.GROUP)
public interface BossHighscoreConfig extends Config
{
	String GROUP = "bosshighscore";
	String TOP_BOSS_COUNT = "topBossCount";

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = TOP_BOSS_COUNT,
		name = "Top bosses shown",
		description = "How many bosses the dropdown under a player's name shows",
		position = 1
	)
	default int topBossCount()
	{
		return 5;
	}
}
