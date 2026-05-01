package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.Seller

interface SellerRepository : JpaRepository<Seller, Long> {
    fun findByUserId(userId: Long): Seller?
    fun existsByUserId(userId: Long): Boolean
    fun findAllByIsActiveTrue(): List<Seller>
}
