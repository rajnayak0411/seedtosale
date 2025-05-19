package com.seedtosale.repository;

import com.seedtosale.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByItemTypeAndItemIdAndQuantity(String itemType, Long itemId, int quantity);
    java.util.List<Order> findByUserId(Long userId);
}
