package com.bosshighscore;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.hiscore.Skill;
import org.junit.Test;

public class BossTotalsTest
{
	private static final Skill UNRANKED = new Skill(-1, -1, -1);

	@Test
	public void sumsRankedBossesMostKillsFirst()
	{
		HiscoreResult result = mock(HiscoreResult.class);
		when(result.getSkill(any())).thenReturn(UNRANKED);
		when(result.getSkill(HiscoreSkill.ZULRAH)).thenReturn(new Skill(1, 129, -1));
		when(result.getSkill(HiscoreSkill.KRAKEN)).thenReturn(new Skill(1, 982, -1));

		PlayerTotals totals = BossTotals.from("tjeerocka", result, 42);

		assertEquals(1111, totals.getTotalKills());
		assertEquals(Arrays.asList(
			new BossKills(HiscoreSkill.KRAKEN.getName(), 982),
			new BossKills(HiscoreSkill.ZULRAH.getName(), 129)
		), totals.getBosses());
		assertEquals(42, totals.getUpdatedMillis());
	}

	@Test
	public void ignoresSkillsAndActivities()
	{
		HiscoreResult result = mock(HiscoreResult.class);
		when(result.getSkill(any())).thenReturn(new Skill(1, 99, 13_034_431));

		PlayerTotals totals = BossTotals.from("x", result, 0);

		long bossCount = Arrays.stream(HiscoreSkill.values())
			.filter(skill -> skill.getType() == HiscoreSkillType.BOSS)
			.count();
		assertEquals(bossCount * 99, totals.getTotalKills());
	}
}
