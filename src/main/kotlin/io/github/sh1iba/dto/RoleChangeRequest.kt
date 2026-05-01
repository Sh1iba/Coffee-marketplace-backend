package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import io.github.sh1iba.entity.Role

@Schema(description = "Запрос на изменение роли пользователя")
data class RoleChangeRequest(
    @field:Schema(description = "Новая роль", example = "SELLER", required = true)
    val role: Role
)
