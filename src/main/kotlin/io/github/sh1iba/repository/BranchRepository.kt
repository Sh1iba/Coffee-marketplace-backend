package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import io.github.sh1iba.entity.Branch
import io.github.sh1iba.entity.BranchStatus

interface BranchRepository : JpaRepository<Branch, Long> {
    fun findAllBySellerId(sellerId: Long): List<Branch>
    fun findAllBySellerIdAndIsActiveTrue(sellerId: Long): List<Branch>
    fun findAllByCityIgnoreCaseAndIsActiveTrue(city: String): List<Branch>
    fun findAllByIsActiveTrue(): List<Branch>
    fun existsBySellerId(sellerId: Long): Boolean
    fun findFirstBySellerId(sellerId: Long): Branch?
    fun findAllByStatus(status: BranchStatus): List<Branch>
    fun existsBySellerIdAndStatus(sellerId: Long, status: BranchStatus): Boolean
    fun findAllBySellerIdAndIsActiveTrueAndStatus(sellerId: Long, status: BranchStatus): List<Branch>
    fun findByEmail(email: String): Branch?
    @Query("SELECT b FROM Branch b JOIN FETCH b.seller")
    fun findAllWithSeller(): List<Branch>
}
