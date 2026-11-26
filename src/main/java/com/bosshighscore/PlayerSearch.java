package com.bosshighscore;

import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.util.Text;

/** Searching a player (search box, Boss lookup, right-click Refresh) and the search history. */
@Singleton
class PlayerSearch
{
	// A player searched again within this time is shown from the saved lookup instead of asking the hiscores again
	private static final Duration SEARCH_COOLDOWN = Duration.ofMinutes(1);

	private final PlayerStore store;
	private final BossLookupService lookups;
	private final SearchCountdown searchCountdown;
	private final PanelPresenter presenter;

	@Inject
	PlayerSearch(PlayerStore store, BossLookupService lookups, SearchCountdown searchCountdown, PanelPresenter presenter)
	{
		this.store = store;
		this.lookups = lookups;
		this.searchCountdown = searchCountdown;
		this.presenter = presenter;
	}

	void search(String typedName)
	{
		String name = Text.toJagexName(typedName.trim());
		if (name.isEmpty())
		{
			return;
		}

		// Searched before: show the saved kills straight away, the lookup below refreshes them
		PlayerTotals saved = store.get(name);
		if (saved != null && saved.hasBossKills())
		{
			showSearched(saved);
		}
		if (saved != null && !saved.isStale(System.currentTimeMillis(), SEARCH_COOLDOWN))
		{
			if (saved.hasBossKills())
			{
				presenter.setStatus("");
			}
			else
			{
				showWithoutKills(name);
			}
			return;
		}

		lookUp(name);
	}

	/** Asks the hiscores now, skipping the saved result; waits out the cooldown first if needed. */
	void lookUp(String name)
	{
		// Set before starting, so a fast result can't be overwritten by it
		presenter.setStatus("Looking up " + name + "...");
		boolean started = lookups.lookupSearch(name, totals ->
		{
			store.put(totals);
			if (totals.hasBossKills())
			{
				showSearched(totals);
				presenter.setStatus("");
			}
			else
			{
				showWithoutKills(name);
			}
		}, missing -> presenter.setStatus("No hiscores found for " + missing));

		if (!started)
		{
			searchCountdown.searchAfterCooldown(name, presenter::setStatus, this::lookUp);
		}
	}

	void removeFromHistory(String name)
	{
		store.removeFromHistory(name);
		presenter.render();
	}

	void clearHistory()
	{
		store.clearHistory();
		presenter.render();
	}

	void stop()
	{
		searchCountdown.stop();
	}

	private void showSearched(PlayerTotals totals)
	{
		String name = totals.getName();
		boolean friend = store.isFriend(name);
		if (!friend)
		{
			store.addToHistory(name);
		}
		presenter.render();
		presenter.select(name, friend);
	}

	private void showWithoutKills(String name)
	{
		presenter.setStatus(name + " has no boss kills on the hiscores");
		presenter.showWithoutKills(name);
	}
}
