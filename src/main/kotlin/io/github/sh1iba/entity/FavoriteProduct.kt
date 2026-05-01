package io.github.sh1iba.entity

import jakarta.persistence.*

@Entity
@Table(name = "favorite_products")
@IdClass(FavoriteProductId::class)
data class FavoriteProduct(
    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Id
    @Column(name = "coffee_id")
    val productId: Int,

    @Id
    @Column(name = "selected_size")
    val selectedSize: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coffee_id", insertable = false, updatable = false)
    val product: Product? = null
)

data class FavoriteProductId(
    val userId: Long = 0,
    val productId: Int = 0,
    val selectedSize: String = ""
) : java.io.Serializable
