package com.bosshighscore;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class Leaderboard
{
	private Leaderboard()
	{
	}

	/** Most kills first. Players without kills in these categories have nothing to compare, so they're left out. */
	static List<PlayerTotals> top(Collection<PlayerTotals> players, Set<BossCategory> categories, int limit)
	{
		Comparator<PlayerTotals> mostKillsFirst = Comparator
			.comparingInt((PlayerTotals player) -> player.totalKills(categories)).reversed()
			.thenComparing(PlayerTotals::getName, String.CASE_INSENSITIVE_ORDER);

		return players.stream()
			.filter(player -> player.totalKills(categories) > 0)
			.sorted(mostKillsFirst)
			.limit(limit)
			.collect(Collectors.toList());
	}
}
