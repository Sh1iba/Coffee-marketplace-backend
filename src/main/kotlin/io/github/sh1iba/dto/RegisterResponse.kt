package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import io.github.sh1iba.entity.Role

@Schema(description = "Ответ после успешной регистрации")
data class RegisterResponse(

    @field:Schema(description = "ID пользователя", example = "123")
    val userID: Long,

    @field:Schema(description = "Email", example = "user@example.com")
    val email: String,

    @field:Schema(description = "Имя", example = "Иван Иванов")
    @field:Size(min = 1, max = 50)
    val name: String,

    @field:Schema(description = "Роль пользователя", example = "BUYER")
    val role: Role
)