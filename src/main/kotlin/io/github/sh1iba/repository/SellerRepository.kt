package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.Seller
import io.github.sh1iba.entity.SellerStatus

interface SellerRepository : JpaRepository<Seller, Long> {
    fun findByUserId(userId: Long): Seller?
    fun existsByUserId(userId: Long): Boolean
    fun findAllByIsActiveTrue(): List<Seller>
    fun findAllByStatus(status: SellerStatus): List<Seller>
    fun findAllByStatusAndIsActiveTrue(status: SellerStatus): List<Seller>
}
