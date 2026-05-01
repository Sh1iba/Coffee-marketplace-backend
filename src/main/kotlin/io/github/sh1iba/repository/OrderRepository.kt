package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.Order

interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByUserIdOrderByOrderDateDesc(userId: Long): List<Order>
}
