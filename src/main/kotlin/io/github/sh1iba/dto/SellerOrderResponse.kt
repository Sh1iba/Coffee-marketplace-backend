package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import io.github.sh1iba.entity.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "Заказ, содержащий товары продавца")
data class SellerOrderResponse(
    @field:Schema(description = "ID заказа", example = "1")
    val orderId: Long,

    @field:Schema(description = "Дата заказа")
    val orderDate: LocalDateTime,

    @field:Schema(description = "Статус заказа", example = "PENDING")
    val status: OrderStatus,

    @field:Schema(description = "Адрес доставки (null для самовывоза)")
    val deliveryAddress: String?,

    @field:Schema(description = "Сумма по товарам продавца в этом заказе", example = "690.00")
    val itemsTotal: BigDecimal,

    @field:Schema(description = "Товары продавца в этом заказе")
    val items: List<OrderItemResponse>
)
