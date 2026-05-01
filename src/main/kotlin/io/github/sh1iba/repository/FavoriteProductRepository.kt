package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.FavoriteProduct
import io.github.sh1iba.entity.FavoriteProductId

interface FavoriteProductRepository : JpaRepository<FavoriteProduct, FavoriteProductId> {
    fun findAllByUserId(userId: Long): List<FavoriteProduct>
    fun existsByUserIdAndProductIdAndSelectedSize(userId: Long, productId: Int, selectedSize: String): Boolean
    fun deleteByUserIdAndProductIdAndSelectedSize(userId: Long, productId: Int, selectedSize: String)
    fun findByUserIdAndProductIdAndSelectedSize(userId: Long, productId: Int, selectedSize: String): FavoriteProduct?
    fun existsByUserIdAndProductId(userId: Long, productId: Int): Boolean
    fun deleteAllByUserIdAndProductId(userId: Long, productId: Int)
    fun findAllByUserIdAndProductId(userId: Long, productId: Int): List<FavoriteProduct>
}
