package com.bosshighscore;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.util.Text;

/** Adds "Boss lookup" to the right-click menu of player names. */
@Singleton
class BossLookupMenu
{
	private static final String LOOKUP_OPTION = "Boss lookup";
	// Options that only show up on player names: chat, friends list, clan/friends chat and players in the world
	private static final Set<String> PLAYER_NAME_OPTIONS = ImmutableSet.of("Message", "Add ignore", "Remove friend", "Trade with");

	private final Client client;
	private final PlayerSearch search;

	@Inject
	BossLookupMenu(Client client, PlayerSearch search)
	{
		this.client = client;
		this.search = search;
	}

	/** @param openPanel shows the side panel; called on the client thread */
	void onMenuEntryAdded(MenuEntryAdded event, Runnable openPanel)
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
				openPanel.run();
				search.search(name);
			});
	}
}
