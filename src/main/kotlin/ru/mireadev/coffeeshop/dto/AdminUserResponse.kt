package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema
import ru.mireadev.coffeeshop.entity.Role

@Schema(description = "Информация о пользователе (для администратора)")
data class AdminUserResponse(
    @field:Schema(description = "ID пользователя", example = "1")
    val id: Long,

    @field:Schema(description = "Email", example = "user@example.com")
    val email: String,

    @field:Schema(description = "Имя", example = "Иван Иванов")
    val name: String,

    @field:Schema(description = "Роль", example = "BUYER")
    val role: Role
)
