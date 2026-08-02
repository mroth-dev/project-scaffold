package com.example.scaffold.promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.scaffold.exception.ConflictException;
import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.exception.ValidationException;
import com.example.scaffold.order.Order;
import com.example.scaffold.user.User;
import com.example.scaffold.user.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionRedemptionRepository promotionRedemptionRepository;
    private final UserRepository userRepository;

    public PromotionService(PromotionRepository promotionRepository,
            PromotionRedemptionRepository promotionRedemptionRepository, UserRepository userRepository) {
        this.promotionRepository = promotionRepository;
        this.promotionRedemptionRepository = promotionRedemptionRepository;
        this.userRepository = userRepository;
    }

    public List<PromotionDto> getPromotions() {
        log.debug("Fetching all promotions");
        return promotionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    public PromotionDto getPromotion(Long id) {
        log.debug("Fetching promotion: {}", id);
        return toDto(findPromotionOrThrow(id));
    }

    public PromotionDto createPromotion(PromotionRequest request) {
        log.debug("Creating promotion with code: {}", request.code());
        if (promotionRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ConflictException("A promotion with code '" + request.code() + "' already exists");
        }
        Promotion promotion = new Promotion();
        applyRequest(promotion, request);
        return toDto(promotionRepository.save(promotion));
    }

    public PromotionDto updatePromotion(Long id, PromotionRequest request) {
        log.debug("Updating promotion: {}", id);
        Promotion promotion = findPromotionOrThrow(id);
        promotionRepository.findByCodeIgnoreCase(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("A promotion with code '" + request.code() + "' already exists");
                });
        applyRequest(promotion, request);
        return toDto(promotionRepository.save(promotion));
    }

    public PromotionDto setActive(Long id, boolean active) {
        log.debug("Setting promotion {} active={}", id, active);
        Promotion promotion = findPromotionOrThrow(id);
        promotion.setActive(active);
        return toDto(promotionRepository.save(promotion));
    }

    public void deletePromotion(Long id) {
        log.debug("Deleting promotion: {}", id);
        if (!promotionRepository.existsById(id)) {
            throw new NotFoundException("Promotion", id);
        }
        promotionRepository.deleteById(id);
    }

    /**
     * Validates a code against the cart subtotal and computes the discount it
     * would apply, without touching usage counts. Safe to call repeatedly as
     * the cart changes (e.g. on every cart page render).
     */
    public PromotionQuote quote(String code, BigDecimal subtotal) {
        Promotion promotion = promotionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ValidationException("Promotion code '" + code + "' does not exist"));

        if (!promotion.isActive()) {
            throw new ValidationException("This promotion is no longer active");
        }
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            throw new ValidationException("This promotion is not active yet");
        }
        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            throw new ValidationException("This promotion has expired");
        }
        if (promotion.getUsageLimit() != null && promotion.getUsageCount() >= promotion.getUsageLimit()) {
            throw new ValidationException("This promotion has reached its usage limit");
        }
        if (promotion.getMinimumPurchaseAmount() != null
                && subtotal.compareTo(promotion.getMinimumPurchaseAmount()) < 0) {
            throw new ValidationException(
                    "A minimum purchase of £" + promotion.getMinimumPurchaseAmount() + " is required for this promotion");
        }

        return new PromotionQuote(promotion.getId(), promotion.getCode(), promotion.getName(), promotion.getType(),
                calculateDiscount(promotion, subtotal));
    }

    /** Re-validates per-customer usage; only meaningful once the customer is known, i.e. at checkout. */
    public void assertCustomerCanRedeem(Long promotionId, Long userId) {
        Promotion promotion = findPromotionOrThrow(promotionId);
        if (promotion.getUsageLimitPerCustomer() != null) {
            long alreadyRedeemed = promotionRedemptionRepository.countByPromotionIdAndUserId(promotionId, userId);
            if (alreadyRedeemed >= promotion.getUsageLimitPerCustomer()) {
                throw new ValidationException("You have already used this promotion the maximum number of times");
            }
        }
    }

    /** Records a redemption and bumps the overall usage count. Called once the order is confirmed. */
    public void redeem(Long promotionId, Long userId, Order order) {
        log.debug("Recording redemption of promotion {} by user {}", promotionId, userId);
        Promotion promotion = findPromotionOrThrow(promotionId);
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));

        PromotionRedemption redemption = new PromotionRedemption();
        redemption.setPromotion(promotion);
        redemption.setUser(user);
        redemption.setOrder(order);
        promotionRedemptionRepository.save(redemption);

        promotion.setUsageCount(promotion.getUsageCount() + 1);
        promotionRepository.save(promotion);
    }

    private BigDecimal calculateDiscount(Promotion promotion, BigDecimal subtotal) {
        BigDecimal discount = switch (promotion.getType()) {
            case FIXED_AMOUNT -> promotion.getValue();
            case PERCENTAGE -> subtotal.multiply(promotion.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FREE_SHIPPING -> BigDecimal.ZERO;
        };
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private Promotion findPromotionOrThrow(Long id) {
        return promotionRepository.findById(id).orElseThrow(() -> new NotFoundException("Promotion", id));
    }

    private void applyRequest(Promotion promotion, PromotionRequest request) {
        if (request.type() != PromotionType.FREE_SHIPPING) {
            if (request.value() == null || request.value().signum() <= 0) {
                throw new ValidationException("A positive value is required for this promotion type");
            }
            if (request.type() == PromotionType.PERCENTAGE && request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ValidationException("A percentage discount cannot exceed 100");
            }
        }
        if (request.startDate() != null && request.endDate() != null
                && !request.startDate().isBefore(request.endDate())) {
            throw new ValidationException("Start date must be before end date");
        }
        if (request.usageLimit() != null && request.usageLimit() <= 0) {
            throw new ValidationException("Usage limit must be positive");
        }
        if (request.usageLimitPerCustomer() != null && request.usageLimitPerCustomer() <= 0) {
            throw new ValidationException("Usage limit per customer must be positive");
        }
        if (request.minimumPurchaseAmount() != null && request.minimumPurchaseAmount().signum() < 0) {
            throw new ValidationException("Minimum purchase amount cannot be negative");
        }

        promotion.setName(request.name());
        promotion.setCode(request.code().trim().toUpperCase());
        promotion.setDescription(request.description());
        promotion.setType(request.type());
        promotion.setValue(request.type() == PromotionType.FREE_SHIPPING ? null : request.value());
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setUsageLimit(request.usageLimit());
        promotion.setUsageLimitPerCustomer(request.usageLimitPerCustomer());
        promotion.setMinimumPurchaseAmount(request.minimumPurchaseAmount());
        promotion.setActive(request.active());
    }

    private PromotionDto toDto(Promotion promotion) {
        return new PromotionDto(
                promotion.getId(),
                promotion.getName(),
                promotion.getCode(),
                promotion.getDescription(),
                promotion.getType(),
                promotion.getValue(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                promotion.getUsageLimit(),
                promotion.getUsageLimitPerCustomer(),
                promotion.getMinimumPurchaseAmount(),
                promotion.isActive(),
                promotion.getUsageCount(),
                promotion.getCreatedAt(),
                promotion.getUpdatedAt());
    }
}
