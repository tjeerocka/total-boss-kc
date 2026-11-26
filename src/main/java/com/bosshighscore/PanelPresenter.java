package com.bosshighscore;

import java.util.EnumSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;

/** Builds what the panel shows from the saved data and config, and hands it to the panel on the Swing thread. */
@Singleton
class PanelPresenter
{
	private static final int FRIENDS_SHOWN = 20;

	private final PlayerStore store;
	private final BossHighscoreConfig config;
	private BossHighscorePanel panel;

	@Inject
	PanelPresenter(PlayerStore store, BossHighscoreConfig config)
	{
		this.store = store;
		this.config = config;
	}

	void attach(BossHighscorePanel panel)
	{
		this.panel = panel;
	}

	void render()
	{
		Set<BossCategory> categories = countedCategories();
		PanelState state = new PanelState(
			Leaderboard.top(store.friends(), categories, FRIENDS_SHOWN),
			Leaderboard.top(store.history(), categories, Integer.MAX_VALUE),
			store.gains(),
			config.topBossCount(),
			categories);
		SwingUtilities.invokeLater(() -> panel.render(state));
	}

	void setStatus(String text)
	{
		SwingUtilities.invokeLater(() -> panel.setStatus(text));
	}

	void select(String name, boolean friend)
	{
		SwingUtilities.invokeLater(() -> panel.select(name, friend));
	}

	void showWithoutKills(String name)
	{
		SwingUtilities.invokeLater(() -> panel.showWithoutKills(name));
	}

	private Set<BossCategory> countedCategories()
	{
		Set<BossCategory> categories = EnumSet.noneOf(BossCategory.class);
		if (config.includePvm())
		{
			categories.add(BossCategory.PVM);
		}
		if (config.includeSkilling())
		{
			categories.add(BossCategory.SKILLING);
		}
		if (config.includeRaids())
		{
			categories.add(BossCategory.RAIDS);
		}
		return categories;
	}
}
