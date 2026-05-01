package io.github.sh1iba.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(name = "products")
data class Product @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int = 0,

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    var category: ProductCategory = ProductCategory(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = true)
    var seller: Seller? = null,

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    var name: String = "",

    @Size(max = 500)
    @NotNull
    @Column(name = "description", nullable = false, length = 500)
    var description: String = "",

    @Size(max = 50)
    @NotNull
    @Column(name = "image_name", nullable = false, length = 50)
    var imageName: String = "",

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var variants: MutableList<ProductVariant> = mutableListOf()
)
