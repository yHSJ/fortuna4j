package me.jshy.fortuna4j;

public class OutRef {
    private final String TX_HASH;
    private final int INDEX;

    public OutRef(String txHash, int index) {
        this.TX_HASH = txHash;
        this.INDEX = index;
    }

    public String getTxHash() {
        return this.TX_HASH;
    }

    public int getIndex() {
        return this.INDEX;
    }
}
