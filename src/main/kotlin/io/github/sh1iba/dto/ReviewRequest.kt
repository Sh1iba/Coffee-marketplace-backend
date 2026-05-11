package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание отзыва")
data class ReviewRequest(
    @field:Schema(description = "ID заказа (должен быть в статусе DELIVERED)", example = "42", required = true)
    val orderId: Long,

    @field:Schema(description = "Оценка от 1 до 5", example = "5", required = true)
    @field:Min(1) @field:Max(5)
    val rating: Int,

    @field:Schema(description = "Текст отзыва (необязательно)", example = "Отличный кофе!")
    @field:Size(max = 1000)
    val comment: String? = null
)
