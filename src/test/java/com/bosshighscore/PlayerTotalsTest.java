package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class PlayerTotalsTest
{
	private static final Duration SIX_HOURS = Duration.ofHours(6);

	private final PlayerTotals tjeerocka = new PlayerTotals("tjeerocka", Arrays.asList(
		new BossKills("Kraken", 982, BossCategory.PVM),
		new BossKills("Wintertodt", 515, BossCategory.SKILLING),
		new BossKills("Chambers of Xeric", 38, BossCategory.RAIDS)
	), 0);

	@Test
	public void totalCountsOnlyChosenCategories()
	{
		assertEquals(1535, tjeerocka.totalKills(EnumSet.allOf(BossCategory.class)));
		assertEquals(1020, tjeerocka.totalKills(EnumSet.of(BossCategory.PVM, BossCategory.RAIDS)));
		assertEquals(0, tjeerocka.totalKills(EnumSet.noneOf(BossCategory.class)));
	}

	@Test
	public void topBossesSkipsExcludedCategories()
	{
		assertEquals(Arrays.asList("Kraken", "Chambers of Xeric"),
			names(tjeerocka.topBosses(5, EnumSet.of(BossCategory.PVM, BossCategory.RAIDS))));
		assertEquals(1, tjeerocka.topBosses(1, EnumSet.allOf(BossCategory.class)).size());
	}

	@Test
	public void snapshotSumsPerCategory()
	{
		KillSnapshot snapshot = tjeerocka.snapshot();

		assertEquals(515, snapshot.total(EnumSet.of(BossCategory.SKILLING)));
		assertEquals(1535, snapshot.total(EnumSet.allOf(BossCategory.class)));
	}

	@Test
	public void staleOnlyAfterMaxAge()
	{
		assertFalse(tjeerocka.isStale(SIX_HOURS.toMillis(), SIX_HOURS));
		assertTrue(tjeerocka.isStale(SIX_HOURS.toMillis() + 1, SIX_HOURS));
	}

	@Test
	public void noBossKillsWhenNothingRanked()
	{
		assertFalse(new PlayerTotals("x", Collections.emptyList(), 0).hasBossKills());
	}

	private static List<String> names(List<BossKills> bosses)
	{
		return bosses.stream().map(BossKills::getBoss).collect(Collectors.toList());
	}
}
