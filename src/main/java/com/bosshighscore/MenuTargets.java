package com.bosshighscore;

import net.runelite.client.util.Text;

final class MenuTargets
{
	private MenuTargets()
	{
	}

	/** "<col=ffffff>Zezima<col=40ff00>  (level-126)" becomes "Zezima". */
	static String playerName(String menuTarget)
	{
		String name = Text.removeTags(menuTarget);
		int levelStart = name.indexOf("(level-");
		if (levelStart >= 0)
		{
			name = name.substring(0, levelStart);
		}
		return Text.toJagexName(name.trim());
	}
}
