package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import io.github.sh1iba.entity.Role

data class LoginResponse(
    @field:Schema(description = "ID пользователя", example = "123")
    val userId: Long,

    @field:Schema(description = "JWT токен", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val token: String,

    @field:Schema(description = "Email пользователя", example = "user@example.com")
    val email: String,

    @field:Schema(description = "Имя", example = "Иван Иванов")
    val name: String,

    @field:Schema(description = "Роль пользователя", example = "BUYER")
    val role: Role
)