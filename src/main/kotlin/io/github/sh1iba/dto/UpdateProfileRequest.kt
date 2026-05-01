package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на обновление профиля")
data class UpdateProfileRequest(
    @field:Schema(description = "Новое имя", example = "Иван Петров", required = false)
    @field:Size(min = 1, max = 50)
    val name: String? = null,

    @field:Schema(description = "Текущий пароль (обязателен при смене пароля)", required = false)
    val currentPassword: String? = null,

    @field:Schema(description = "Новый пароль", required = false)
    @field:Size(min = 6, max = 100)
    val newPassword: String? = null
)
