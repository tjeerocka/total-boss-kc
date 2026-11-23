package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BossCategoryTest
{
	@Test
	public void sortsBossesIntoCategories()
	{
		assertEquals(BossCategory.SKILLING, BossCategory.of("Wintertodt"));
		assertEquals(BossCategory.RAIDS, BossCategory.of("Tombs of Amascut: Expert Mode"));
		assertEquals(BossCategory.RAIDS, BossCategory.of("The Fractured Archive"));
		assertEquals(BossCategory.PVM, BossCategory.of("Kraken"));
	}
}
