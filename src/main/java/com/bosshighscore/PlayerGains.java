package com.bosshighscore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
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
			nextBaselines.put(period, withBossDetail(newPeriodStarted ? latest : baselines.get(period)));
		}
		return new PlayerGains(next, nextBaselines);
	}

	/**
	 * A starting point saved before gains per boss existed gets the latest lookup's per-boss counts when nothing was
	 * killed in between: kill counts only go up, so an equal total means every boss is equal too.
	 */
	private KillSnapshot withBossDetail(KillSnapshot baseline)
	{
		Set<BossCategory> all = EnumSet.allOf(BossCategory.class);
		if (baseline.hasBossDetail() || !latest.hasBossDetail() || baseline.total(all) != latest.total(all))
		{
			return baseline;
		}
		return KillSnapshot.of(baseline.getMillis(), latest.getBosses());
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
	 * Every boss that went up in the period, most gained first. When the period's starting point was saved before gains
	 * per boss existed, the bosses count from the earliest later starting point that has them, and the kills before
	 * that are returned as unknown.
	 *
	 * @return null when no starting point in the period has per-boss counts yet
	 */
	GainBreakdown breakdown(GainPeriod period, Set<BossCategory> categories, LocalDate today, ZoneId zone)
	{
		if (day(latest, zone).isBefore(period.start(today)))
		{
			return new GainBreakdown(Collections.emptyList(), 0);
		}

		KillSnapshot baseline = baselines.get(period);
		KillSnapshot from = baseline.hasBossDetail() ? baseline : earliestWithBossDetailSince(baseline);
		if (from == null || !latest.hasBossDetail())
		{
			return null;
		}

		List<BossKills> bosses = latest.getBosses().entrySet().stream()
			.filter(boss -> categories.contains(BossCategory.of(boss.getKey())))
			.map(boss -> new BossKills(boss.getKey(), boss.getValue() - from.kills(boss.getKey())))
			.filter(gain -> gain.getKills() > 0)
			.sorted(Comparator.comparingInt(BossKills::getKills).reversed())
			.collect(Collectors.toList());
		return new GainBreakdown(bosses, from.total(categories) - baseline.total(categories));
	}

	private KillSnapshot earliestWithBossDetailSince(KillSnapshot baseline)
	{
		return baselines.values().stream()
			.filter(KillSnapshot::hasBossDetail)
			.filter(snapshot -> snapshot.getMillis() >= baseline.getMillis())
			.min(Comparator.comparingLong(KillSnapshot::getMillis))
			.orElse(null);
	}

	private static LocalDate day(KillSnapshot snapshot, ZoneId zone)
	{
		return Instant.ofEpochMilli(snapshot.getMillis()).atZone(zone).toLocalDate();
	}
}
