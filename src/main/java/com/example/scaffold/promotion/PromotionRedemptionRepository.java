package com.example.scaffold.promotion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption, Long> {

    long countByPromotionIdAndUserId(Long promotionId, Long userId);
}
