package com.bosshighscore;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class Leaderboard
{
	private static final Comparator<PlayerTotals> MOST_KILLS_FIRST = Comparator
		.comparingInt(PlayerTotals::getTotalKills).reversed()
		.thenComparing(PlayerTotals::getName, String.CASE_INSENSITIVE_ORDER);

	private Leaderboard()
	{
	}

	static List<PlayerTotals> top(Collection<PlayerTotals> players, int limit)
	{
		return players.stream()
			.sorted(MOST_KILLS_FIRST)
			.limit(limit)
			.collect(Collectors.toList());
	}
}
