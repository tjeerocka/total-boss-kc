package com.bosshighscore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FriendListLoadTest
{
	@Test
	public void waitsWhileTheListIsStillGrowing()
	{
		FriendListLoad load = new FriendListLoad();

		assertFalse(load.tick(-1));
		assertFalse(load.tick(4));
		assertFalse(load.tick(12));
		assertFalse(load.tick(16));
	}

	@Test
	public void loadedOnceTheSizeStopsChanging()
	{
		FriendListLoad load = new FriendListLoad();
		load.tick(16);

		assertFalse(load.tick(16));
		assertFalse(load.tick(16));
		assertTrue(load.tick(16));
	}

	@Test
	public void givesUpWaitingWhenTheListNeverShows()
	{
		FriendListLoad load = new FriendListLoad();
		for (int i = 1; i < FriendListLoad.MAX_TICKS; i++)
		{
			assertFalse(load.tick(-1));
		}

		assertTrue(load.tick(-1));
	}
}
