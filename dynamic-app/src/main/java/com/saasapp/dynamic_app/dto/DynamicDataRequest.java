package com.saasapp.dynamic_app.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DynamicDataRequest {
    private JsonNode data;

    // Constructor to create from JsonNode
    public DynamicDataRequest(JsonNode data) {
        this.data = data;
    }
}
