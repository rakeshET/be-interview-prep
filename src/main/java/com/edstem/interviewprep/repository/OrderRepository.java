package com.edstem.interviewprep.repository;

import com.edstem.interviewprep.entity.Order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByCustomerAndIdempotencyKey(String customer, String idempotencyKey);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    List<Order> findByCustomerOrderByIdDesc(String customer);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Order o set o.status = com.edstem.interviewprep.entity.OrderStatus.CANCELLED "
            + "where o.id = :id and o.status = com.edstem.interviewprep.entity.OrderStatus.PLACED")
    int markCancelled(@Param("id") Long id);
}
