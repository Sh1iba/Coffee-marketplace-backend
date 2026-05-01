package ru.mireadev.coffeeshop.entity

import jakarta.persistence.*

@Entity
@Table(name = "cart_items")
@IdClass(CartItemId::class)
data class CartItem(
    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Id
    @Column(name = "coffee_id")
    val productId: Int,

    @Id
    @Column(name = "selected_size", nullable = false, length = 10)
    val selectedSize: String,

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 1,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coffee_id", insertable = false, updatable = false)
    val product: Product? = null
)

data class CartItemId(
    val userId: Long = 0,
    val productId: Int = 0,
    val selectedSize: String = ""
) : java.io.Serializable
