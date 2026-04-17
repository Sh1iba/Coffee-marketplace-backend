package ru.mireadev.coffeeshop.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.mireadev.coffeeshop.entity.Seller

interface SellerRepository : JpaRepository<Seller, Long> {
    fun findByUserId(userId: Long): Seller?
    fun existsByUserId(userId: Long): Boolean
    fun findAllByIsActiveTrue(): List<Seller>
}
