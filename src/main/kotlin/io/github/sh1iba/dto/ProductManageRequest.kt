package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(description = "Запрос на создание или обновление товара")
data class ProductManageRequest(

    @field:Schema(description = "Название товара", example = "Раф", required = true)
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "Описание товара", example = "Кофейный напиток со сливками", required = true)
    @field:NotBlank
    @field:Size(max = 500)
    val description: String,

    @field:Schema(description = "ID категории", example = "1", required = true)
    val categoryId: Int,

    @field:Schema(description = "Имя файла изображения", example = "raf.jpg", required = true)
    @field:NotBlank
    @field:Size(max = 50)
    val imageName: String,

    @field:Schema(description = "Варианты товара (размер и цена)", required = true)
    @field:NotEmpty
    val variants: List<VariantRequest>
)

@Schema(description = "Вариант товара")
data class VariantRequest(
    @field:Schema(description = "Размер / вариант", example = "L", required = true)
    @field:NotBlank
    val size: String,

    @field:Schema(description = "Цена", example = "345.90", required = true)
    @field:Positive
    val price: BigDecimal
)
