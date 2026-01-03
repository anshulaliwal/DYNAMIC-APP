package com.saasapp.dynamic_app.service;

import com.saasapp.dynamic_app.entity.DynamicData;
import com.saasapp.dynamic_app.repository.DynamicDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

@Service
public class DynamicDataService {

    @Autowired
    private DynamicDataRepository repository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void saveDynamicData(String userId, String key, Object data, java.time.Instant updatedTime, String updatedBy) {
        try {
            // Convert object to JSON string
            String jsonString = data instanceof String ? (String) data : objectMapper.writeValueAsString(data);
            DynamicData dynamicData = new DynamicData(userId, key, jsonString, updatedTime, updatedBy);
            repository.save(dynamicData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save dynamic data", e);
        }
    }

    // Overloaded for backward compatibility
    public void saveDynamicData(String userId, String key, Object data) {
        saveDynamicData(userId, key, data, java.time.Instant.now(), "system");
    }

    public Optional<DynamicData> getDynamicData(String userId, String key) {
        return repository.findByUserIdAndKey(userId, key);
    }

    public void updateDynamicData(String userId, String key, Object data, java.time.Instant updatedTime, String updatedBy) {
        try {
            Optional<DynamicData> existing = repository.findByUserIdAndKey(userId, key);
            if (existing.isPresent()) {
                DynamicData dynamicData = existing.get();
                // Convert object to JSON string
                String jsonString = data instanceof String ? (String) data : objectMapper.writeValueAsString(data);
                dynamicData.setData(jsonString);
                dynamicData.setUpdatedTime(updatedTime);
                dynamicData.setUpdatedBy(updatedBy);
                repository.save(dynamicData);
            } else {
                throw new RuntimeException("Record not found for userId: " + userId + ", key: " + key);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update dynamic data", e);
        }
    }

    // Overloaded for backward compatibility
    public void updateDynamicData(String userId, String key, Object data) {
        updateDynamicData(userId, key, data, java.time.Instant.now(), "system");
    }
}
