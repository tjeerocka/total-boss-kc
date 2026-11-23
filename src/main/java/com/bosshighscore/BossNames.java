package com.bosshighscore;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** The short names players use, so long boss names fit the panel. Keyed by hiscore name. */
final class BossNames
{
	private static final Map<String, String> SHORT_NAMES = ImmutableMap.<String, String>builder()
		.put("Chambers of Xeric", "CoX")
		.put("Chambers of Xeric: Challenge Mode", "CoX CM")
		.put("Theatre of Blood", "ToB")
		.put("Theatre of Blood: Hard Mode", "ToB HM")
		.put("Tombs of Amascut", "ToA")
		.put("Tombs of Amascut: Expert Mode", "ToA Expert")
		.put("The Fractured Archive", "TFA")
		.put("Thermonuclear Smoke Devil", "Thermy")
		.put("The Corrupted Gauntlet", "CG")
		.put("Crazy Archaeologist", "Crazy Arch")
		.put("Deranged Archaeologist", "Deranged Arch")
		.put("Grotesque Guardians", "GGs")
		.put("Kalphite Queen", "KQ")
		.put("King Black Dragon", "KBD")
		.put("Corporeal Beast", "Corp")
		.put("Alchemical Hydra", "Hydra")
		.put("TzKal-Zuk", "Zuk")
		.put("TzTok-Jad", "Jad")
		.build();

	private BossNames()
	{
	}

	static String shortName(String boss)
	{
		return SHORT_NAMES.getOrDefault(boss, boss);
	}
}
