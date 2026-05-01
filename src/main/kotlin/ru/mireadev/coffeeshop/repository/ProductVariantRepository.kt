package ru.mireadev.coffeeshop.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.mireadev.coffeeshop.entity.ProductVariant

interface ProductVariantRepository : JpaRepository<ProductVariant, Int> {
    fun findAllByProductId(productId: Int): List<ProductVariant>
    fun deleteAllByProductId(productId: Int)
}
