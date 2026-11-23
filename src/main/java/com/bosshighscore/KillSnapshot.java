package com.bosshighscore;

import java.util.Map;
import java.util.Set;
import lombok.Value;

/** Kills per category at one moment: enough to work out gains without keeping every lookup. */
@Value
public class KillSnapshot
{
	long millis;
	Map<BossCategory, Integer> kills;

	public int total(Set<BossCategory> categories)
	{
		return categories.stream()
			.mapToInt(category -> kills.getOrDefault(category, 0))
			.sum();
	}
}
