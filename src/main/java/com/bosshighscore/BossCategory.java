package com.bosshighscore;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.runelite.client.hiscore.HiscoreSkill;

public enum BossCategory
{
	PVM,
	SKILLING,
	RAIDS;

	// By hiscore name rather than enum constant, so newer raids (The Fractured Archive) work with any RuneLite version
	private static final Set<String> SKILLING_BOSSES = ImmutableSet.of(
		"Wintertodt",
		"Tempoross",
		"Zalcano"
	);

	private static final Set<String> RAIDS_BOSSES = ImmutableSet.of(
		"Chambers of Xeric",
		"Chambers of Xeric: Challenge Mode",
		"Theatre of Blood",
		"Theatre of Blood: Hard Mode",
		"Tombs of Amascut",
		"Tombs of Amascut: Expert Mode",
		"The Fractured Archive"
	);

	static BossCategory of(HiscoreSkill boss)
	{
		if (SKILLING_BOSSES.contains(boss.getName()))
		{
			return SKILLING;
		}
		return RAIDS_BOSSES.contains(boss.getName()) ? RAIDS : PVM;
	}
}
