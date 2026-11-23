package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class PlayerTotalsTest
{
	private static final Duration SIX_HOURS = Duration.ofHours(6);

	@Test
	public void staleOnlyAfterMaxAge()
	{
		PlayerTotals totals = new PlayerTotals("tjeerocka", 1, Collections.emptyList(), 0);

		assertFalse(totals.isStale(SIX_HOURS.toMillis(), SIX_HOURS));
		assertTrue(totals.isStale(SIX_HOURS.toMillis() + 1, SIX_HOURS));
	}

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
