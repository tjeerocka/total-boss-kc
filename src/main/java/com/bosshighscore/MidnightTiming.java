package com.bosshighscore;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * When to look friends up before local midnight. The last lookup before midnight becomes the next day's starting
 * point, so doing it just before midnight keeps late-evening kills in the day they happened. It starts early enough
 * for every friend to be looked up (one per second) before 00:00.
 */
final class MidnightTiming
{
	private static final Duration EXTRA_LEAD = Duration.ofSeconds(30);

	private MidnightTiming()
	{
	}

	static Duration untilNextRun(ZonedDateTime now, int playerCount)
	{
		Duration lead = Duration.ofSeconds(playerCount).plus(EXTRA_LEAD);
		ZonedDateTime run = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone()).minus(lead);
		if (!run.isAfter(now))
		{
			// Already inside tonight's window (or just ran): the next run is before the following midnight
			run = now.toLocalDate().plusDays(2).atStartOfDay(now.getZone()).minus(lead);
		}
		return Duration.between(now, run);
	}
}
