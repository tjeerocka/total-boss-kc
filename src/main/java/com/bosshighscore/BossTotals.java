package com.bosshighscore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.hiscore.Skill;

/** Turns a hiscore lookup into a player's boss kills. */
final class BossTotals
{
	private BossTotals()
	{
	}

	static PlayerTotals from(String name, HiscoreResult result, long nowMillis)
	{
		List<BossKills> bosses = new ArrayList<>();
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if (skill.getType() != HiscoreSkillType.BOSS)
			{
				continue;
			}

			Skill entry = result.getSkill(skill);
			// Unranked bosses (below the hiscore minimum) come back as -1
			if (entry != null && entry.getLevel() > 0)
			{
				bosses.add(new BossKills(skill.getName(), entry.getLevel(), BossCategory.of(skill)));
			}
		}

		bosses.sort(Comparator.comparingInt(BossKills::getKills).reversed());
		return new PlayerTotals(name, bosses, nowMillis);
	}
}
