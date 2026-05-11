package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(description = "Запрос на создание или обновление филиала")
data class BranchRequest(
    @field:Schema(description = "Название филиала", example = "Urban Brew — Центр", required = true)
    @field:NotBlank @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "Адрес филиала", example = "ул. Навои, 12", required = true)
    @field:NotBlank @field:Size(max = 300)
    val address: String,

    @field:Schema(description = "Город", example = "Ташкент", required = true)
    @field:NotBlank @field:Size(max = 100)
    val city: String,

    @field:Schema(description = "Широта", example = "41.2995")
    val latitude: Double? = null,

    @field:Schema(description = "Долгота", example = "69.2401")
    val longitude: Double? = null,

    @field:Schema(description = "Стоимость доставки", example = "15000.00")
    val deliveryFee: BigDecimal = BigDecimal.ZERO,

    @field:Schema(description = "Минимальная сумма заказа", example = "30000.00")
    val minOrderAmount: BigDecimal = BigDecimal.ZERO,

    @field:Schema(description = "Часы работы", example = "09:00–22:00")
    @field:Size(max = 50)
    val workingHours: String? = null,

    @field:Schema(description = "Email менеджера (только при создании)")
    @field:Size(max = 50)
    val managerEmail: String? = null,

    @field:Schema(description = "Пароль менеджера (только при создании)")
    @field:Size(min = 4, max = 64)
    val managerPassword: String? = null
)
