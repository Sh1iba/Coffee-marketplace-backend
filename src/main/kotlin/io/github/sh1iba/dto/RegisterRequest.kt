package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import io.github.sh1iba.entity.Role

@Schema(description = "Запрос на регистрацию пользователя")
data class RegisterRequest(

    @field:Schema(description = "Email пользователя", example = "user@example.com", required = true)
    @field:Email
    val email: String,

    @field:Schema(description = "Пароль", example = "SecurePass123!", required = true)
    val password: String,

    @field:Schema(description = "Имя", example = "Иван Иванов", required = true)
    val name: String,

    @field:Schema(description = "Роль пользователя: BUYER, SELLER", example = "BUYER", required = false)
    val role: Role = Role.BUYER
)