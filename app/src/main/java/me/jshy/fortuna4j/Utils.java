package me.jshy.fortuna4j;

import com.bloxbean.cardano.client.util.Tuple;

import java.math.BigInteger;

public class Utils {

    public static long convertSecondsToSlots(long seconds, boolean testnet) {
        long timeWhenSlotChanged = testnet ? 1595967616 : 1595982016;
        long slotWhenSlotChanged = 1598400;

        return slotWhenSlotChanged + (seconds - timeWhenSlotChanged);
    }

    public static String toHex(String str) {
        char[] ch = str.toCharArray();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ch.length; i++) {
            String hexString = Integer.toHexString(ch[i]);
            sb.append(hexString);

        }

        return sb.toString();
    }

    public static Tuple<BigInteger, BigInteger> getDifficultyAdjustment(
            BigInteger totalEpochTime, BigInteger epochTarget
    ) {
        if (epochTarget.divide(totalEpochTime).compareTo(BigInteger.valueOf(4)) >= 0 &&
            epochTarget.mod(totalEpochTime).compareTo(BigInteger.ZERO) > 0) {
            return new Tuple<>(BigInteger.ONE, BigInteger.valueOf(4));
        } else if (totalEpochTime.divide(epochTarget).compareTo(BigInteger.valueOf(4)) >= 0 &&
                   totalEpochTime.mod(epochTarget).compareTo(BigInteger.ZERO) > 0) {
            return new Tuple<>(BigInteger.valueOf(4), BigInteger.ONE);
        } else {
            return new Tuple<>(totalEpochTime, epochTarget);
        }
    }
}
