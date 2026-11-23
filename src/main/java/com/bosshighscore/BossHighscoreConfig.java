package com.bosshighscore;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
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

	@Range(min = 1, max = 168)
	@ConfigItem(
		keyName = "refreshHours",
		name = "Refresh after (hours)",
		description = "On login, friends whose kills are older than this are looked up again",
		position = 2
	)
	default int refreshHours()
	{
		return 6;
	}

	@ConfigSection(
		name = "Bosses counted",
		description = "Which bosses count toward totals, top bosses and gains",
		position = 10
	)
	String BOSSES_SECTION = "bosses";

	@ConfigItem(
		keyName = "includePvm",
		name = "PvM bosses",
		description = "Every boss that isn't a skilling boss or a raid",
		section = BOSSES_SECTION,
		position = 11
	)
	default boolean includePvm()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeSkilling",
		name = "Skilling bosses",
		description = "Wintertodt, Tempoross and Zalcano",
		section = BOSSES_SECTION,
		position = 12
	)
	default boolean includeSkilling()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeRaids",
		name = "Raids",
		description = "Chambers of Xeric, Theatre of Blood and Tombs of Amascut, all modes",
		section = BOSSES_SECTION,
		position = 13
	)
	default boolean includeRaids()
	{
		return true;
	}
}
