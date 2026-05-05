package io.github.sh1iba.entity

import jakarta.persistence.*
import java.time.LocalDateTime

enum class InteractionType { VIEW, FAVORITE, CART }

@Entity
@Table(name = "user_product_interactions")
data class UserProductInteraction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    val type: InteractionType,

    @Column(name = "interacted_at", nullable = false)
    val interactedAt: LocalDateTime = LocalDateTime.now()
)
