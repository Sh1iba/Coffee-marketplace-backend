package io.github.sh1iba.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import io.github.sh1iba.entity.BranchStatus

@Entity
@Table(name = "branches")
data class Branch @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0,

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    var seller: Seller = Seller(),

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    var name: String = "",

    @Size(max = 300)
    @NotNull
    @Column(name = "address", nullable = false, length = 300)
    var address: String = "",

    @Size(max = 100)
    @NotNull
    @Column(name = "city", nullable = false, length = 100)
    var city: String = "",

    @Column(name = "latitude")
    var latitude: Double? = null,

    @Column(name = "longitude")
    var longitude: Double? = null,

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    var deliveryFee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    var minOrderAmount: BigDecimal = BigDecimal.ZERO,

    @Size(max = 50)
    @Column(name = "working_hours", length = 50)
    var workingHours: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    var status: BranchStatus = BranchStatus.PENDING,

    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null
)
