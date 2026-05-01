package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import io.github.sh1iba.entity.OrderStatus

@Schema(description = "Запрос на изменение статуса заказа")
data class OrderStatusRequest(
    @field:Schema(description = "Новый статус", example = "CONFIRMED", required = true)
    val status: OrderStatus
)
