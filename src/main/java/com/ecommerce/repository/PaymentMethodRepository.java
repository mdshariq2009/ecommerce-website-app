package com.ecommerce.repository;

import com.ecommerce.model.PaymentMethod;
import com.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUser(User user);
    List<PaymentMethod> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);
    Optional<PaymentMethod> findByUserAndIsDefault(User user, Boolean isDefault);
    Optional<PaymentMethod> findByUserAndStripePaymentMethodId(User user, String stripePaymentMethodId);
}
