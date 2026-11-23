package com.bosshighscore;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.GameState;
import net.runelite.api.NameableContainer;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
	name = "Boss Highscore",
	description = "Total boss kills and top bosses for you, your friends and players you search",
	tags = {"boss", "kc", "killcount", "hiscore", "highscore", "friends", "total"}
)
public class BossHighscorePlugin extends Plugin
{
	private static final int FRIENDS_SHOWN = 20;
	// The friends list arrives a few ticks after login
	private static final int TICKS_UNTIL_FRIENDS_LOADED = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private BossHighscoreConfig config;

	@Inject
	private PlayerStore store;

	@Inject
	private BossLookupService lookups;

	private BossHighscorePanel panel;
	private NavigationButton navigationButton;
	private int ticksUntilFriendsRefresh;
	private boolean friendsRefreshedThisLogin;

	@Override
	protected void startUp()
	{
		store.load();
		panel = new BossHighscorePanel(this::search, this::refreshFriendsNow);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navigationButton = NavigationButton.builder()
			.tooltip("Boss Highscore")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		render();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		friendsRefreshedThisLogin = false;
		ticksUntilFriendsRefresh = 0;
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
			ticksUntilFriendsRefresh = TICKS_UNTIL_FRIENDS_LOADED;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (ticksUntilFriendsRefresh > 0 && --ticksUntilFriendsRefresh == 0)
		{
			refreshFriends(false);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (BossHighscoreConfig.GROUP.equals(event.getGroup())
			&& BossHighscoreConfig.TOP_BOSS_COUNT.equals(event.getKey()))
		{
			render();
		}
	}

	private void search(String typedName)
	{
		String name = Text.toJagexName(typedName.trim());
		if (name.isEmpty())
		{
			return;
		}

		setStatus("Looking up " + name + "...");
		lookups.lookupNow(name, totals ->
		{
			store.addToHistory(totals.getName());
			saveAndRender(totals);
			setStatus("");
			SwingUtilities.invokeLater(() -> panel.select(totals.getName()));
		}, missing -> setStatus("No hiscores found for " + missing));
	}

	private void refreshFriendsNow()
	{
		clientThread.invokeLater(() -> refreshFriends(true));
	}

	/** Runs on the client thread. Without force, only friends never looked up before are looked up. */
	private void refreshFriends(boolean force)
	{
		List<String> names = readFriendNames();
		if (names.isEmpty())
		{
			setStatus("Log in to refresh your friends");
			return;
		}
		store.setFriends(names);

		for (String name : names)
		{
			if (force || store.get(name) == null)
			{
				lookups.lookupSpaced(name, this::saveAndRender, missing -> { });
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
		List<PlayerTotals> friends = Leaderboard.top(store.friends(), FRIENDS_SHOWN);
		List<PlayerTotals> history = store.history();
		int topBossCount = config.topBossCount();
		SwingUtilities.invokeLater(() -> panel.render(friends, history, topBossCount));
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
