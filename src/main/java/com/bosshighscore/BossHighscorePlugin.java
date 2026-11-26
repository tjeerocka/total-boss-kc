package com.bosshighscore;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

/** Wires the parts together and passes RuneLite's events on; the work happens in the classes it injects. */
@PluginDescriptor(
	name = "Total Boss KC",
	description = "Total boss kills and top bosses for you, your friends and players you search",
	tags = {"boss", "kc", "killcount", "hiscore", "highscore", "friends", "total"}
)
public class BossHighscorePlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private PlayerStore store;

	@Inject
	private PanelPresenter presenter;

	@Inject
	private PlayerSearch search;

	@Inject
	private FriendsRefresher friends;

	@Inject
	private BossLookupMenu bossLookupMenu;

	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		store.load();
		BossHighscorePanel panel = new BossHighscorePanel(panelActions());
		presenter.attach(panel);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navigationButton = NavigationButton.builder()
			.tooltip("Total Boss KC")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		presenter.render();
		friends.start();
	}

	@Override
	protected void shutDown()
	{
		friends.stop();
		search.stop();
		clientToolbar.removeNavigation(navigationButton);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			friends.onLoginScreen();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			friends.onLoggedIn();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		friends.onGameTick();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Saved lookups are written to the same group; those already render themselves
		if (BossHighscoreConfig.GROUP.equals(event.getGroup()) && !PlayerStore.KEYS.contains(event.getKey()))
		{
			presenter.render();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Menu clicks run on the client thread; the toolbar is Swing
		bossLookupMenu.onMenuEntryAdded(event,
			() -> SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navigationButton)));
	}

	private PanelActions panelActions()
	{
		return new PanelActions()
		{
			@Override
			public void search(String name)
			{
				search.search(name);
			}

			@Override
			public void refreshFriends()
			{
				friends.refreshNow();
			}

			@Override
			public void refreshPlayer(String name)
			{
				search.lookUp(name);
			}

			@Override
			public void removeFromHistory(String name)
			{
				search.removeFromHistory(name);
			}

			@Override
			public void clearHistory()
			{
				search.clearHistory();
			}
		};
	}

	@Provides
	BossHighscoreConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossHighscoreConfig.class);
	}
}
