package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public class MidnightTimingTest
{
	private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

	@Test
	public void runsBeforeMidnightWithTimeForEveryFriend()
	{
		ZonedDateTime noon = ZonedDateTime.of(2026, 11, 26, 12, 0, 0, 0, BERLIN);

		// 20 friends: 20s of lookups + 30s spare, so 23:59:10
		assertEquals(Duration.ofHours(11).plusMinutes(59).plusSeconds(10), MidnightTiming.untilNextRun(noon, 20));
	}

	@Test
	public void insideTonightsWindowWaitsForTomorrowNight()
	{
		ZonedDateTime justStarted = ZonedDateTime.of(2026, 11, 26, 23, 59, 10, 0, BERLIN);

		assertEquals(Duration.ofDays(1), MidnightTiming.untilNextRun(justStarted, 20));
	}

	@Test
	public void followsLocalMidnightAcrossDaylightSavingChange()
	{
		// Clocks go forward at 02:00 on 29 March 2026; midnight itself is unaffected
		ZonedDateTime evening = ZonedDateTime.of(2026, 3, 28, 22, 0, 0, 0, BERLIN);

		assertEquals(Duration.ofHours(1).plusMinutes(59).plusSeconds(10), MidnightTiming.untilNextRun(evening, 20));
	}
}
