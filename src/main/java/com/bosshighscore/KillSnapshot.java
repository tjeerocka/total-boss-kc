package com.bosshighscore;

import java.util.Map;
import java.util.Set;
import lombok.Value;

/** Kills at one moment: enough to work out gains without keeping every lookup. */
@Value
public class KillSnapshot
{
	long millis;
	/** Kills per boss by hiscore name. Null in snapshots saved before gains per boss existed. */
	Map<String, Integer> bosses;
	/** Kills per category. Only in snapshots saved before gains per boss existed; null otherwise. */
	Map<BossCategory, Integer> kills;

	static KillSnapshot of(long millis, Map<String, Integer> bosses)
	{
		return new KillSnapshot(millis, bosses, null);
	}

	public boolean hasBossDetail()
	{
		return bosses != null;
	}

	public int kills(String boss)
	{
		return bosses.getOrDefault(boss, 0);
	}

	public int total(Set<BossCategory> categories)
	{
		if (!hasBossDetail())
		{
			return categories.stream()
				.mapToInt(category -> kills.getOrDefault(category, 0))
				.sum();
		}
		return bosses.entrySet().stream()
			.filter(boss -> categories.contains(BossCategory.of(boss.getKey())))
			.mapToInt(Map.Entry::getValue)
			.sum();
	}
}
