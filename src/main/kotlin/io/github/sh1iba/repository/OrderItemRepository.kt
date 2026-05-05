package io.github.sh1iba.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import io.github.sh1iba.entity.OrderItem
import java.time.LocalDateTime

interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findAllByOrderId(orderId: Long): List<OrderItem>
    fun findAllBySellerId(sellerId: Long): List<OrderItem>

    @Query("""
        SELECT oi.productId FROM OrderItem oi
        WHERE oi.productId IS NOT NULL AND oi.order.orderDate >= :since
        GROUP BY oi.productId
        ORDER BY COUNT(oi) DESC
    """)
    fun findTopProductIdsByOrderCount(
        @Param("since") since: LocalDateTime,
        pageable: Pageable
    ): List<Int>

    @Query("SELECT DISTINCT oi.productId FROM OrderItem oi WHERE oi.order.userId = :userId AND oi.productId IS NOT NULL")
    fun findProductIdsByUserId(@Param("userId") userId: Long): List<Int>
}
