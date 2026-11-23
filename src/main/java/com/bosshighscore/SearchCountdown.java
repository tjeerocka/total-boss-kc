package com.bosshighscore;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.client.callback.ClientThread;

/**
 * A search clicked during the cooldown waits here; the latest click wins. It counts down in the panel and in one
 * chat line, then searches when the cooldown is over and removes the chat line.
 */
@Singleton
class SearchCountdown
{
	private static final long TICK_MILLIS = 200;

	private final Client client;
	private final ClientThread clientThread;
	private final ScheduledExecutorService executor;
	private final BossLookupService lookups;

	private String pendingName;
	private Consumer<String> onStatus;
	private Consumer<String> onSearch;
	private ScheduledFuture<?> ticker;
	// Only touched on the client thread
	private MessageNode chatLine;

	@Inject
	SearchCountdown(Client client, ClientThread clientThread, ScheduledExecutorService executor, BossLookupService lookups)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.executor = executor;
		this.lookups = lookups;
	}

	synchronized void searchAfterCooldown(String name, Consumer<String> onStatus, Consumer<String> onSearch)
	{
		this.pendingName = name;
		this.onStatus = onStatus;
		this.onSearch = onSearch;
		if (ticker == null)
		{
			ticker = executor.scheduleAtFixedRate(this::tick, 0, TICK_MILLIS, TimeUnit.MILLISECONDS);
		}
	}

	synchronized void stop()
	{
		if (ticker != null)
		{
			ticker.cancel(false);
			ticker = null;
		}
		pendingName = null;
		clientThread.invoke(this::removeChatLine);
	}

	private synchronized void tick()
	{
		long remaining = lookups.cooldownRemainingMillis();
		if (remaining > 0)
		{
			String text = "Looking up " + pendingName + " in " + (remaining + 999) / 1000 + "s...";
			onStatus.accept(text);
			clientThread.invoke(() -> showChatLine(text));
			return;
		}

		String name = pendingName;
		Consumer<String> search = onSearch;
		stop();
		search.accept(name);
	}

	private void showChatLine(String text)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (chatLine == null)
		{
			chatLine = client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Total Boss KC: " + text, null);
		}
		else
		{
			chatLine.setValue("Total Boss KC: " + text);
		}
		client.refreshChat();
	}

	private void removeChatLine()
	{
		if (chatLine == null)
		{
			return;
		}
		ChatLineBuffer buffer = client.getChatLineMap().get(ChatMessageType.GAMEMESSAGE.getType());
		if (buffer != null)
		{
			buffer.removeMessageNode(chatLine);
		}
		chatLine = null;
		client.refreshChat();
	}
}
