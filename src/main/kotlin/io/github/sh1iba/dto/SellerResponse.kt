package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Информация о магазине продавца")
data class SellerResponse(

    @field:Schema(description = "ID магазина", example = "1")
    val id: Long,

    @field:Schema(description = "Название магазина", example = "Coffee House")
    val name: String,

    @field:Schema(description = "Описание магазина", example = "Лучший кофе в городе")
    val description: String,

    @field:Schema(description = "Категория магазина", example = "Кофейня")
    val category: String,

    @field:Schema(description = "URL логотипа магазина")
    val logoUrl: String?,

    @field:Schema(description = "Рейтинг магазина", example = "4.8")
    val rating: Double,

    @field:Schema(description = "Активен ли магазин", example = "true")
    val isActive: Boolean,

    @field:Schema(description = "Контактный телефон")
    val phone: String?,

    @field:Schema(description = "Сайт магазина")
    val website: String?,

    @field:Schema(description = "ID владельца", example = "42")
    val ownerId: Long,

    @field:Schema(description = "Имя владельца", example = "Иван Иванов")
    val ownerName: String,

    @field:Schema(description = "Статус модерации", example = "PENDING", allowableValues = ["PENDING", "APPROVED", "REJECTED"])
    val status: String,

    @field:Schema(description = "Причина отклонения (если REJECTED)")
    val rejectionReason: String? = null
)
