package com.bosshighscore;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GainPeriod
{
	TODAY("Today"),
	WEEK("Week"),
	MONTH("Month"),
	YEAR("Year");

	private final String label;

	/** First day of the period that {@code date} falls in. Weeks start on Monday. */
	LocalDate start(LocalDate date)
	{
		switch (this)
		{
			case WEEK:
				return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			case MONTH:
				return date.withDayOfMonth(1);
			case YEAR:
				return date.withDayOfYear(1);
			default:
				return date;
		}
	}
}
