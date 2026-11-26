package com.bosshighscore;

import java.util.List;
import lombok.Value;

/** A period's gains per boss, most first, plus kills whose boss isn't known (saved before gains per boss existed). */
@Value
public class GainBreakdown
{
	List<BossKills> bosses;
	int unknownKills;
}
