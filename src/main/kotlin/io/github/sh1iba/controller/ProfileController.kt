package io.github.sh1iba.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.github.sh1iba.dto.UpdateProfileRequest
import io.github.sh1iba.service.UserService

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Профиль", description = "Просмотр и обновление профиля пользователя")
class ProfileController(
    private val userService: UserService
) {

    @Operation(summary = "Получить свой профиль")
    @GetMapping
    fun getProfile(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return userService.getProfile(userId)
    }

    @Operation(
        summary = "Обновить профиль",
        description = "Можно обновить имя и/или пароль. Для смены пароля обязательно укажите currentPassword."
    )
    @PutMapping
    fun updateProfile(
        authentication: Authentication,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return userService.updateProfile(userId, request)
    }
}
