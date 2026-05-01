package ru.mireadev.coffeeshop.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class OrderStatus {
    PENDING, CONFIRMED, PROCESSING, DELIVERED, CANCELLED
}

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: BigDecimal,

    @Column(name = "delivery_fee", nullable = false)
    var deliveryFee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "delivery_address", nullable = false)
    val deliveryAddress: String,

    @Column(name = "order_date", nullable = false)
    val orderDate: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING
)

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order,

    @Column(name = "product_name", nullable = false)
    val productName: String,

    @Column(name = "selected_size", nullable = false)
    val selectedSize: String,

    @Column(name = "unit_price", nullable = false)
    val unitPrice: BigDecimal,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Column(name = "total_price", nullable = false)
    val totalPrice: BigDecimal,

    @Column(name = "seller_id", nullable = true)
    val sellerId: Long? = null
)
