package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
		), gains.breakdown(GainPeriod.TODAY, ALL, MONDAY, UTC).getBosses());
	}

	@Test
	public void breakdownFollowsCountedCategories()
	{
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, ImmutableMap.of("Wintertodt", 10)))
			.record(snapshot(MONDAY, ImmutableMap.of("Wintertodt", 20)), UTC);

		assertEquals(Collections.emptyList(),
			gains.breakdown(GainPeriod.TODAY, EnumSet.of(BossCategory.PVM), MONDAY, UTC).getBosses());
	}

	@Test
	public void breakdownUnknownForStartingPointsSavedBeforeIt()
	{
		KillSnapshot old = new KillSnapshot(millis(MONDAY), null, Collections.singletonMap(BossCategory.PVM, 100));

		assertNull(PlayerGains.first(old).breakdown(GainPeriod.TODAY, ALL, MONDAY, UTC));

		PlayerGains gains = PlayerGains.first(old).record(snapshot(MONDAY, 130), UTC);
		GainBreakdown today = gains.breakdown(GainPeriod.TODAY, ALL, MONDAY, UTC);
		assertEquals(Collections.emptyList(), today.getBosses());
		assertEquals(30, today.getUnknownKills());
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
			gains.breakdown(GainPeriod.TODAY, ALL, MONDAY, UTC).getBosses());
	}

	@Test
	public void olderPeriodShowsKnownBossesAndTheRestAsUnknown()
	{
		LocalDate tuesday = MONDAY.plusDays(1);
		KillSnapshot old = new KillSnapshot(millis(MONDAY), null, Collections.singletonMap(BossCategory.PVM, 100));
		PlayerGains gains = PlayerGains.first(old)
			.record(snapshot(MONDAY, 120), UTC)
			.record(snapshot(tuesday, 150), UTC);

		GainBreakdown week = gains.breakdown(GainPeriod.WEEK, ALL, tuesday, UTC);

		assertEquals(Collections.singletonList(new BossKills("Zulrah", 30)), week.getBosses());
		assertEquals(20, week.getUnknownKills());
	}

	@Test
	public void olderPeriodKeepsBossesFromDaysBefore()
	{
		LocalDate tuesday = MONDAY.plusDays(1);
		LocalDate wednesday = MONDAY.plusDays(2);
		KillSnapshot old = new KillSnapshot(millis(MONDAY), null, Collections.singletonMap(BossCategory.PVM, 100));
		PlayerGains gains = PlayerGains.first(old)
			.record(snapshot(MONDAY, 120), UTC)
			.record(snapshot(tuesday, 150), UTC)
			.record(snapshot(wednesday, 170), UTC);

		GainBreakdown week = gains.breakdown(GainPeriod.WEEK, ALL, wednesday, UTC);

		// Tuesday's kills stay listed after Today has moved on to Wednesday
		assertEquals(Collections.singletonList(new BossKills("Zulrah", 50)), week.getBosses());
		assertEquals(20, week.getUnknownKills());
	}

	@Test
	public void midnightLookupStartsTheNewDay()
	{
		LocalDate tuesday = MONDAY.plusDays(1);
		PlayerGains gains = PlayerGains.first(snapshot(MONDAY, 100))
			.recordPeriodStart(snapshot(tuesday.atStartOfDay(), 110), UTC)
			.record(snapshot(tuesday, 125), UTC);

		// The 10 kills from Monday evening stay out of Tuesday
		assertEquals(15, gains.gain(GainPeriod.TODAY, ALL, tuesday, UTC));
		assertEquals(25, gains.gain(GainPeriod.WEEK, ALL, tuesday, UTC));
	}

	private static KillSnapshot snapshot(LocalDateTime time, int zulrahKills)
	{
		return KillSnapshot.of(time.toInstant(ZoneOffset.UTC).toEpochMilli(), Collections.singletonMap("Zulrah", zulrahKills));
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
