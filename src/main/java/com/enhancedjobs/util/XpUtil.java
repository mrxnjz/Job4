package com.enhancedjobs.util;

public final class XpUtil {

    public static final int MAX_LEVEL = 10;
    public static final int XP_PER_QUEST = 200;

    private XpUtil() {}

    /**
     * XP required to advance FROM a given level to the next.
     * Level 1 → 2 requires 200, Level 2 → 3 requires 400, etc.
     */
    public static int getXpRequired(int level) {
        if (level < 1 || level >= MAX_LEVEL) return 0;
        return level * 200;
    }

    /**
     * Total cumulative XP required to REACH a level from level 1 at 0 XP.
     */
    public static int getTotalXpToReachLevel(int targetLevel) {
        int total = 0;
        for (int i = 1; i < targetLevel; i++) {
            total += getXpRequired(i);
        }
        return total;
    }

    /**
     * Compute the level a player is at given their total accumulated XP.
     */
    public static int getLevelFromTotalXp(int totalXp) {
        int level = 1;
        while (level < MAX_LEVEL) {
            int required = getXpRequired(level);
            if (required == 0 || totalXp < required) break;
            totalXp -= required;
            level++;
        }
        return level;
    }

    /**
     * Get XP progress within the current level.
     */
    public static int getXpInCurrentLevel(int totalXp) {
        int level = 1;
        while (level < MAX_LEVEL) {
            int required = getXpRequired(level);
            if (required == 0 || totalXp < required) break;
            totalXp -= required;
            level++;
        }
        return totalXp;
    }

    /**
     * Calculate quest cost for a given purchase number that day.
     * questsBoughtToday=0 → free, =1 → 30, =2 → 60, =3 → 120 ...
     */
    public static double getQuestCost(int questsBoughtToday, double baseCost, double multiplier) {
        if (questsBoughtToday == 0) return 0;
        return baseCost * Math.pow(multiplier, questsBoughtToday - 1);
    }

    /** Even levels (2, 4, 6, 8, 10) grant special rewards. */
    public static boolean isRewardLevel(int level) {
        return level > 1 && level % 2 == 0;
    }

    /** Build a nice progress bar string. */
    public static String buildProgressBar(int current, int max, int length,
                                          String filledChar, String emptyChar,
                                          String filledColor, String emptyColor) {
        if (max <= 0) return emptyColor + emptyChar.repeat(length);
        int filled = (int) Math.round((double) current / max * length);
        filled = Math.max(0, Math.min(filled, length));
        return filledColor + filledChar.repeat(filled)
                + emptyColor + emptyChar.repeat(length - filled);
    }
}
