package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Формат ошибки авторизации")
data class ErrorResponse(
    @field:Schema(description = "Флаг успешности", example = "false")
    val success: Boolean,

    @field:Schema(description = "Сообщение об ошибке", example = "Ошибка")
    val message: String,

    @field:Schema(description = "Код ошибки", example = "AUTH_FAILED")
    val errorCode: String
)