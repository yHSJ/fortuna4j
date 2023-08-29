package me.jshy.fortuna4j;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.jshy.fortuna4j.deserialize.GenesisDeserializer;

@JsonDeserialize(using = GenesisDeserializer.class)
public class Genesis {

    private final String VALIDATOR;
    private final String VALIDATOR_HASH;
    private final String VALIDATOR_ADDRESS;
    private final String BOOTSTRAP_HASH;
    private final OutRef OUT_REF;

    public Genesis(
            String validator,
            String validatorHash,
            String validatorAddress,
            String bootstrapHash,
            OutRef outRef
    ) {
        this.VALIDATOR = validator;
        this.VALIDATOR_HASH = validatorHash;
        this.VALIDATOR_ADDRESS = validatorAddress;
        this.BOOTSTRAP_HASH = bootstrapHash;
        this.OUT_REF = outRef;
    }

    public String getValidator() {
        return this.VALIDATOR;
    }

    public String getValidatorHash() {
        return this.VALIDATOR_HASH;
    }

    public String getValidatorAddress() {
        return this.VALIDATOR_ADDRESS;
    }

    public String getBootstrapHash() {
        return this.BOOTSTRAP_HASH;
    }

    public OutRef getOutRef() {
        return this.OUT_REF;
    }
}
