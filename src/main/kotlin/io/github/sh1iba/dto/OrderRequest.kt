package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import io.github.sh1iba.entity.DeliveryType
import java.math.BigDecimal

@Schema(description = "Запрос на создание заказа")
data class OrderRequest(
    @field:Schema(description = "ID филиала, из которого оформляется заказ", example = "1")
    val branchId: Long? = null,

    @field:Schema(description = "Адрес доставки (обязателен для DELIVERY)", example = "ул. Навои, 45, кв. 10")
    val deliveryAddress: String? = null,

    @field:Schema(description = "Стоимость доставки", example = "15000.00")
    val deliveryFee: BigDecimal = BigDecimal.ZERO,

    @field:Schema(description = "Тип получения: DELIVERY или PICKUP", example = "DELIVERY")
    val deliveryType: DeliveryType = DeliveryType.DELIVERY,

    @field:Schema(description = "Выбранные товары для заказа", required = true)
    val items: List<OrderCartItem>
)

@Schema(description = "Элемент корзины для заказа")
data class OrderCartItem(
    @field:Schema(description = "ID товара", example = "1", required = true)
    val productId: Int,

    @field:Schema(description = "Выбранный размер / вариант", example = "L", required = true)
    val selectedSize: String
)
