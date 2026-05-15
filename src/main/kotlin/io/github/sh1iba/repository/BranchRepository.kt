package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
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
}
