package io.github.sh1iba.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.github.sh1iba.dto.OrderResponse
import io.github.sh1iba.dto.OrderStatusRequest
import io.github.sh1iba.service.CourierService
import io.github.sh1iba.service.UserService

@RestController
@RequestMapping("/api/courier")
@Tag(name = "Курьер", description = "Операции курьера: регистрация, заказы, статус")
class CourierController(
    private val courierService: CourierService,
    private val userService: UserService
) {

    @Operation(summary = "Стать курьером (меняет роль на COURIER)")
    @PostMapping("/register")
    fun register(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return courierService.becomeCourier(userId)
    }

    @Operation(summary = "Мой профиль курьера [COURIER]")
    @GetMapping("/me")
    @PreAuthorize("hasRole('COURIER')")
    fun getMyProfile(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return courierService.getMyProfile(userId)
    }

    @Operation(summary = "Обновить доступность [COURIER]")
    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('COURIER')")
    fun updateAvailability(
        @RequestParam isAvailable: Boolean,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return courierService.updateAvailability(userId, isAvailable)
    }

    @Operation(summary = "Обновить местоположение [COURIER]")
    @PutMapping("/me/location")
    @PreAuthorize("hasRole('COURIER')")
    fun updateLocation(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return courierService.updateLocation(userId, latitude, longitude)
    }

    @Operation(summary = "Доступные заказы для взятия [COURIER]", description = "Заказы в статусе READY_FOR_PICKUP без назначенного курьера")
    @GetMapping("/orders/available")
    @PreAuthorize("hasRole('COURIER')")
    fun getAvailableOrders(): ResponseEntity<List<OrderResponse>> =
        courierService.getAvailableOrders()

    @Operation(summary = "Взять заказ [COURIER]")
    @PostMapping("/orders/{orderId}/take")
    @PreAuthorize("hasRole('COURIER')")
    fun takeOrder(
        @PathVariable orderId: Long,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return courierService.takeOrder(userId, orderId)
    }

    @Operation(summary = "Текущий заказ [COURIER]")
    @GetMapping("/orders/current")
    @PreAuthorize("hasRole('COURIER')")
    fun getCurrentOrder(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return courierService.getCurrentOrder(userId)
    }

    @Operation(summary = "Обновить статус доставки [COURIER]", description = "Допустимые статусы: DELIVERING, DELIVERED")
    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('COURIER')")
    fun updateDeliveryStatus(
        @PathVariable orderId: Long,
        @RequestBody request: OrderStatusRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return courierService.updateDeliveryStatus(userId, orderId, request.status)
    }
}
