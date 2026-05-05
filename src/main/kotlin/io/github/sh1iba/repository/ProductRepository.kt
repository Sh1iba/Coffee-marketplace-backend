package io.github.sh1iba.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import io.github.sh1iba.entity.Product

interface ProductRepository : JpaRepository<Product, Int> {

    @Query("""
        SELECT p FROM Product p
        WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:sellerId IS NULL OR (p.seller IS NOT NULL AND p.seller.id = :sellerId))
        AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
    """)
    fun findWithFilters(
        @Param("categoryId") categoryId: Int?,
        @Param("sellerId") sellerId: Long?,
        @Param("name") name: String?,
        pageable: Pageable
    ): Page<Product>

    fun findAllBySellerId(sellerId: Long): List<Product>

    fun countByImageName(imageName: String): Long

    @Query("""
        SELECT p FROM Product p
        WHERE p.category.id IN :categoryIds
        AND p.id NOT IN :excludeIds
    """)
    fun findByCategoryIdInAndIdNotIn(
        @Param("categoryIds") categoryIds: List<Int>,
        @Param("excludeIds") excludeIds: List<Int>,
        pageable: Pageable
    ): Page<Product>
}
