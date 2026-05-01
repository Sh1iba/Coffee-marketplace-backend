package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Товар маркетплейса")
data class ProductResponse(
    @field:Schema(description = "ID товара", example = "2")
    val id: Int,

    @field:Schema(description = "Категория товара")
    val category: ProductCategoryResponse,

    @field:Schema(description = "Название товара", example = "Раф")
    val name: String,

    @field:Schema(description = "Описание товара", example = "Кофейный напиток со сливками")
    val description: String,

    @field:Schema(description = "Имя файла изображения", example = "raf.jpg")
    val imageName: String,

    @field:Schema(description = "Варианты товара (размер и цена)")
    val variants: List<ProductVariantResponse>,

    @field:Schema(description = "ID магазина продавца", example = "1")
    val sellerId: Long? = null,

    @field:Schema(description = "Название магазина продавца", example = "Coffee House")
    val sellerName: String? = null
)
