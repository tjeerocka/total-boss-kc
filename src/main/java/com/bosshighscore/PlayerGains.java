package com.bosshighscore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * Gains per period from only a handful of snapshots: the latest lookup, plus for each period the last lookup before
 * that period started. Until a period has such a lookup, its baseline is the first lookup ever made.
 */
@Value
public class PlayerGains
{
	KillSnapshot latest;
	Map<GainPeriod, KillSnapshot> baselines;

	static PlayerGains first(KillSnapshot snapshot)
	{
		Map<GainPeriod, KillSnapshot> baselines = new EnumMap<>(GainPeriod.class);
		for (GainPeriod period : GainPeriod.values())
		{
			baselines.put(period, snapshot);
		}
		return new PlayerGains(snapshot, baselines);
	}

	PlayerGains record(KillSnapshot next, ZoneId zone)
	{
		LocalDate latestDay = day(latest, zone);
		LocalDate nextDay = day(next, zone);
		Map<GainPeriod, KillSnapshot> nextBaselines = new EnumMap<>(GainPeriod.class);
		for (GainPeriod period : GainPeriod.values())
		{
			boolean newPeriodStarted = latestDay.isBefore(period.start(nextDay));
			nextBaselines.put(period, newPeriodStarted ? latest : baselines.get(period));
		}
		return new PlayerGains(next, nextBaselines);
	}

	int gain(GainPeriod period, Set<BossCategory> categories, LocalDate today, ZoneId zone)
	{
		// No lookup yet in this period: nothing gained that we know of
		if (day(latest, zone).isBefore(period.start(today)))
		{
			return 0;
		}
		return latest.total(categories) - baselines.get(period).total(categories);
	}

	/**
	 * Every boss that went up in the period, most gained first.
	 *
	 * @return null when the period's starting point was saved before gains per boss existed
	 */
	List<BossKills> breakdown(GainPeriod period, Set<BossCategory> categories, LocalDate today, ZoneId zone)
	{
		if (day(latest, zone).isBefore(period.start(today)))
		{
			return Collections.emptyList();
		}

		KillSnapshot baseline = baselines.get(period);
		if (!baseline.hasBossDetail() || !latest.hasBossDetail())
		{
			return null;
		}
		return latest.getBosses().entrySet().stream()
			.filter(boss -> categories.contains(BossCategory.of(boss.getKey())))
			.map(boss -> new BossKills(boss.getKey(), boss.getValue() - baseline.kills(boss.getKey())))
			.filter(gain -> gain.getKills() > 0)
			.sorted(Comparator.comparingInt(BossKills::getKills).reversed())
			.collect(Collectors.toList());
	}

	private static LocalDate day(KillSnapshot snapshot, ZoneId zone)
	{
		return Instant.ofEpochMilli(snapshot.getMillis()).atZone(zone).toLocalDate();
	}
}
