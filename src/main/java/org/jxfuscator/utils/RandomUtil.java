package org.jxfuscator.utils;

import java.util.Random;

public class RandomUtil {
    static Random random = new Random();

    public static int randomInt(int lowerbound, int upperbound) {
        return random.nextInt(lowerbound, upperbound);
    }
}
