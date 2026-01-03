package com.saasapp.dynamic_app.repository;

import com.saasapp.dynamic_app.entity.DynamicData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DynamicDataRepository extends JpaRepository<DynamicData, Long> {
    Optional<DynamicData> findByUserIdAndKey(String userId, String key);
}
