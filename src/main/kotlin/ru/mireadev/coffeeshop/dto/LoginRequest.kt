package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email

@Schema(description = "Запрос на авторизацию")
data class LoginRequest(
    @field:Schema(description = "Email пользователя", example = "user@example.com", required = true)
    @field:Email
    val email: String,

    @field:Schema(description = "Пароль", example = "Qwerty123!", required = true)
    val password: String
)
