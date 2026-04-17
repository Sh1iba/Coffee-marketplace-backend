package ru.mireadev.coffeeshop.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

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

    @Size(max = 50)
    @Column(name = "logo_image", length = 50)
    var logoImage: String? = null,

    @Size(max = 50)
    @NotNull
    @Column(name = "category", nullable = false, length = 50)
    var category: String = "",

    @Column(name = "rating", nullable = false)
    var rating: Double = 0.0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)
