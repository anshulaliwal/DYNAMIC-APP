package com.saasapp.dynamic_app.controller;

import com.saasapp.dynamic_app.entity.DynamicData;
import com.saasapp.dynamic_app.service.DynamicDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dynamic")
@CrossOrigin(origins = {"https://nyayapathlegal.in", "https://client-ca-saas-app.vercel.app", "https://super-saas-app.vercel.app", "http://localhost:3000", "http://localhost:3001"},
             allowedHeaders = "*",
             methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
             allowCredentials = "true",
             maxAge = 3600)
public class DynamicDataController {
    private static final Logger logger = LoggerFactory.getLogger(DynamicDataController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DynamicDataService service;

    @PostMapping("/update/{userId}/{key}")
    public ResponseEntity<String> saveOrUpdateDynamicData(
            @PathVariable String userId,
            @PathVariable String key,
            @RequestBody String data
    ) {
        logger.debug("Received save/update request - userId: {}, key: {}, data: {}", userId, key, data);

        if (data == null || data.trim().isEmpty()) {
            logger.warn("Data is null or empty in request");
            return ResponseEntity.badRequest().body("Data cannot be null or empty");
        }

        try {
            // Validate that the string is valid JSON
            objectMapper.readTree(data);
            logger.debug("Valid JSON received");

            // Check if record exists
            Optional<DynamicData> existing = service.getDynamicData(userId, key);

            if (existing.isPresent()) {
                logger.debug("Record exists, updating");
                service.updateDynamicData(userId, key, data, Instant.now(), "system");
                return ResponseEntity.ok("updated");
            } else {
                logger.debug("Record doesn't exist, creating");
                service.saveDynamicData(userId, key, data, Instant.now(), "system");
                return ResponseEntity.status(HttpStatus.CREATED).body("created");
            }
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            logger.error("JSON parse error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("JSON parse error: " + e.getOriginalMessage());
        } catch (RuntimeException e) {
            logger.error("Failed to save/update data - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save/update: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/test")
    public ResponseEntity<String> testPost(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return ResponseEntity.ok("POST received with empty body");
        }
        return ResponseEntity.ok("POST received");
    }


    @GetMapping("/fetch/{userId}/{key}")
    public ResponseEntity<?> getDynamicData(
            @PathVariable String userId,
            @PathVariable String key
    ) {
            logger.debug("Received get request - userId: {}, key: {}", userId, key);
        Optional<DynamicData> data = service.getDynamicData(userId, key);
        if (data.isPresent()) {
            try {
                logger.debug("Data found for userId: {}, key: {}", userId, key);
                // Return the raw JSON string as response
                return ResponseEntity.ok(data.get().getData());
            } catch (Exception e) {
                logger.error("Error retrieving data: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving data");
            }
        }
        logger.warn("No data found for userId: {}, key: {}", userId, key);
        return ResponseEntity.notFound().build();
    }
}
