package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import io.github.sh1iba.entity.Review

interface ReviewRepository : JpaRepository<Review, Long> {
    fun findAllBySellerId(sellerId: Long): List<Review>
    fun findByOrderId(orderId: Long): Review?
    fun existsByOrderId(orderId: Long): Boolean
    fun findAllByUserId(userId: Long): List<Review>

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.seller.id = :sellerId")
    fun avgRatingBySellerId(@Param("sellerId") sellerId: Long): Double?
}
