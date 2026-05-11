package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.Courier

interface CourierRepository : JpaRepository<Courier, Long> {
    fun findByUserId(userId: Long): Courier?
    fun existsByUserId(userId: Long): Boolean
    fun findAllByIsAvailableTrue(): List<Courier>
}
