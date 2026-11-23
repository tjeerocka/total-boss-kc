package com.bosshighscore;

import lombok.Value;

@Value
public class BossKills
{
	String boss;
	int kills;

	// Worked out from the name, so saved lookups follow category changes without a new lookup
	public BossCategory getCategory()
	{
		return BossCategory.of(boss);
	}
}
