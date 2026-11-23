package com.bosshighscore;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A search clicked during the cooldown waits here; the latest click wins. It counts down in the panel's status line,
 * then searches when the cooldown is over.
 */
@Singleton
class SearchCountdown
{
	private static final long TICK_MILLIS = 200;

	private final ScheduledExecutorService executor;
	private final BossLookupService lookups;

	private String pendingName;
	private Consumer<String> onStatus;
	private Consumer<String> onSearch;
	private ScheduledFuture<?> ticker;

	@Inject
	SearchCountdown(ScheduledExecutorService executor, BossLookupService lookups)
	{
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
	}

	private synchronized void tick()
	{
		long remaining = lookups.cooldownRemainingMillis();
		if (remaining > 0)
		{
			onStatus.accept("Looking up " + pendingName + " in " + (remaining + 999) / 1000 + "s...");
			return;
		}

		String name = pendingName;
		Consumer<String> search = onSearch;
		stop();
		search.accept(name);
	}
}
