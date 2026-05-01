package ru.mireadev.coffeeshop.dto

import io.swagger.v3.oas.annotations.media.Schema
import ru.mireadev.coffeeshop.entity.Role

@Schema(description = "Запрос на изменение роли пользователя")
data class RoleChangeRequest(
    @field:Schema(description = "Новая роль", example = "SELLER", required = true)
    val role: Role
)
