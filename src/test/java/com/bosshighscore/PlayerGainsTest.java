package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class PlayerGainsTest
{
	private static final ZoneId UTC = ZoneOffset.UTC;
	private static final Set<BossCategory> ALL = EnumSet.allOf(BossCategory.class);

	// Monday 2026-11-23
	private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

	@Test
	public void firstLookupHasNoGains()
	{
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, 100));

		for (GainPeriod period : GainPeriod.values())
		{
			assertEquals(0, gains.gain(period, ALL, MONDAY, UTC));
		}
	}

	@Test
	public void sameDayLookupsAddUpForEveryPeriod()
	{
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, 100))
			.record(snapshot(MONDAY, 130), UTC);

		assertEquals(30, gains.gain(GainPeriod.TODAY, ALL, MONDAY, UTC));
		assertEquals(30, gains.gain(GainPeriod.YEAR, ALL, MONDAY, UTC));
	}

	@Test
	public void newDayStartsTodayFromLastLookup()
	{
		LocalDate tuesday = MONDAY.plusDays(1);
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, 100))
			.record(snapshot(MONDAY, 130), UTC)
			.record(snapshot(tuesday, 140), UTC);

		assertEquals(10, gains.gain(GainPeriod.TODAY, ALL, tuesday, UTC));
		assertEquals(40, gains.gain(GainPeriod.WEEK, ALL, tuesday, UTC));
	}

	@Test
	public void newWeekStartsWeekFromLastLookup()
	{
		LocalDate nextMonday = MONDAY.plusWeeks(1);
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, 100))
			.record(snapshot(MONDAY.plusDays(3), 150), UTC)
			.record(snapshot(nextMonday, 170), UTC);

		assertEquals(20, gains.gain(GainPeriod.WEEK, ALL, nextMonday, UTC));
		assertEquals(70, gains.gain(GainPeriod.MONTH, ALL, nextMonday, UTC));
	}

	@Test
	public void noLookupYetTodayMeansNoGainToday()
	{
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, 100))
			.record(snapshot(MONDAY, 130), UTC);

		assertEquals(0, gains.gain(GainPeriod.TODAY, ALL, MONDAY.plusDays(1), UTC));
		assertEquals(30, gains.gain(GainPeriod.WEEK, ALL, MONDAY.plusDays(1), UTC));
	}

	@Test
	public void gainsFollowCountedCategories()
	{
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, 100))
			.record(snapshot(MONDAY, 130), UTC);

		assertEquals(0, gains.gain(GainPeriod.TODAY, EnumSet.of(BossCategory.RAIDS), MONDAY, UTC));
	}

	@Test
	public void breakdownListsEveryBossThatWentUpMostFirst()
	{
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, ImmutableMap.of("Zulrah", 10, "Kraken", 50, "Vorkath", 7)))
			.record(snapshot(MONDAY, ImmutableMap.of("Zulrah", 12, "Kraken", 90, "Vorkath", 7, "Cerberus", 5)), UTC);

		assertEquals(Arrays.asList(
			new BossKills("Kraken", 40),
			new BossKills("Cerberus", 5),
			new BossKills("Zulrah", 2)
		), gains.breakdown(GainPeriod.TODAY, ALL, MONDAY, UTC));
	}

	@Test
	public void breakdownFollowsCountedCategories()
	{
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, ImmutableMap.of("Wintertodt", 10)))
			.record(snapshot(MONDAY, ImmutableMap.of("Wintertodt", 20)), UTC);

		assertEquals(Collections.emptyList(), gains.breakdown(GainPeriod.TODAY, EnumSet.of(BossCategory.PVM), MONDAY, UTC));
	}

	@Test
	public void breakdownUnknownForStartingPointsSavedBeforeIt()
	{
		KillSnapshot old = new KillSnapshot(millis(MONDAY), null, Collections.singletonMap(BossCategory.PVM, 100));
		PlayerGains gains = PlayerGains.first(old).record(snapshot(MONDAY, 130), UTC);

		assertNull(gains.breakdown(GainPeriod.TODAY, ALL, MONDAY, UTC));
		assertEquals(30, gains.gain(GainPeriod.TODAY, ALL, MONDAY, UTC));
	}

	@Test
	public void oldStartingPointGetsBossDetailWhenNothingWasKilledSince()
	{
		KillSnapshot old = new KillSnapshot(millis(MONDAY), null, Collections.singletonMap(BossCategory.PVM, 100));
		PlayerGains gains = PlayerGains.first(old)
			.record(snapshot(MONDAY, 100), UTC)
			.record(snapshot(MONDAY, 104), UTC);

		assertEquals(Collections.singletonList(new BossKills("Zulrah", 4)),
			gains.breakdown(GainPeriod.TODAY, ALL, MONDAY, UTC));
	}

	private static KillSnapshot snapshot(LocalDate day, int zulrahKills)
	{
		return snapshot(day, Collections.singletonMap("Zulrah", zulrahKills));
	}

	private static KillSnapshot snapshot(LocalDate day, Map<String, Integer> bosses)
	{
		return KillSnapshot.of(millis(day), bosses);
	}

	private static long millis(LocalDate day)
	{
		return day.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
	}
}
