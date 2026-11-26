package com.bosshighscore;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/** Looked-up totals, search history and the last seen friends list, saved in the RuneLite config. */
@Slf4j
@Singleton
class PlayerStore
{
	private static final String PLAYERS_KEY = "players";
	private static final String HISTORY_KEY = "history";
	private static final String FRIENDS_KEY = "friends";
	private static final String GAINS_KEY = "gains";
	static final Set<String> KEYS = ImmutableSet.of(PLAYERS_KEY, HISTORY_KEY, FRIENDS_KEY, GAINS_KEY);
	private static final int HISTORY_LIMIT = 50;
	private static final Duration KEEP_UNUSED = Duration.ofDays(60);
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
		pruneUnused();
	}

	synchronized PlayerTotals get(String name)
	{
		return players.get(key(name));
	}

	/** Saves the lookup and moves the player's gain baselines along with it. */
	synchronized void put(PlayerTotals totals)
	{
		put(totals, false);
	}

	/** For the midnight lookup: it becomes the starting point of the day (and week, month, year) that just began. */
	synchronized void putAtMidnight(PlayerTotals totals)
	{
		put(totals, true);
	}

	private void put(PlayerTotals totals, boolean atMidnight)
	{
		String key = key(totals.getName());
		players.put(key, totals);

		KillSnapshot snapshot = totals.snapshot();
		PlayerGains previous = gains.get(key);
		ZoneId zone = ZoneId.systemDefault();
		if (previous == null)
		{
			gains.put(key, PlayerGains.first(snapshot));
		}
		else
		{
			gains.put(key, atMidnight ? previous.recordPeriodStart(snapshot, zone) : previous.record(snapshot, zone));
		}

		write(PLAYERS_KEY, players);
		write(GAINS_KEY, gains);
	}

	/** Keyed like {@link #key(String)}. */
	/**
	 * True when the player's last lookup was saved before gains per boss existed. Looking them up once more gives
	 * their gains a per-boss starting point straight away instead of waiting out the refresh time.
	 */
	synchronized boolean lacksBossDetail(String name)
	{
		PlayerGains playerGains = gains.get(key(name));
		return playerGains != null && !playerGains.getLatest().hasBossDetail();
	}

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

	/**
	 * Drops saved kills and gains of players that are neither a friend nor in the history and haven't been looked up
	 * for {@link #KEEP_UNUSED}, so the save can't grow forever. Not by friends list alone: right after login that list
	 * can still be half loaded, and dropping those friends would reset their gains.
	 */
	private void pruneUnused()
	{
		Set<String> named = new HashSet<>();
		friends.forEach(name -> named.add(key(name)));
		history.forEach(name -> named.add(key(name)));
		long cutoff = System.currentTimeMillis() - KEEP_UNUSED.toMillis();

		boolean playersChanged = players.entrySet()
			.removeIf(entry -> !named.contains(entry.getKey()) && entry.getValue().getUpdatedMillis() < cutoff);
		boolean gainsChanged = gains.keySet().retainAll(players.keySet());
		if (playersChanged)
		{
			write(PLAYERS_KEY, players);
		}
		if (gainsChanged)
		{
			write(GAINS_KEY, gains);
		}
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
		try
		{
			T value = gson.fromJson(json, type);
			return value == null ? fallback : value;
		}
		catch (RuntimeException e)
		{
			// A damaged save must not stop the plugin from starting: start that part empty instead
			log.warn("Could not read saved {}, starting without it", configKey, e);
			return fallback;
		}
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
