package ru.mireadev.coffeeshop.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

enum class Role {
    BUYER, SELLER, ADMIN
}

@Entity
@Table(name = "users")
data class User @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0,

    @Size(max = 50)
    @NotNull
    @Column(name = "email", unique = true, nullable = false, length = 50)
    var email: String = "",

    @Size(max = 50)
    @NotNull
    @Column(name = "name", nullable = false, length = 50)
    var name: String = "",

    @Size(max = 64)
    @NotNull
    @Column(name = "password_hash", nullable = false, length = 64)
    var passwordHash: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    var role: Role = Role.BUYER
)