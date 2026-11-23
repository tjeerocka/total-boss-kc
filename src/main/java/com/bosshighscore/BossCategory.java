package com.bosshighscore;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

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

	/** By hiscore name, e.g. "Chambers of Xeric". */
	static BossCategory of(String bossName)
	{
		if (SKILLING_BOSSES.contains(bossName))
		{
			return SKILLING;
		}
		return RAIDS_BOSSES.contains(bossName) ? RAIDS : PVM;
	}
}
