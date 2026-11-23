package com.bosshighscore;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;

/** One player's boss kills as the hiscores showed them at {@code updatedMillis}. Bosses are sorted, most kills first. */
@Value
public class PlayerTotals
{
	String name;
	List<BossKills> bosses;
	long updatedMillis;

	/** False when no boss reaches the hiscore minimum (5 kills). */
	public boolean hasBossKills()
	{
		return !bosses.isEmpty();
	}

	public int totalKills(Set<BossCategory> categories)
	{
		return bosses.stream()
			.filter(boss -> categories.contains(boss.getCategory()))
			.mapToInt(BossKills::getKills)
			.sum();
	}

	public List<BossKills> topBosses(int count, Set<BossCategory> categories)
	{
		return bosses.stream()
			.filter(boss -> categories.contains(boss.getCategory()))
			.limit(count)
			.collect(Collectors.toList());
	}

	public boolean isStale(long nowMillis, Duration maxAge)
	{
		return nowMillis - updatedMillis > maxAge.toMillis();
	}

	public KillSnapshot snapshot()
	{
		Map<BossCategory, Integer> kills = new EnumMap<>(BossCategory.class);
		for (BossKills boss : bosses)
		{
			kills.merge(boss.getCategory(), boss.getKills(), Integer::sum);
		}
		return new KillSnapshot(updatedMillis, kills);
	}
}
