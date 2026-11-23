package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class LeaderboardTest
{
	private static final Set<BossCategory> ALL = EnumSet.allOf(BossCategory.class);

	@Test
	public void mostKillsFirstWithoutPlayersWithoutKills()
	{
		List<PlayerTotals> top = Leaderboard.top(Arrays.asList(
			player("Moss Gnome"),
			player("tjeerocka", pvm(2866)),
			player("Kraken Kid", pvm(1730))
		), ALL, 20);

		assertEquals(Arrays.asList("tjeerocka", "Kraken Kid"), names(top));
	}

	@Test
	public void ranksOnCountedCategoriesOnly()
	{
		List<PlayerTotals> top = Leaderboard.top(Arrays.asList(
			player("skiller", new BossKills("Wintertodt", 900)),
			player("raider", new BossKills("Theatre of Blood", 50))
		), EnumSet.of(BossCategory.RAIDS), 20);

		assertEquals(Collections.singletonList("raider"), names(top));
	}

	@Test
	public void cutsOffAtLimit()
	{
		List<PlayerTotals> top = Leaderboard.top(Arrays.asList(
			player("a", pvm(1)), player("b", pvm(2)), player("c", pvm(3))
		), ALL, 2);

		assertEquals(Arrays.asList("c", "b"), names(top));
	}

	@Test
	public void sameTotalSortsByName()
	{
		List<PlayerTotals> top = Leaderboard.top(Arrays.asList(player("bob", pvm(5)), player("Alice", pvm(5))), ALL, 20);

		assertEquals(Arrays.asList("Alice", "bob"), names(top));
	}

	private static BossKills pvm(int kills)
	{
		return new BossKills("Zulrah", kills);
	}

	private static PlayerTotals player(String name, BossKills... bosses)
	{
		return new PlayerTotals(name, Arrays.asList(bosses), 0);
	}

	private static List<String> names(List<PlayerTotals> players)
	{
		return players.stream().map(PlayerTotals::getName).collect(Collectors.toList());
	}
}
