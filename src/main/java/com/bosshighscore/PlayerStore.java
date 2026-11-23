package com.bosshighscore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Looked-up totals, search history and the last seen friends list, saved in the RuneLite config. */
@Singleton
class PlayerStore
{
	private static final String PLAYERS_KEY = "players";
	private static final String HISTORY_KEY = "history";
	private static final String FRIENDS_KEY = "friends";
	private static final int HISTORY_LIMIT = 50;
	private static final Type PLAYERS_TYPE = new TypeToken<Map<String, PlayerTotals>>()
	{
	}.getType();
	private static final Type NAMES_TYPE = new TypeToken<List<String>>()
	{
	}.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	private Map<String, PlayerTotals> players = new HashMap<>();
	private List<String> history = new ArrayList<>();
	private List<String> friends = new ArrayList<>();

	@Inject
	PlayerStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	synchronized void load()
	{
		players = read(PLAYERS_KEY, PLAYERS_TYPE, new HashMap<>());
		history = read(HISTORY_KEY, NAMES_TYPE, new ArrayList<>());
		friends = read(FRIENDS_KEY, NAMES_TYPE, new ArrayList<>());
	}

	synchronized PlayerTotals get(String name)
	{
		return players.get(key(name));
	}

	synchronized void put(PlayerTotals totals)
	{
		players.put(key(totals.getName()), totals);
		write(PLAYERS_KEY, players);
	}

	synchronized void addToHistory(String name)
	{
		history.removeIf(name::equalsIgnoreCase);
		history.add(0, name);
		if (history.size() > HISTORY_LIMIT)
		{
			history = new ArrayList<>(history.subList(0, HISTORY_LIMIT));
		}
		write(HISTORY_KEY, history);
	}

	synchronized void setFriends(List<String> names)
	{
		friends = new ArrayList<>(names);
		write(FRIENDS_KEY, friends);
	}

	synchronized List<String> friendNames()
	{
		return new ArrayList<>(friends);
	}

	/** Searched players with known totals, newest search first. */
	synchronized List<PlayerTotals> history()
	{
		return known(history);
	}

	synchronized List<PlayerTotals> friends()
	{
		return known(friends);
	}

	private List<PlayerTotals> known(List<String> names)
	{
		return names.stream()
			.map(this::get)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private <T> T read(String configKey, Type type, T fallback)
	{
		String json = configManager.getConfiguration(BossHighscoreConfig.GROUP, configKey);
		if (json == null)
		{
			return fallback;
		}
		T value = gson.fromJson(json, type);
		return value == null ? fallback : value;
	}

	private void write(String configKey, Object value)
	{
		configManager.setConfiguration(BossHighscoreConfig.GROUP, configKey, gson.toJson(value));
	}

	private static String key(String name)
	{
		return name.toLowerCase(Locale.ROOT);
	}
}
