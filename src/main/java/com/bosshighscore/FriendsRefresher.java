package com.bosshighscore;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.NameableContainer;
import net.runelite.api.Player;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;

/** Keeps friends' kills up to date: once the friends list has loaded after login, on request, and at midnight. */
@Singleton
class FriendsRefresher
{
	// Friends without any boss on the hiscores rarely change, so they're looked up less often to save requests
	private static final Duration NO_KILLS_REFRESH = Duration.ofDays(2);

	private final Client client;
	private final ClientThread clientThread;
	private final ScheduledExecutorService executor;
	private final PlayerStore store;
	private final BossLookupService lookups;
	private final BossHighscoreConfig config;
	private final PanelPresenter presenter;

	// Set while waiting for the friends list to finish loading after login, null otherwise
	private FriendListLoad friendListLoad;
	private boolean refreshedThisLogin;
	private ScheduledFuture<?> midnightRefresh;

	@Inject
	FriendsRefresher(Client client, ClientThread clientThread, ScheduledExecutorService executor, PlayerStore store,
		BossLookupService lookups, BossHighscoreConfig config, PanelPresenter presenter)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.executor = executor;
		this.store = store;
		this.lookups = lookups;
		this.config = config;
		this.presenter = presenter;
	}

	void start()
	{
		scheduleMidnightRefresh();
	}

	void stop()
	{
		if (midnightRefresh != null)
		{
			midnightRefresh.cancel(false);
		}
		refreshedThisLogin = false;
		friendListLoad = null;
	}

	void onLoginScreen()
	{
		refreshedThisLogin = false;
	}

	/** LOGGED_IN also fires after every loading screen, so this only starts waiting once per login. */
	void onLoggedIn()
	{
		if (!refreshedThisLogin)
		{
			refreshedThisLogin = true;
			friendListLoad = new FriendListLoad();
		}
	}

	void onGameTick()
	{
		if (friendListLoad == null)
		{
			return;
		}

		NameableContainer<Friend> container = client.getFriendContainer();
		if (friendListLoad.tick(container == null ? -1 : container.getCount()))
		{
			friendListLoad = null;
			refresh(false);
		}
	}

	void refreshNow()
	{
		clientThread.invokeLater(() -> refresh(true));
	}

	/** Runs on the client thread. Without force, only friends whose kills are due for a lookup are looked up. */
	private void refresh(boolean force)
	{
		List<String> names = readFriendNames();
		if (names.isEmpty())
		{
			presenter.setStatus("Log in to refresh your friends");
			return;
		}
		store.setFriends(names);

		long now = System.currentTimeMillis();
		Duration maxAge = Duration.ofHours(config.refreshHours());
		for (String name : names)
		{
			if (force || isDue(store.get(name), now, maxAge) || store.lacksBossDetail(name))
			{
				lookups.lookupBatch(name, this::saveAndRender, missing -> { });
			}
		}
		presenter.render();
	}

	private void scheduleMidnightRefresh()
	{
		long delayMillis = MidnightTiming.untilNextMidnight(ZonedDateTime.now()).toMillis();
		midnightRefresh = executor.schedule(this::refreshAtMidnight, delayMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * Starts the new day for every friend: each lookup becomes their starting point for today. Uses the saved friends
	 * list, so it also works while logged out, as long as RuneLite is open.
	 */
	private void refreshAtMidnight()
	{
		long now = System.currentTimeMillis();
		for (String name : store.friendNames())
		{
			if (isDue(store.get(name), now, Duration.ZERO))
			{
				lookups.lookupBatch(name, totals ->
				{
					store.putAtMidnight(totals);
					presenter.render();
				}, missing -> { });
			}
		}
		scheduleMidnightRefresh();
	}

	/** Friends without boss kills wait {@link #NO_KILLS_REFRESH}; everyone else waits {@code maxAge}. */
	private static boolean isDue(PlayerTotals known, long now, Duration maxAge)
	{
		if (known == null)
		{
			return true;
		}
		return known.isStale(now, known.hasBossKills() ? maxAge : NO_KILLS_REFRESH);
	}

	/** You plus your friends list, or empty while it hasn't loaded. */
	private List<String> readFriendNames()
	{
		List<String> names = new ArrayList<>();
		NameableContainer<Friend> container = client.getFriendContainer();
		if (container == null)
		{
			return names;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && localPlayer.getName() != null)
		{
			names.add(Text.toJagexName(localPlayer.getName()));
		}
		for (Friend friend : container.getMembers())
		{
			names.add(Text.toJagexName(friend.getName()));
		}
		return names;
	}

	private void saveAndRender(PlayerTotals totals)
	{
		store.put(totals);
		presenter.render();
	}
}
