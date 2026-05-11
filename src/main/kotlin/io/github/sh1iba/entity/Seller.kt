package io.github.sh1iba.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

enum class SellerStatus { PENDING, APPROVED, REJECTED }

@Entity
@Table(name = "sellers")
data class Seller @JvmOverloads constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0,

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User = User(),

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    var name: String = "",

    @Size(max = 500)
    @NotNull
    @Column(name = "description", nullable = false, length = 500)
    var description: String = "",

    @Size(max = 500)
    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null,

    @Size(max = 50)
    @NotNull
    @Column(name = "category", nullable = false, length = 50)
    var category: String = "",

    @Column(name = "rating", nullable = false)
    var rating: Double = 0.0,

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    var phone: String? = null,

    @Size(max = 200)
    @Column(name = "website", length = 200)
    var website: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    var status: SellerStatus = SellerStatus.PENDING,

    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null
)
