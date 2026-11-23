package com.bosshighscore;

import java.util.EnumSet;
import java.util.Set;
import net.runelite.client.hiscore.HiscoreSkill;

public enum BossCategory
{
	PVM,
	SKILLING,
	RAIDS;

	private static final Set<HiscoreSkill> SKILLING_BOSSES = EnumSet.of(
		HiscoreSkill.WINTERTODT,
		HiscoreSkill.TEMPOROSS,
		HiscoreSkill.ZALCANO
	);

	private static final Set<HiscoreSkill> RAIDS_BOSSES = EnumSet.of(
		HiscoreSkill.CHAMBERS_OF_XERIC,
		HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE,
		HiscoreSkill.THEATRE_OF_BLOOD,
		HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE,
		HiscoreSkill.TOMBS_OF_AMASCUT,
		HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT
	);

	static BossCategory of(HiscoreSkill boss)
	{
		if (SKILLING_BOSSES.contains(boss))
		{
			return SKILLING;
		}
		return RAIDS_BOSSES.contains(boss) ? RAIDS : PVM;
	}
}
