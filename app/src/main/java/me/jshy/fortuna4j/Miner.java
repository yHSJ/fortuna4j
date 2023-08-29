package me.jshy.fortuna4j;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.ogmios.KupmiosBackendService;
import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.util.Tuple;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static me.jshy.fortuna4j.Utils.toHex;

public class Miner {
    private final Network NETWORK;
    private final Genesis GENESIS;
    private final Account ACCOUNT;
    private final Address ADDRESS;
    private final BackendService KUPMIOS;

    private Utxo validatorOutRef;
    private State currentState;


    private Miner(MinerBuilder builder) {
        this.NETWORK = builder.network;
        this.GENESIS = builder.genesis;

        this.ACCOUNT = new Account(this.NETWORK, builder.mnemonic);
        this.ADDRESS = ACCOUNT.getBaseAddress();
        this.KUPMIOS = new KupmiosBackendService(builder.ogmiosUrl, builder.kupoUrl);

        this.validatorOutRef = fetchValidatorOutRef();

        this.currentState = new State(this.validatorOutRef.getInlineDatum());
        System.out.println(currentState.toConstrPlutusData().serializeToHex());
    }

    public void start() {
        SecureRandom random = new SecureRandom();
        byte[] nonce = new byte[16];
        random.nextBytes(nonce);

        TargetState targetState = new TargetState(nonce, currentState);
        long timer = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - timer > 30000) {
                System.out.println("No proof found in 30 seconds. Updating state...");
                updateState();
                timer = System.currentTimeMillis();
                continue;
            }

            if (targetState.isTarget()) {
                System.out.println("Found a proof!");
                break;
            }

            incrementNonce(nonce);
            targetState = new TargetState(nonce, targetState);
        }
        int realTimeNow = (int) Math.round(System.currentTimeMillis() / 1000.0) - 60;


        State newState = buildState(targetState, realTimeNow);
        buildAndSubmit(newState, nonce, realTimeNow);

    }

    private State buildState(TargetState targetState, int realTimeNow) {

        ArrayList<String> interlink = targetState.calculateInterlink(
                currentState.getDifficulty(),
                currentState.getInterlinkStringArray()
        );

        System.out.println("Current State Epoch Time: " + currentState.getEpochTime());
        BigInteger epochTime = currentState.getEpochTime()
                                           .add(BigInteger.valueOf(90000 + realTimeNow * 1000L))
                                           .subtract(currentState.getCurrentPosixTime());

        System.out.println("New Epoch Time: " + epochTime);
        Difficulty difficulty = targetState.getDifficulty();

        if (currentState.getBlockNumber().mod(BigInteger.valueOf(2016)).equals(BigInteger.ZERO) &&
            currentState.getBlockNumber().compareTo(BigInteger.ZERO) > 0) {
            difficulty = adjustDifficulty(epochTime);
        }

        return State.builder()
                    .setBlockNumber(currentState.getBlockNumber().add(BigInteger.ONE))
                    .setCurrentHash(targetState.toHash())
                    .setLeadingZeroes(difficulty.getLeadingZeroes())
                    .setDifficultyNumber(difficulty.getDifficultyNumber())
                    .setEpochTime(epochTime)
                    .setCurrentPosixTime(BigInteger.valueOf(90000 + realTimeNow))
                    .setExtraData(BytesPlutusData.of("JSHy was here"))
                    .setInterlink(interlink)
                    .build();
    }

    private void buildAndSubmit(State state, byte[] nonce, int realTimeNow) {
        int validityStart = realTimeNow - 1596491091 + 4924800;

        System.out.println(validityStart);
        List<Utxo> utxos = getUtxos(ADDRESS.getAddress());
        utxos.add(validatorOutRef);
        PlutusV2Script script = PlutusV2Script.builder().cborHex(GENESIS.getValidator()).build();
        Asset mintAsset = new Asset("TUNA", BigInteger.valueOf(5_000_000_000L));

        System.out.println(state.toConstrPlutusData().serializeToHex());
        System.out.println(BytesPlutusData.of(nonce).serializeToHex());
        System.out.println(PlutusData.unit().serializeToHex());

        List<PlutusData> plutusDataList = List.of(BytesPlutusData.of(nonce));
        ListPlutusData plutusData = ListPlutusData.builder().plutusDataList(plutusDataList).build();
        ConstrPlutusData redeemer = ConstrPlutusData.builder().data(plutusData).build();

        ScriptTx stateTx = new ScriptTx()
                .collectFrom(utxos, redeemer)
                .readFrom(validatorOutRef)
                .payToContract(
                        GENESIS.getValidatorAddress(),
                        List.of(Amount.asset(GENESIS.getValidatorHash() + toHex("lord tuna"), 1)),
                        state.toConstrPlutusData()
                ).mintAsset(script, mintAsset, PlutusData.unit(), ACCOUNT.baseAddress())
                .attachSpendingValidator(script)
                .withChangeAddress(ACCOUNT.baseAddress());

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(KUPMIOS);
        Result<String> tx = quickTxBuilder
                .compose(stateTx)
                .withSigner(SignerProviders.signerFrom(ACCOUNT))
                .feePayer(ACCOUNT.baseAddress())
                .validFrom(validityStart)
                .validTo(validityStart + 180)
                .postBalanceTx((context, txn) -> {
                    txn.getWitnessSet().getPlutusV2Scripts().clear();
                })
                .completeAndWait(System.out::println);
    }

    private Difficulty adjustDifficulty(BigInteger epochTime) {
        Tuple<BigInteger, BigInteger> adjustment =
                Utils.getDifficultyAdjustment(epochTime, BigInteger.valueOf(1_209_600_000));

        return currentState.getDifficulty().adjustDifficulty(adjustment._1, adjustment._2);
    }

    private void incrementNonce(byte[] nonce) {
        for (int i = 0; i < nonce.length; i++) {
            if (nonce[i] == (byte) 255) {
                nonce[i] = 0;
            } else {
                nonce[i] += 1;
                break;
            }
        }
    }


    private void updateState() {
        this.validatorOutRef = fetchValidatorOutRef();
        this.currentState = new State(this.validatorOutRef.getInlineDatum());
    }

    public double getBalance() throws RuntimeException {
        List<Utxo> utxos = getUtxos(ADDRESS.getAddress());

        return utxos.stream().map(this::getLovelaceAmount)
                    .mapToDouble(lovelace -> lovelace.map(this::getAdaValue).orElse(0.0)).sum();
    }

    private Utxo fetchValidatorOutRef() {
        List<Utxo> utxos = getUtxos(GENESIS.getValidatorAddress());
        Optional<Utxo> validatorUtxo =
                utxos.stream().filter((utxo) -> getValidatorAmount(utxo).isPresent()).findFirst();

        if (validatorUtxo.isEmpty()) {
            throw new RuntimeException("Could not find validator output reference");
        }

        return validatorUtxo.get();
    }

    private Optional<Amount> getValidatorAmount(Utxo utxo) {
        return utxo.getAmount().stream()
                   .filter((amount) -> amount.getUnit().equals(GENESIS.getValidatorHash() + toHex("lord tuna")))
                   .findFirst();
    }

    private Optional<Amount> getLovelaceAmount(Utxo utxo) {
        return utxo.getAmount().stream().filter((amount) -> amount.getUnit().equals(LOVELACE)).findFirst();
    }

    private double getAdaValue(Amount lovelaceAmount) {
        return lovelaceAmount.getQuantity().doubleValue() / 1_000_000;
    }

    private List<Utxo> getUtxos(String address) {
        Result<List<Utxo>> request;
        try {
            request = KUPMIOS.getUtxoService().getUtxos(address, 100, 1);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

        if (!request.isSuccessful()) {
            throw new RuntimeException("request failed with code: " + request.code());
        }

        return request.getValue();
    }

    public static class MinerBuilder {
        private Network network;
        private Genesis genesis;
        private String mnemonic;
        private String kupoUrl;
        private String ogmiosUrl;

        public Miner build() {
            return new Miner(this);
        }

        public MinerBuilder setNetwork(boolean testnet) {
            this.network = testnet ? Networks.preview() : Networks.mainnet();

            return this;
        }

        public MinerBuilder setGenesis(Genesis genesis) {
            this.genesis = genesis;

            return this;
        }

        public MinerBuilder setMnemonic(String mnemonic) {
            this.mnemonic = mnemonic;

            return this;
        }

        public MinerBuilder setKupoUrl(String kupoUrl) {
            this.kupoUrl = kupoUrl;

            return this;
        }

        public MinerBuilder setOgmiosUrl(String ogmiosUrl) {
            this.ogmiosUrl = ogmiosUrl;

            return this;
        }
    }

}
