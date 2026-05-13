package io.github.sh1iba.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import io.github.sh1iba.dto.AdminUserResponse
import io.github.sh1iba.dto.BranchResponse
import io.github.sh1iba.dto.CourierResponse
import io.github.sh1iba.dto.OrderStatusRequest
import io.github.sh1iba.dto.ProductResponse
import io.github.sh1iba.dto.RejectSellerRequest
import io.github.sh1iba.dto.RoleChangeRequest
import io.github.sh1iba.dto.SellerResponse
import io.github.sh1iba.service.AdminService
import io.github.sh1iba.service.BranchService
import io.github.sh1iba.service.OrderService

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Администратор", description = "Управление пользователями, магазинами и заказами")
class AdminController(
    private val adminService: AdminService,
    private val orderService: OrderService,
    private val branchService: BranchService
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

    @Operation(summary = "Магазины на модерации")
    @GetMapping("/sellers/pending")
    fun getPendingSellers(): ResponseEntity<List<SellerResponse>> =
        ResponseEntity.ok(adminService.getPendingSellers())

    @Operation(summary = "Одобрить магазин")
    @PutMapping("/sellers/{sellerId}/approve")
    fun approveSeller(@PathVariable sellerId: Long): ResponseEntity<Any> =
        when (val result = adminService.approveSeller(sellerId)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Магазин одобрен"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    @Operation(summary = "Отклонить магазин")
    @PutMapping("/sellers/{sellerId}/reject")
    fun rejectSeller(
        @PathVariable sellerId: Long,
        @RequestBody request: RejectSellerRequest
    ): ResponseEntity<Any> =
        when (val result = adminService.rejectSeller(sellerId, request.reason)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Магазин отклонён"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

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

    // ── Товары ────────────────────────────────────────────────────────────

    @Operation(summary = "Все товары на модерации (PENDING)")
    @GetMapping("/products/pending")
    fun getPendingProducts(): ResponseEntity<List<ProductResponse>> =
        ResponseEntity.ok(adminService.getPendingProducts())

    @Operation(summary = "Товары магазина (все статусы)")
    @GetMapping("/sellers/{sellerId}/products")
    fun getSellerProducts(@PathVariable sellerId: Long): ResponseEntity<List<ProductResponse>> =
        ResponseEntity.ok(adminService.getSellerProducts(sellerId))

    @Operation(summary = "Одобрить товар")
    @PutMapping("/products/{productId}/approve")
    fun approveProduct(@PathVariable productId: Int): ResponseEntity<Any> =
        when (val result = adminService.approveProduct(productId)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Товар одобрен"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    @Operation(summary = "Отклонить товар")
    @PutMapping("/products/{productId}/reject")
    fun rejectProduct(
        @PathVariable productId: Int,
        @RequestBody request: RejectSellerRequest
    ): ResponseEntity<Any> =
        when (val result = adminService.rejectProduct(productId, request.reason)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Товар отклонён"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    @Operation(summary = "Удалить товар")
    @DeleteMapping("/products/{productId}")
    fun deleteProduct(@PathVariable productId: Int): ResponseEntity<Any> =
        when (val result = adminService.deleteProduct(productId)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Товар удалён"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    // ── Курьеры ────────────────────────────────────────────────────────────

    @Operation(summary = "Все курьеры")
    @GetMapping("/couriers")
    fun getAllCouriers(): ResponseEntity<List<CourierResponse>> =
        ResponseEntity.ok(adminService.getAllCouriers())

    @Operation(summary = "Переключить доступность курьера")
    @PutMapping("/couriers/{courierId}/toggle")
    fun toggleCourier(@PathVariable courierId: Long): ResponseEntity<Any> =
        when (val result = adminService.toggleCourierAvailability(courierId)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Статус курьера обновлён"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    @Operation(summary = "Снять роль курьера (роль → BUYER)")
    @DeleteMapping("/couriers/{courierId}")
    fun removeCourier(@PathVariable courierId: Long): ResponseEntity<Any> =
        when (val result = adminService.removeCourierRole(courierId)) {
            is AdminService.AdminResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Роль курьера снята"))
            is AdminService.AdminResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }

    // ── Филиалы ────────────────────────────────────────────────────────────

    @Operation(summary = "Все активные филиалы")
    @GetMapping("/branches")
    fun getAllBranches(): ResponseEntity<List<BranchResponse>> =
        branchService.getAllActiveBranches()

    // ── Заказы ─────────────────────────────────────────────────────────────

    @Operation(summary = "Изменить статус заказа")
    @PutMapping("/orders/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody request: OrderStatusRequest
    ): ResponseEntity<Any> =
        orderService.updateOrderStatus(orderId, request.status)
}
