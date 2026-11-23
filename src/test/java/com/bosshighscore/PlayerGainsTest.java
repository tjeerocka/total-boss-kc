package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumSet;
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

	private static KillSnapshot snapshot(LocalDate day, int pvmKills)
	{
		long millis = day.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
		return new KillSnapshot(millis, Collections.singletonMap(BossCategory.PVM, pvmKills));
	}
}
