package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import net.runelite.client.hiscore.HiscoreSkill;
import org.junit.Test;

public class BossNamesTest
{
	@Test
	public void shortensRaidsUsingHiscoreNames()
	{
		assertEquals("CoX", BossNames.shortName(HiscoreSkill.CHAMBERS_OF_XERIC.getName()));
		assertEquals("CoX CM", BossNames.shortName(HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE.getName()));
		assertEquals("ToB HM", BossNames.shortName(HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE.getName()));
		assertEquals("ToA Expert", BossNames.shortName(HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT.getName()));
	}

	@Test
	public void keepsOtherBossNames()
	{
		assertEquals("Kraken", BossNames.shortName("Kraken"));
	}
}
