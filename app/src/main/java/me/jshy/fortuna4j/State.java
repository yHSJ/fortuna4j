package me.jshy.fortuna4j;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.plutus.spec.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

public class State {
    private final BigInteger BLOCK_NUMBER;
    private final byte[] CURRENT_HASH;
    private final BigInteger LEADING_ZEROES;
    private final BigInteger DIFFICULTY_NUMBER;
    private final BigInteger EPOCH_TIME;
    private final BigInteger CURRENT_POSIX_TIME;
    private final PlutusData EXTRA_DATA;
    private final ListPlutusData INTERLINK;

    public State(String cbor) throws RuntimeException {
        try {
            byte[] cborBytes = HexFormat.of().parseHex(cbor);
            List<DataItem> dataItems = CborDecoder.decode(cborBytes);
            ConstrPlutusData constrPlutusData = ConstrPlutusData.deserialize(dataItems.get(0));

            BLOCK_NUMBER = getBlockNumberFromConstrData(constrPlutusData);
            CURRENT_HASH = getCurrentHashFromConstrData(constrPlutusData);
            LEADING_ZEROES = getLeadingZeroesFromConstrData(constrPlutusData);
            DIFFICULTY_NUMBER = getDifficultyNumberFromConstrData(constrPlutusData);
            EPOCH_TIME = getEpochTimeFromConstrData(constrPlutusData);
            CURRENT_POSIX_TIME = getCurrentPosixTimeFromConstrData(constrPlutusData);
            EXTRA_DATA = getExtraDataFromConstrData(constrPlutusData);
            INTERLINK = getInterlinkFromConstrData(constrPlutusData);
        } catch (CborException | CborDeserializationException e) {
            throw new RuntimeException("Failed to decode CBOR string", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to serialize PlutusData", e);
        }
    }

    public State(StateBuilder builder) {
        this.BLOCK_NUMBER = builder.blockNumber;
        this.CURRENT_HASH = builder.currentHash;
        this.LEADING_ZEROES = builder.leadingZeroes;
        this.DIFFICULTY_NUMBER = builder.difficultyNumber;
        this.EPOCH_TIME = builder.epochTime;
        this.CURRENT_POSIX_TIME = builder.currentPosixTime;
        this.EXTRA_DATA = builder.extraData;
        this.INTERLINK = builder.interlink;
    }

    public static StateBuilder builder() {
        return new StateBuilder();
    }

    public Difficulty getDifficulty() {
        return new Difficulty(LEADING_ZEROES, DIFFICULTY_NUMBER);
    }

    private BigInteger getBlockNumberFromConstrData(ConstrPlutusData plutusData) {
        BigIntPlutusData data = (BigIntPlutusData) plutusData.getData().getPlutusDataList().get(0);
        return data.getValue();
    }

    private byte[] getCurrentHashFromConstrData(ConstrPlutusData plutusData) {
        BytesPlutusData data = (BytesPlutusData) plutusData.getData().getPlutusDataList().get(1);
        return data.getValue();
    }

    private BigInteger getLeadingZeroesFromConstrData(ConstrPlutusData plutusData) {
        BigIntPlutusData data = (BigIntPlutusData) plutusData.getData().getPlutusDataList().get(2);
        return data.getValue();
    }

    private BigInteger getDifficultyNumberFromConstrData(ConstrPlutusData plutusData) {
        BigIntPlutusData data = (BigIntPlutusData) plutusData.getData().getPlutusDataList().get(3);
        return data.getValue();
    }

    private BigInteger getEpochTimeFromConstrData(ConstrPlutusData plutusData) {
        BigIntPlutusData data = (BigIntPlutusData) plutusData.getData().getPlutusDataList().get(4);
        return data.getValue();
    }

    private BigInteger getCurrentPosixTimeFromConstrData(ConstrPlutusData plutusData) {
        BigIntPlutusData data = (BigIntPlutusData) plutusData.getData().getPlutusDataList().get(5);
        return data.getValue();
    }

    private PlutusData getExtraDataFromConstrData(ConstrPlutusData plutusData) {
        return plutusData.getData().getPlutusDataList().get(6);
    }

    private ListPlutusData getInterlinkFromConstrData(ConstrPlutusData plutusData) {
        return (ListPlutusData) plutusData.getData().getPlutusDataList().get(7);
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

    public BigInteger getCurrentPosixTime() {
        return CURRENT_POSIX_TIME;
    }

    public PlutusData getExtraData() {
        return EXTRA_DATA;
    }

    public ArrayList<String> getInterlinkStringArray() {
        return INTERLINK.getPlutusDataList().stream().map((data) -> {
            byte[] byteString = ((BytesPlutusData) data).getValue();

            return HexFormat.of().formatHex(byteString);
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    public ListPlutusData getInterlink() {
        return INTERLINK;
    }

    public ConstrPlutusData toConstrPlutusData() {
        ListPlutusData plutusDataList = ListPlutusData.of(
                BigIntPlutusData.of(BLOCK_NUMBER),
                BytesPlutusData.of(CURRENT_HASH),
                BigIntPlutusData.of(LEADING_ZEROES),
                BigIntPlutusData.of(DIFFICULTY_NUMBER),
                BigIntPlutusData.of(EPOCH_TIME),
                BigIntPlutusData.of(CURRENT_POSIX_TIME),
                EXTRA_DATA,
                INTERLINK
        );

        return ConstrPlutusData.builder().data(plutusDataList).build();
    }

    public static class StateBuilder {
        private BigInteger blockNumber;
        private byte[] currentHash;
        private BigInteger leadingZeroes;
        private BigInteger difficultyNumber;
        private BigInteger epochTime;
        private BigInteger currentPosixTime;
        private PlutusData extraData;
        private ListPlutusData interlink;

        public StateBuilder setBlockNumber(BigInteger blockNumber) {
            this.blockNumber = blockNumber;
            return this;
        }

        public StateBuilder setCurrentHash(byte[] currentHash) {
            this.currentHash = currentHash;
            return this;
        }

        public StateBuilder setLeadingZeroes(BigInteger leadingZeroes) {
            this.leadingZeroes = leadingZeroes;
            return this;
        }

        public StateBuilder setDifficultyNumber(BigInteger difficultyNumber) {
            this.difficultyNumber = difficultyNumber;
            return this;
        }

        public StateBuilder setEpochTime(BigInteger epochTime) {
            this.epochTime = epochTime;
            return this;
        }

        public StateBuilder setCurrentPosixTime(BigInteger currentPosixTime) {
            this.currentPosixTime = currentPosixTime;
            return this;
        }

        public StateBuilder setExtraData(PlutusData extraData) {
            this.extraData = extraData;
            return this;
        }

        public StateBuilder setInterlink(ArrayList<String> interlink) {
            List<PlutusData> plutusDataList =
                    interlink.stream().map((link) -> (PlutusData) BytesPlutusData.of(HexFormat.of().parseHex(link)))
                             .toList();

            this.interlink = ListPlutusData.builder().plutusDataList(plutusDataList).build();
            return this;
        }

        public StateBuilder setInterlink(ListPlutusData interlink) {
            this.interlink = interlink;
            return this;
        }

        public State build() {
            return new State(this);
        }
    }
}