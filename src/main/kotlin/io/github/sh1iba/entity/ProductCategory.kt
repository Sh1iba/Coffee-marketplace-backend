package io.github.sh1iba.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(name = "product_categories")
data class ProductCategory @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int = 0,

    @Size(max = 50)
    @NotNull
    @Column(name = "type", nullable = false, length = 50)
    var type: String = ""
)
