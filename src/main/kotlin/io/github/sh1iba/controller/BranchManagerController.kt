package io.github.sh1iba.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.github.sh1iba.dto.OrderStatusRequest
import io.github.sh1iba.dto.SellerOrderResponse
import io.github.sh1iba.service.BranchManagerService
import io.github.sh1iba.service.UserService

@RestController
@RequestMapping("/api/branch-manager")
@Tag(name = "Менеджер филиала", description = "Операции сотрудника конкретного филиала")
class BranchManagerController(
    private val branchManagerService: BranchManagerService,
    private val userService: UserService
) {

    @Operation(summary = "Мой профиль и информация о филиале [BRANCH_MANAGER]")
    @GetMapping("/me")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    fun getMyProfile(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchManagerService.getMyProfile(userId)
    }

    @Operation(summary = "Заказы моего филиала [BRANCH_MANAGER]")
    @GetMapping("/orders")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    fun getBranchOrders(authentication: Authentication): ResponseEntity<List<SellerOrderResponse>> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchManagerService.getBranchOrders(userId)
    }

    @Operation(
        summary = "Обновить статус заказа [BRANCH_MANAGER]",
        description = "Допустимые статусы: CONFIRMED, COOKING, READY_FOR_PICKUP"
    )
    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody request: OrderStatusRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchManagerService.updateOrderStatus(userId, orderId, request.status)
    }
}
