package com.bosshighscore;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/** Everything the panel shows, built by the plugin. Gains are keyed by lower case player name. */
@Value
class PanelState
{
	static final PanelState EMPTY = new PanelState(
		Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), 5, Collections.emptySet());

	List<PlayerTotals> friends;
	List<PlayerTotals> history;
	Map<String, PlayerGains> gains;
	int topBossCount;
	Set<BossCategory> categories;
}
