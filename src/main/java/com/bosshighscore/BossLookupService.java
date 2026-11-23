package com.bosshighscore;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;

/**
 * Hiscore lookups off the client thread, at most one search per 2 seconds and one batch lookup per second and never the same player twice at once,
 * so spam clicking can't flood the hiscores or block RuneLite's shared executor. Searches get their own queue so they
 * don't wait behind a friends refresh.
 */
@Slf4j
@Singleton
class BossLookupService
{
	private static final long SEARCH_SPACING_MILLIS = 2000;
	private static final long BATCH_SPACING_MILLIS = 1000;

	private final HiscoreClient hiscoreClient;
	private final ScheduledExecutorService executor;
	private final AtomicLong nextSearchMillis = new AtomicLong();
	private final AtomicLong nextBatchMillis = new AtomicLong();
	private final Set<String> queued = ConcurrentHashMap.newKeySet();

	@Inject
	BossLookupService(HiscoreClient hiscoreClient, ScheduledExecutorService executor)
	{
		this.hiscoreClient = hiscoreClient;
		this.executor = executor;
	}

	/** For searches and Boss lookup. */
	void lookupSearch(String name, Consumer<PlayerTotals> onFound, Consumer<String> onMissing)
	{
		schedule(name, nextSearchMillis, SEARCH_SPACING_MILLIS, onFound, onMissing);
	}

	/** For batches like the friends list. */
	void lookupBatch(String name, Consumer<PlayerTotals> onFound, Consumer<String> onMissing)
	{
		schedule(name, nextBatchMillis, BATCH_SPACING_MILLIS, onFound, onMissing);
	}

	private void schedule(String name, AtomicLong nextSlotMillis, long spacingMillis, Consumer<PlayerTotals> onFound,
		Consumer<String> onMissing)
	{
		String key = name.toLowerCase(Locale.ROOT);
		if (!queued.add(key))
		{
			return;
		}

		long now = System.currentTimeMillis();
		long slot = nextSlotMillis.updateAndGet(previous -> Math.max(previous + spacingMillis, now));
		executor.schedule(() ->
		{
			queued.remove(key);
			lookup(name, onFound, onMissing);
		}, slot - now, TimeUnit.MILLISECONDS);
	}

	private void lookup(String name, Consumer<PlayerTotals> onFound, Consumer<String> onMissing)
	{
		try
		{
			HiscoreResult result = hiscoreClient.lookup(name);
			if (result == null)
			{
				onMissing.accept(name);
				return;
			}
			onFound.accept(BossTotals.from(name, result, System.currentTimeMillis()));
		}
		catch (IOException e)
		{
			log.debug("Hiscore lookup failed for {}", name, e);
			onMissing.accept(name);
		}
	}
}
