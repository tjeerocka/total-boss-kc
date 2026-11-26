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
	public void waitsUntilLocalMidnight()
	{
		ZonedDateTime noon = ZonedDateTime.of(2026, 11, 26, 12, 0, 0, 0, BERLIN);

		assertEquals(Duration.ofHours(12), MidnightTiming.untilNextMidnight(noon));
	}

	@Test
	public void atMidnightWaitsForTheNextOne()
	{
		ZonedDateTime midnight = ZonedDateTime.of(2026, 11, 27, 0, 0, 0, 0, BERLIN);

		assertEquals(Duration.ofDays(1), MidnightTiming.untilNextMidnight(midnight));
	}

	@Test
	public void followsLocalMidnightAcrossDaylightSavingChange()
	{
		// Clocks go forward at 02:00 on 29 March 2026; midnight itself is unaffected
		ZonedDateTime evening = ZonedDateTime.of(2026, 3, 28, 22, 0, 0, 0, BERLIN);

		assertEquals(Duration.ofHours(2), MidnightTiming.untilNextMidnight(evening));
	}
}
