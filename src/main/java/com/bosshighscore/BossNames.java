package com.bosshighscore;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** The short names players use, so long raid names fit the panel. */
final class BossNames
{
	private static final Map<String, String> SHORT_NAMES = ImmutableMap.<String, String>builder()
		.put("Chambers of Xeric", "CoX")
		.put("Chambers of Xeric: Challenge Mode", "CoX CM")
		.put("Theatre of Blood", "ToB")
		.put("Theatre of Blood: Hard Mode", "ToB HM")
		.put("Tombs of Amascut", "ToA")
		.put("Tombs of Amascut: Expert Mode", "ToA Expert")
		.build();

	private BossNames()
	{
	}

	static String shortName(String boss)
	{
		return SHORT_NAMES.getOrDefault(boss, boss);
	}
}
