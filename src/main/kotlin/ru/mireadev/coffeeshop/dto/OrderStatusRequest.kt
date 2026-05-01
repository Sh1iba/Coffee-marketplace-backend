package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema
import ru.mireadev.coffeeshop.entity.OrderStatus

@Schema(description = "Запрос на изменение статуса заказа")
data class OrderStatusRequest(
    @field:Schema(description = "Новый статус", example = "CONFIRMED", required = true)
    val status: OrderStatus
)
