package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Отзыв о магазине")
data class ReviewResponse(
    @field:Schema(description = "ID отзыва", example = "1")
    val id: Long,

    @field:Schema(description = "ID пользователя", example = "3")
    val userId: Long,

    @field:Schema(description = "Имя пользователя", example = "Анна")
    val userName: String,

    @field:Schema(description = "ID магазина (сети)", example = "1")
    val sellerId: Long,

    @field:Schema(description = "Название магазина", example = "Urban Brew")
    val sellerName: String,

    @field:Schema(description = "ID заказа", example = "42")
    val orderId: Long,

    @field:Schema(description = "Оценка 1–5", example = "5")
    val rating: Int,

    @field:Schema(description = "Текст отзыва")
    val comment: String?,

    @field:Schema(description = "Дата создания")
    val createdAt: LocalDateTime
)
