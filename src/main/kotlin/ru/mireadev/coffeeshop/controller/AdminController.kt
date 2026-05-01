package ru.mireadev.coffeeshop.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import ru.mireadev.coffeeshop.dto.AdminUserResponse
import ru.mireadev.coffeeshop.dto.OrderStatusRequest
import ru.mireadev.coffeeshop.dto.RoleChangeRequest
import ru.mireadev.coffeeshop.dto.SellerResponse
import ru.mireadev.coffeeshop.service.AdminService
import ru.mireadev.coffeeshop.service.OrderService

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Администратор", description = "Управление пользователями, магазинами и заказами")
class AdminController(
    private val adminService: AdminService,
    private val orderService: OrderService
) {

    // ── Пользователи ───────────────────────────────────────────────────────

    @Operation(summary = "Все пользователи")
    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<List<AdminUserResponse>> =
        ResponseEntity.ok(adminService.getAllUsers())

    @Operation(summary = "Изменить роль пользователя")
    @PutMapping("/users/{userId}/role")
    fun changeUserRole(
        @PathVariable userId: Long,
        @RequestBody request: RoleChangeRequest
    ): ResponseEntity<Any> =
        when (val result = adminService.changeUserRole(userId, request.role)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Роль обновлена"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    // ── Магазины ───────────────────────────────────────────────────────────

    @Operation(summary = "Все магазины (включая неактивные)")
    @GetMapping("/sellers")
    fun getAllSellers(): ResponseEntity<List<SellerResponse>> =
        ResponseEntity.ok(adminService.getAllSellers())

    @Operation(summary = "Активировать магазин")
    @PutMapping("/sellers/{sellerId}/activate")
    fun activateSeller(@PathVariable sellerId: Long): ResponseEntity<Any> =
        when (val result = adminService.activateSeller(sellerId)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Магазин активирован"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    @Operation(summary = "Деактивировать магазин")
    @PutMapping("/sellers/{sellerId}/deactivate")
    fun deactivateSeller(@PathVariable sellerId: Long): ResponseEntity<Any> =
        when (val result = adminService.deactivateSeller(sellerId)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Магазин деактивирован"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    // ── Заказы ─────────────────────────────────────────────────────────────

    @Operation(summary = "Изменить статус заказа")
    @PutMapping("/orders/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody request: OrderStatusRequest
    ): ResponseEntity<Any> =
        orderService.updateOrderStatus(orderId, request.status)
}
