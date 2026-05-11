package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.Order
import io.github.sh1iba.entity.OrderStatus

interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByUserIdOrderByOrderDateDesc(userId: Long): List<Order>
    fun findAllByStatusAndCourierIdIsNull(status: OrderStatus): List<Order>
    fun findByCourierIdAndStatusIn(courierId: Long, statuses: List<OrderStatus>): List<Order>
    fun findAllByBranchIdOrderByOrderDateDesc(branchId: Long): List<Order>
}
