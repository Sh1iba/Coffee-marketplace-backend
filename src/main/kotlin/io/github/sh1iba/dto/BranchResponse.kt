package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Информация о филиале")
data class BranchResponse(
    @field:Schema(description = "ID филиала", example = "1")
    val id: Long,

    @field:Schema(description = "ID сети (продавца)", example = "1")
    val sellerId: Long,

    @field:Schema(description = "Название сети", example = "Urban Brew")
    val sellerName: String,

    @field:Schema(description = "Название филиала", example = "Urban Brew — Центр")
    val name: String,

    @field:Schema(description = "Адрес", example = "ул. Навои, 12")
    val address: String,

    @field:Schema(description = "Город", example = "Ташкент")
    val city: String,

    @field:Schema(description = "Широта")
    val latitude: Double?,

    @field:Schema(description = "Долгота")
    val longitude: Double?,

    @field:Schema(description = "Стоимость доставки", example = "15000.00")
    val deliveryFee: BigDecimal,

    @field:Schema(description = "Минимальная сумма заказа", example = "30000.00")
    val minOrderAmount: BigDecimal,

    @field:Schema(description = "Часы работы", example = "09:00–22:00")
    val workingHours: String?,

    @field:Schema(description = "Активен ли филиал", example = "true")
    val isActive: Boolean,

    @field:Schema(description = "Email менеджера филиала")
    val managerEmail: String?
)
