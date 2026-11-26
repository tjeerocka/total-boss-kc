package com.bosshighscore;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NameableContainer;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Total Boss KC",
	description = "Total boss kills and top bosses for you, your friends and players you search",
	tags = {"boss", "kc", "killcount", "hiscore", "highscore", "friends", "total"}
)
public class BossHighscorePlugin extends Plugin
{
	private static final int FRIENDS_SHOWN = 20;
	private static final String LOOKUP_OPTION = "Boss lookup";
	// Options that only show up on player names: chat, friends list, clan/friends chat and players in the world
	private static final Set<String> PLAYER_NAME_OPTIONS = ImmutableSet.of("Message", "Add ignore", "Remove friend", "Trade with");
	// A player searched again within this time is shown from the saved lookup instead of asking the hiscores again
	private static final Duration SEARCH_COOLDOWN = Duration.ofMinutes(1);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SearchCountdown searchCountdown;

	@Inject
	private BossHighscoreConfig config;

	@Inject
	private PlayerStore store;

	@Inject
	private BossLookupService lookups;

	@Inject
	private ScheduledExecutorService executor;

	private BossHighscorePanel panel;
	private NavigationButton navigationButton;
	// Set while waiting for the friends list to finish loading after login, null otherwise
	private FriendListLoad friendListLoad;
	private boolean friendsRefreshedThisLogin;
	private ScheduledFuture<?> midnightRefresh;

	@Override
	protected void startUp()
	{
		store.load();
		panel = new BossHighscorePanel(new PanelActions()
		{
			@Override
			public void search(String name)
			{
				BossHighscorePlugin.this.search(name);
			}

			@Override
			public void refreshFriends()
			{
				refreshFriendsNow();
			}

			@Override
			public void refreshPlayer(String name)
			{
				lookUp(name);
			}

			@Override
			public void removeFromHistory(String name)
			{
				BossHighscorePlugin.this.removeFromHistory(name);
			}

			@Override
			public void clearHistory()
			{
				BossHighscorePlugin.this.clearHistory();
			}
		});

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navigationButton = NavigationButton.builder()
			.tooltip("Total Boss KC")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		render();
		scheduleMidnightRefresh();
	}

	@Override
	protected void shutDown()
	{
		midnightRefresh.cancel(false);
		searchCountdown.stop();
		clientToolbar.removeNavigation(navigationButton);
		friendsRefreshedThisLogin = false;
		friendListLoad = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// LOGGED_IN also fires after every loading screen, so only refresh once per login
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			friendsRefreshedThisLogin = false;
		}
		else if (event.getGameState() == GameState.LOGGED_IN && !friendsRefreshedThisLogin)
		{
			friendsRefreshedThisLogin = true;
			friendListLoad = new FriendListLoad();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (friendListLoad == null)
		{
			return;
		}

		NameableContainer<Friend> container = client.getFriendContainer();
		if (friendListLoad.tick(container == null ? -1 : container.getCount()))
		{
			friendListLoad = null;
			refreshFriends(false);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Saved lookups are written to the same group; those already render themselves
		if (BossHighscoreConfig.GROUP.equals(event.getGroup()) && !PlayerStore.KEYS.contains(event.getKey()))
		{
			render();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!PLAYER_NAME_OPTIONS.contains(Text.removeTags(event.getOption())))
		{
			return;
		}

		String name = MenuTargets.playerName(event.getTarget());
		if (name.isEmpty())
		{
			return;
		}

		// Index 0 is Cancel at the bottom, so 1 puts it last, right above Cancel
		client.createMenuEntry(1)
			.setOption(LOOKUP_OPTION)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(entry ->
			{
				// Menu clicks run on the client thread; the toolbar is Swing
				SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navigationButton));
				search(name);
			});
	}

	private void search(String typedName)
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
			if (!saved.hasBossKills())
			{
				setStatus(name + " has no boss kills on the hiscores");
				SwingUtilities.invokeLater(() -> panel.showWithoutKills(name));
				return;
			}
			setStatus("");
			return;
		}

		lookUp(name);
	}

	/** Asks the hiscores now, skipping the saved result; waits out the cooldown first if needed. */
	private void lookUp(String name)
	{
		// Set before starting, so a fast result can't be overwritten by it
		setStatus("Looking up " + name + "...");
		boolean started = lookups.lookupSearch(name, totals ->
		{
			if (!totals.hasBossKills())
			{
				store.put(totals);
				setStatus(name + " has no boss kills on the hiscores");
				SwingUtilities.invokeLater(() -> panel.showWithoutKills(name));
				return;
			}
			store.put(totals);
			showSearched(totals);
			setStatus("");
		}, missing -> setStatus("No hiscores found for " + missing));

		if (!started)
		{
			searchCountdown.searchAfterCooldown(name, this::setStatus, this::lookUp);
		}
	}

	private void showSearched(PlayerTotals totals)
	{
		String name = totals.getName();
		boolean friend = store.isFriend(name);
		if (!friend)
		{
			store.addToHistory(name);
		}
		render();
		SwingUtilities.invokeLater(() -> panel.select(name, friend));
	}

	private void removeFromHistory(String name)
	{
		store.removeFromHistory(name);
		render();
	}

	private void clearHistory()
	{
		store.clearHistory();
		render();
	}

	private void scheduleMidnightRefresh()
	{
		long delayMillis = MidnightTiming.untilNextRun(ZonedDateTime.now(), store.friendNames().size()).toMillis();
		midnightRefresh = executor.schedule(this::refreshBeforeMidnight, delayMillis, TimeUnit.MILLISECONDS);
	}

	/** Uses the saved friends list, so it also works while logged out, as long as RuneLite is open. */
	private void refreshBeforeMidnight()
	{
		for (String name : store.friendNames())
		{
			lookups.lookupBatch(name, this::saveAndRender, missing -> { });
		}
		scheduleMidnightRefresh();
	}

	private void refreshFriendsNow()
	{
		clientThread.invokeLater(() -> refreshFriends(true));
	}

	/** Runs on the client thread. Without force, only friends with old or missing totals are looked up. */
	private void refreshFriends(boolean force)
	{
		List<String> names = readFriendNames();
		if (names.isEmpty())
		{
			setStatus("Log in to refresh your friends");
			return;
		}
		store.setFriends(names);

		long now = System.currentTimeMillis();
		Duration maxAge = Duration.ofHours(config.refreshHours());
		for (String name : names)
		{
			PlayerTotals known = store.get(name);
			if (force || known == null || known.isStale(now, maxAge) || store.lacksBossDetail(name))
			{
				lookups.lookupBatch(name, this::saveAndRender, missing -> { });
			}
		}
		render();
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
		render();
	}

	private void render()
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

	private void setStatus(String text)
	{
		SwingUtilities.invokeLater(() -> panel.setStatus(text));
	}

	@Provides
	BossHighscoreConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossHighscoreConfig.class);
	}
}
