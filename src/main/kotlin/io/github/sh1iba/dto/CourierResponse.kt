package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Информация о курьере")
data class CourierResponse(
    @field:Schema(description = "ID курьера", example = "1")
    val id: Long,

    @field:Schema(description = "ID пользователя", example = "5")
    val userId: Long,

    @field:Schema(description = "Имя курьера", example = "Алишер Набиев")
    val userName: String,

    @field:Schema(description = "Email курьера", example = "courier@mail.ru")
    val userEmail: String,

    @field:Schema(description = "Доступен ли курьер", example = "true")
    val isAvailable: Boolean,

    @field:Schema(description = "Широта (текущее местоположение)")
    val latitude: Double?,

    @field:Schema(description = "Долгота (текущее местоположение)")
    val longitude: Double?
)
