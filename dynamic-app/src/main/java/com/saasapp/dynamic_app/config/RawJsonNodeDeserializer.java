package com.saasapp.dynamic_app.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.IOException;

public class RawJsonNodeDeserializer extends JsonDeserializer<JsonNode> {
    @Override
    public JsonNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        // Return the node as-is, even if it's null or NullNode
        return node != null ? node : NullNode.getInstance();
    }
}

