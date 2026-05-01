package ru.mireadev.coffeeshop.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.mireadev.coffeeshop.entity.CartItem
import ru.mireadev.coffeeshop.entity.CartItemId

interface CartItemRepository : JpaRepository<CartItem, CartItemId> {
    fun findAllByUserId(userId: Long): List<CartItem>
    fun existsByUserIdAndProductIdAndSelectedSize(userId: Long, productId: Int, selectedSize: String): Boolean
    fun deleteByUserIdAndProductIdAndSelectedSize(userId: Long, productId: Int, selectedSize: String)
    fun findByUserIdAndProductIdAndSelectedSize(userId: Long, productId: Int, selectedSize: String): CartItem?
    fun deleteAllByUserId(userId: Long)
}
