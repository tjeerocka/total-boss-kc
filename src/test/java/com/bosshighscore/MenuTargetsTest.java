package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MenuTargetsTest
{
	@Test
	public void stripsColoursAndCombatLevel()
	{
		assertEquals("Zezima", MenuTargets.playerName("<col=ffffff>Zezima<col=40ff00>  (level-126)"));
	}

	@Test
	public void keepsPlainChatName()
	{
		assertEquals("tjeerocka", MenuTargets.playerName("<col=ffffff>tjeerocka"));
	}

	@Test
	public void turnsNonBreakingSpacesIntoSpaces()
	{
		assertEquals("Moss Gnome", MenuTargets.playerName("Moss Gnome"));
	}
}
