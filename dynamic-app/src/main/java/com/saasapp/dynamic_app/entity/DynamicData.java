package com.saasapp.dynamic_app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_dynamic_data")
@Data
@NoArgsConstructor
public class DynamicData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "data", columnDefinition = "TEXT", nullable = true)
    private String data;

    @Column(name = "updated_time")
    private java.time.Instant updatedTime;

    @Column(name = "updated_by")
    private String updatedBy;

    // Custom constructor with id
    public DynamicData(Long id, String userId, String key, String data, java.time.Instant updatedTime, String updatedBy) {
        this.id = id;
        this.userId = userId;
        this.key = key;
        this.data = data;
        this.updatedTime = updatedTime;
        this.updatedBy = updatedBy;
    }

    // Custom constructor without id
    public DynamicData(String userId, String key, String data, java.time.Instant updatedTime, String updatedBy) {
        this(null, userId, key, data, updatedTime, updatedBy);
    }
}
