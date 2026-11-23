package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class LeaderboardTest
{
	@Test
	public void mostKillsFirstWithoutUnranked()
	{
		List<PlayerTotals> top = Leaderboard.top(Arrays.asList(
			player("Moss Gnome", 0),
			player("tjeerocka", 2866),
			player("Kraken Kid", 1730)
		), 20);

		assertEquals(Arrays.asList("tjeerocka", "Kraken Kid"), names(top));
	}

	@Test
	public void cutsOffAtLimit()
	{
		List<PlayerTotals> top = Leaderboard.top(Arrays.asList(
			player("a", 1), player("b", 2), player("c", 3)
		), 2);

		assertEquals(Arrays.asList("c", "b"), names(top));
	}

	@Test
	public void sameTotalSortsByName()
	{
		List<PlayerTotals> top = Leaderboard.top(Arrays.asList(player("bob", 5), player("Alice", 5)), 20);

		assertEquals(Arrays.asList("Alice", "bob"), names(top));
	}

	private static PlayerTotals player(String name, int total)
	{
		return new PlayerTotals(name, total, Collections.emptyList(), 0);
	}

	private static List<String> names(List<PlayerTotals> players)
	{
		return players.stream().map(PlayerTotals::getName).collect(Collectors.toList());
	}
}
