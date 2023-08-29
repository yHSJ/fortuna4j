package me.jshy.fortuna4j.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import me.jshy.fortuna4j.Genesis;
import me.jshy.fortuna4j.OutRef;

import java.io.IOException;

public class GenesisDeserializer extends StdDeserializer<Genesis> {

    public GenesisDeserializer() {
        this(null);
    }

    protected GenesisDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Genesis deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String validator = node.get("validator").asText();
        String validatorHash = node.get("validatorHash").asText();
        String validatorAddress = node.get("validatorAddress").asText();
        String bootstrapHash = node.get("bootstrapHash").asText();
        OutRef outRef = deserializeOutRef(node.get("outRef"));

        return new Genesis(validator, validatorHash, validatorAddress, bootstrapHash, outRef);

    }

    private OutRef deserializeOutRef(JsonNode node) {
        String txHash = node.get("txHash").asText();
        int index = node.get("index").asInt();
        return new OutRef(txHash, index);
    }
}
