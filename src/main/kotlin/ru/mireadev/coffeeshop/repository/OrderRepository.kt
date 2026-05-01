package ru.mireadev.coffeeshop.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.mireadev.coffeeshop.entity.Order

interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByUserIdOrderByOrderDateDesc(userId: Long): List<Order>
}
