package com.bosshighscore;

/**
 * Tells when the friends list has finished loading after login: the game sends it bit by bit, so it counts as loaded
 * once its size stops changing for a few ticks. Gives up waiting after {@link #MAX_TICKS} (e.g. no friends at all).
 */
class FriendListLoad
{
	static final int STABLE_TICKS = 3;
	static final int MAX_TICKS = 30;

	private int ticks;
	private int lastCount = -1;
	private int stableTicks;

	/**
	 * @param count friends loaded so far, or -1 while the list isn't there yet
	 * @return true once the list counts as loaded
	 */
	boolean tick(int count)
	{
		ticks++;
		stableTicks = count >= 0 && count == lastCount ? stableTicks + 1 : 0;
		lastCount = count;
		return stableTicks >= STABLE_TICKS || ticks >= MAX_TICKS;
	}
}
