package io.github.sh1iba.repository

import org.springframework.data.jpa.repository.JpaRepository
import io.github.sh1iba.entity.ProductVariant

interface ProductVariantRepository : JpaRepository<ProductVariant, Int> {
    fun findAllByProductId(productId: Int): List<ProductVariant>
    fun deleteAllByProductId(productId: Int)
}
