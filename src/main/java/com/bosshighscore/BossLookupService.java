package com.bosshighscore;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;

/** Hiscore lookups off the client thread. Batch lookups are spaced out so the hiscores don't throttle us. */
@Slf4j
@Singleton
class BossLookupService
{
	private static final long SPACING_MILLIS = 1000;

	private final HiscoreClient hiscoreClient;
	private final ScheduledExecutorService executor;
	private final AtomicLong nextSlotMillis = new AtomicLong();

	@Inject
	BossLookupService(HiscoreClient hiscoreClient, ScheduledExecutorService executor)
	{
		this.hiscoreClient = hiscoreClient;
		this.executor = executor;
	}

	/** For a search: runs right away. */
	void lookupNow(String name, Consumer<PlayerTotals> onFound, Consumer<String> onMissing)
	{
		executor.execute(() -> lookup(name, onFound, onMissing));
	}

	/** For batches like the friends list: one lookup per {@link #SPACING_MILLIS}. */
	void lookupSpaced(String name, Consumer<PlayerTotals> onFound, Consumer<String> onMissing)
	{
		long now = System.currentTimeMillis();
		long slot = nextSlotMillis.updateAndGet(previous -> Math.max(previous + SPACING_MILLIS, now));
		executor.schedule(() -> lookup(name, onFound, onMissing), slot - now, TimeUnit.MILLISECONDS);
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
