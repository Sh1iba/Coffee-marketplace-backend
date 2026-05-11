package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.BranchManager

interface BranchManagerRepository : JpaRepository<BranchManager, Long> {
    fun findByUserId(userId: Long): BranchManager?
    fun existsByUserId(userId: Long): Boolean
    fun findAllByBranchId(branchId: Long): List<BranchManager>
}
