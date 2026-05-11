package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import io.github.sh1iba.entity.DeliveryType
import io.github.sh1iba.entity.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "Информация о заказе")
data class OrderResponse(
    @field:Schema(description = "ID заказа", example = "1")
    val id: Long,

    @field:Schema(description = "Общая сумма заказа", example = "125000.50")
    val totalAmount: BigDecimal,

    @field:Schema(description = "Стоимость доставки", example = "15000.00")
    val deliveryFee: BigDecimal,

    @field:Schema(description = "Адрес доставки (null для самовывоза)")
    val deliveryAddress: String?,

    @field:Schema(description = "ID филиала", example = "2")
    val branchId: Long?,

    @field:Schema(description = "Название филиала", example = "Urban Brew — Центр")
    val branchName: String?,

    @field:Schema(description = "ID курьера (null если не назначен)")
    val courierId: Long?,

    @field:Schema(description = "Тип получения", example = "DELIVERY")
    val deliveryType: DeliveryType,

    @field:Schema(description = "Дата заказа")
    val orderDate: LocalDateTime,

    @field:Schema(description = "Статус заказа", example = "COOKING")
    val status: OrderStatus,

    @field:Schema(description = "Элементы заказа")
    val items: List<OrderItemResponse>
)

@Schema(description = "Элемент заказа")
data class OrderItemResponse(
    @field:Schema(description = "ID элемента", example = "1")
    val id: Long,

    @field:Schema(description = "Название товара", example = "Раф")
    val productName: String,

    @field:Schema(description = "Выбранный вариант", example = "L")
    val selectedSize: String,

    @field:Schema(description = "Цена за единицу", example = "34500.90")
    val unitPrice: BigDecimal,

    @field:Schema(description = "Количество", example = "2")
    val quantity: Int,

    @field:Schema(description = "Общая стоимость элемента", example = "69001.80")
    val totalPrice: BigDecimal
)
