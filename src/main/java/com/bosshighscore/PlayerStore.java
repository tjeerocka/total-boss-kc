package com.bosshighscore;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
	private static final String GAINS_KEY = "gains";
	static final Set<String> KEYS = ImmutableSet.of(PLAYERS_KEY, HISTORY_KEY, FRIENDS_KEY, GAINS_KEY);
	private static final int HISTORY_LIMIT = 50;
	private static final Type PLAYERS_TYPE = new TypeToken<Map<String, PlayerTotals>>()
	{
	}.getType();
	private static final Type GAINS_TYPE = new TypeToken<Map<String, PlayerGains>>()
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
	private Map<String, PlayerGains> gains = new HashMap<>();

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
		gains = read(GAINS_KEY, GAINS_TYPE, new HashMap<>());

		// Saved by an older version without a boss list: look them up again instead
		players.values().removeIf(totals -> totals.getBosses() == null);
		gains.keySet().retainAll(players.keySet());
	}

	synchronized PlayerTotals get(String name)
	{
		return players.get(key(name));
	}

	/** Saves the lookup and moves the player's gain baselines along with it. */
	synchronized void put(PlayerTotals totals)
	{
		String key = key(totals.getName());
		players.put(key, totals);

		KillSnapshot snapshot = totals.snapshot();
		PlayerGains previous = gains.get(key);
		gains.put(key, previous == null ? PlayerGains.first(snapshot) : previous.record(snapshot, ZoneId.systemDefault()));

		write(PLAYERS_KEY, players);
		write(GAINS_KEY, gains);
	}

	/** Keyed like {@link #key(String)}. */
	synchronized Map<String, PlayerGains> gains()
	{
		return new HashMap<>(gains);
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

	synchronized void removeFromHistory(String name)
	{
		history.removeIf(name::equalsIgnoreCase);
		write(HISTORY_KEY, history);
	}

	synchronized void clearHistory()
	{
		history.clear();
		write(HISTORY_KEY, history);
	}

	synchronized void setFriends(List<String> names)
	{
		friends = new ArrayList<>(names);
		write(FRIENDS_KEY, friends);
	}

	synchronized boolean isFriend(String name)
	{
		return friends.stream().anyMatch(name::equalsIgnoreCase);
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

	static String key(String name)
	{
		return name.toLowerCase(Locale.ROOT);
	}
}
