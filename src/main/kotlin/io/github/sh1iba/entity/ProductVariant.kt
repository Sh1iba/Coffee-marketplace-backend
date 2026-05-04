package io.github.sh1iba.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "product_variants")
data class ProductVariant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coffee_id", nullable = false)
    val product: Product,

    @Column(name = "size", nullable = false)
    val size: String,

    @Column(name = "price", nullable = false)
    val price: BigDecimal,

    @Column(name = "volume", nullable = true)
    val volume: String? = null
)
