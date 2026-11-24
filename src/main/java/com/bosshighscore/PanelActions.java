package com.bosshighscore;

/** What the panel's buttons and menus ask the plugin to do. */
interface PanelActions
{
	void search(String name);

	void refreshFriends();

	/** Looks one player up again now, even if their saved kills are recent. */
	void refreshPlayer(String name);

	void removeFromHistory(String name);

	void clearHistory();
}
