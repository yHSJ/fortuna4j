package me.jshy.fortuna4j;

import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ListPlutusData;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;

public class TargetState {
    private final BigInteger BLOCK_NUMBER;
    private final byte[] CURRENT_HASH;
    private final BigInteger LEADING_ZEROES;
    private final BigInteger DIFFICULTY_NUMBER;
    private final BigInteger EPOCH_TIME;
    private String nonce;
    private byte[] hashDigest;

    private Difficulty difficulty;

    public TargetState(byte[] nonce, State initState) {
        this.nonce = HexFormat.of().formatHex(nonce);
        BLOCK_NUMBER = initState.getBlockNumber();
        CURRENT_HASH = initState.getCurrentHash();
        LEADING_ZEROES = initState.getLeadingZeroes();
        DIFFICULTY_NUMBER = initState.getDifficultyNumber();
        EPOCH_TIME = initState.getEpochTime();
    }

    public TargetState(byte[] nonce, TargetState initState) {
        this.nonce = HexFormat.of().formatHex(nonce);
        BLOCK_NUMBER = initState.getBlockNumber();
        CURRENT_HASH = initState.getCurrentHash();
        LEADING_ZEROES = initState.getLeadingZeroes();
        DIFFICULTY_NUMBER = initState.getDifficultyNumber();
        EPOCH_TIME = initState.getEpochTime();
    }

    public ArrayList<String> calculateInterlink(
            Difficulty previousDifficulty,
            ArrayList<String> previousInterlink
    ) {
        Difficulty halfDifficulty = previousDifficulty.halfDifficulty();

        ArrayList<String> interlink = previousInterlink;

        int currentIndex = 0;

        while (halfDifficulty.getLeadingZeroes().compareTo(getDifficulty().getLeadingZeroes()) < 0 ||
               (halfDifficulty.getLeadingZeroes().compareTo(getDifficulty().getLeadingZeroes()) == 0 &&
                halfDifficulty.getDifficultyNumber().compareTo(getDifficulty().getDifficultyNumber()) > 0)) {
            if (currentIndex < interlink.size()) {
                interlink.set(currentIndex, toHashString());
            } else {
                interlink.add(toHashString());
            }

            halfDifficulty = halfDifficulty.halfDifficulty();
            currentIndex += 1;
        }

        return interlink;
    }

    public Difficulty getDifficulty() {
        if (difficulty != null) {
            return difficulty;
        }

        if (hashDigest == null) {
            toHash();
        }

        int leadingZeros = 0;
        int difficultyNumber = 0;

        for (int i = 0; i < hashDigest.length; i++) {
            byte chr = hashDigest[i];

            if (chr != 0) {
                if ((chr & 0x0F) == chr) {
                    leadingZeros += 1;
                    difficultyNumber += chr * 4096;
                    difficultyNumber += hashDigest[i + 1] * 16;
                    difficultyNumber += (hashDigest[i + 2] & 0xFF) / 16;
                } else {
                    difficultyNumber += chr * 256;
                    difficultyNumber += hashDigest[i + 1] & 0xFF;
                }

                difficulty = new Difficulty(BigInteger.valueOf(leadingZeros), BigInteger.valueOf(difficultyNumber));
                return difficulty;
            } else {
                leadingZeros += 2;
            }
        }
        difficulty = new Difficulty(BigInteger.valueOf(32), BigInteger.ZERO);
        return difficulty;
    }


    public String toHashString() {
        if (hashDigest != null) {
            return HexFormat.of().formatHex(hashDigest);
        }

        return HexFormat.of().formatHex(toHash());
    }

    public byte[] toHash() {
        if (hashDigest != null) {
            return hashDigest;
        }

        ConstrPlutusData constrPlutusData = toConstrPlutusData();
        byte[] constrPlutusDataBytes = constrPlutusData.serializeToBytes();
        MessageDigest hash;
        try {
            hash = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        this.hashDigest = hash.digest(hash.digest(constrPlutusDataBytes));
        return hashDigest;
    }

    public ConstrPlutusData toConstrPlutusData() {
        ListPlutusData plutusDataList = ListPlutusData.of(
                BytesPlutusData.of(nonce),
                BigIntPlutusData.of(BLOCK_NUMBER),
                BytesPlutusData.of(CURRENT_HASH),
                BigIntPlutusData.of(LEADING_ZEROES),
                BigIntPlutusData.of(DIFFICULTY_NUMBER),
                BigIntPlutusData.of(EPOCH_TIME)
        );

        return ConstrPlutusData.builder().data(plutusDataList).build();
    }

    public boolean isTarget() {
        if (difficulty == null) {
            getDifficulty();
        }

        return difficulty.getLeadingZeroes().compareTo(LEADING_ZEROES) > 0 || (
                difficulty.getLeadingZeroes().equals(LEADING_ZEROES) &&
                difficulty.getDifficultyNumber().compareTo(DIFFICULTY_NUMBER) > 0);
    }

    public BigInteger getBlockNumber() {
        return BLOCK_NUMBER;
    }

    public byte[] getCurrentHash() {
        return CURRENT_HASH;
    }

    public BigInteger getLeadingZeroes() {
        return LEADING_ZEROES;
    }

    public BigInteger getDifficultyNumber() {
        return DIFFICULTY_NUMBER;
    }

    public BigInteger getEpochTime() {
        return EPOCH_TIME;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = HexFormat.of().formatHex(nonce);
    }
}
