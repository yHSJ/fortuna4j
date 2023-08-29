package me.jshy.fortuna4j;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class App {

    public App() {
        boolean testnet = Boolean.parseBoolean(EnvManager.get("testnet"));
        Genesis genesis = null;

        try {
            genesis = deserializeGenesis(testnet);
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.err.println("Could not find genesis file for " + (testnet ? "testnet" : "mainnet") + ".");
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("Could not deserialize genesis file.");
            System.exit(1);
        }

        String mnemonic = EnvManager.get("seed_phrase");
        if (mnemonic == null) {
            System.err.println("Missing seed phrase. Please set the SEED_PHRASE environment variable.");
            System.exit(1);
        }

        String kupoUrl = EnvManager.get("kupo_url");
        if (kupoUrl == null) {
            System.err.println("Missing Kupo URL. Please set the KUPO_URL environment variable.");
            System.exit(1);
        }

        String ogmiosUrl = EnvManager.get("ogmios_url");
        if (ogmiosUrl == null) {
            System.err.println("Missing Ogmios URL. Please set the OGMIOS_URL environment variable.");
            System.exit(1);
        }


        Miner miner = new Miner.MinerBuilder()
                .setNetwork(testnet)
                .setGenesis(genesis)
                .setMnemonic(mnemonic)
                .setKupoUrl(kupoUrl)
                .setOgmiosUrl(ogmiosUrl)
                .build();

        double balance = miner.getBalance();
        System.out.println("Balance: " + balance + " ADA");

        System.out.println("Starting miner...");
        miner.start();
    }

    public static void main(String[] args) {
        new App();
    }

    private Genesis deserializeGenesis(boolean testnet) throws RuntimeException {
        String genesisFileName = testnet ? "preview.json" : "mainnet.json";
        File file = new File("genesis/" + genesisFileName);

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(file, Genesis.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
