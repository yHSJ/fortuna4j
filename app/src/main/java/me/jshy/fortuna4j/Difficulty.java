package me.jshy.fortuna4j;

import java.math.BigInteger;

public class Difficulty {
    private final BigInteger LEADING_ZEROS;
    private final BigInteger DIFFICULTY_NUMBER;

    public Difficulty(BigInteger leadingZeros, BigInteger difficultyNumber) {
        this.LEADING_ZEROS = leadingZeros;
        this.DIFFICULTY_NUMBER = difficultyNumber;
    }

    public Difficulty adjustDifficulty(BigInteger numerator, BigInteger denominator) {
        BigInteger newPaddedDifficulty = DIFFICULTY_NUMBER
                .multiply(BigInteger.valueOf(16))
                .multiply(numerator)
                .divide(denominator);

        BigInteger newDifficulty = newPaddedDifficulty.divide(BigInteger.valueOf(16));

        if (newPaddedDifficulty.divide(BigInteger.valueOf(65536)).equals(BigInteger.ZERO)) {
            if (LEADING_ZEROS.compareTo(BigInteger.valueOf(62)) >= 0) {
                return new Difficulty(BigInteger.valueOf(62), BigInteger.valueOf(4096));
            } else {
                return new Difficulty(LEADING_ZEROS.add(BigInteger.ONE), newPaddedDifficulty);
            }
        } else if (newDifficulty.divide(BigInteger.valueOf(65536)).compareTo(BigInteger.ZERO) > 0) {
            if (LEADING_ZEROS.compareTo(BigInteger.valueOf(2)) <= 0) {
                return new Difficulty(BigInteger.valueOf(2), BigInteger.valueOf(65535));
            } else {
                return new Difficulty(
                        LEADING_ZEROS.subtract(BigInteger.ONE), newDifficulty.divide(BigInteger.valueOf(16)));
            }
        } else {
            return new Difficulty(LEADING_ZEROS, newDifficulty);
        }
    }

    public BigInteger getLeadingZeros() {
        return LEADING_ZEROS;
    }

    public BigInteger getDifficultyNumber() {
        return DIFFICULTY_NUMBER;
    }

    public Difficulty halfDifficulty() {
        BigInteger newDifficultyNumber = DIFFICULTY_NUMBER.divide(BigInteger.TWO);
        if (newDifficultyNumber.compareTo(BigInteger.valueOf(4096)) < 0) {
            return new Difficulty(
                    LEADING_ZEROS.add(BigInteger.ONE),
                    newDifficultyNumber.multiply(BigInteger.valueOf(16))
            );
        } else {
            return new Difficulty(LEADING_ZEROS, newDifficultyNumber);
        }
    }
}
