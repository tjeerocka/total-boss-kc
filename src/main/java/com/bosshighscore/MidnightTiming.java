package com.bosshighscore;

import java.time.Duration;
import java.time.ZonedDateTime;

/** Time until the next local midnight, when friends are looked up to start the new day. */
final class MidnightTiming
{
	private MidnightTiming()
	{
	}

	static Duration untilNextMidnight(ZonedDateTime now)
	{
		ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone());
		return Duration.between(now, midnight);
	}
}
