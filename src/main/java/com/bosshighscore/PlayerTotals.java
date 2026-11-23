package com.bosshighscore;

import java.util.List;
import lombok.Value;

/** One player's boss kills as the hiscores showed them at {@code updatedMillis}. Bosses are sorted, most kills first. */
@Value
public class PlayerTotals
{
	String name;
	int totalKills;
	List<BossKills> bosses;
	long updatedMillis;

	/** False when no boss reaches the hiscore minimum (5 kills). */
	public boolean isRanked()
	{
		return totalKills > 0;
	}

	public List<BossKills> topBosses(int count)
	{
		return bosses.subList(0, Math.min(count, bosses.size()));
	}
}
