package com.bosshighscore;

import lombok.Value;

@Value
public class BossKills
{
	String boss;
	int kills;
	BossCategory category;
}
