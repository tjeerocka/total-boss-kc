package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import net.runelite.client.hiscore.HiscoreSkill;
import org.junit.Test;

public class BossCategoryTest
{
	@Test
	public void sortsBossesIntoCategories()
	{
		assertEquals(BossCategory.SKILLING, BossCategory.of(HiscoreSkill.WINTERTODT));
		assertEquals(BossCategory.RAIDS, BossCategory.of(HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT));
		assertEquals(BossCategory.PVM, BossCategory.of(HiscoreSkill.KRAKEN));
	}
}
