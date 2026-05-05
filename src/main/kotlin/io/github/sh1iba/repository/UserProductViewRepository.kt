package io.github.sh1iba.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import io.github.sh1iba.entity.InteractionType
import io.github.sh1iba.entity.UserProductInteraction
import java.time.LocalDateTime

interface UserProductInteractionRepository : JpaRepository<UserProductInteraction, Long> {

    @Query(value = """
        SELECT product_id FROM user_product_interactions
        WHERE interacted_at >= :since
        GROUP BY product_id
        ORDER BY SUM(CASE interaction_type
            WHEN 'VIEW'     THEN 1
            WHEN 'CART'     THEN 2
            WHEN 'FAVORITE' THEN 3
            ELSE 0 END) DESC
    """, nativeQuery = true)
    fun findTopProductIdsByScore(
        @Param("since") since: LocalDateTime,
        pageable: Pageable
    ): List<Int>

    @Query("SELECT DISTINCT v.productId FROM UserProductInteraction v WHERE v.userId = :userId AND v.interactedAt >= :since")
    fun findProductIdsByUserSince(
        @Param("userId") userId: Long,
        @Param("since") since: LocalDateTime
    ): List<Int>

    fun existsByUserIdAndProductIdAndTypeAndInteractedAtAfter(
        userId: Long,
        productId: Int,
        type: InteractionType,
        after: LocalDateTime
    ): Boolean
}
