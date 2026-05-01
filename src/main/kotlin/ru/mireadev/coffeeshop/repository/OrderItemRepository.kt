package ru.mireadev.coffeeshop.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.mireadev.coffeeshop.entity.OrderItem

interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findAllByOrderId(orderId: Long): List<OrderItem>
    fun findAllBySellerId(sellerId: Long): List<OrderItem>
}
