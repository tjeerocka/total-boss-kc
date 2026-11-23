package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class PlayerTotalsTest
{
	@Test
	public void topBossesNeverExceedsKnownBosses()
	{
		PlayerTotals totals = new PlayerTotals("tjeerocka", 1497, Arrays.asList(
			new BossKills("Kraken", 982),
			new BossKills("Wintertodt", 515)
		), 0);

		assertEquals(1, totals.topBosses(1).size());
		assertEquals(2, totals.topBosses(5).size());
	}

	@Test
	public void unrankedWhenNoKills()
	{
		assertFalse(new PlayerTotals("x", 0, Collections.emptyList(), 0).isRanked());
	}
}
