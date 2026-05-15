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

@RestController
@RequestMapping("/api/branch-manager")
@Tag(name = "Менеджер филиала", description = "Операции сотрудника конкретного филиала")
class BranchManagerController(
    private val branchManagerService: BranchManagerService
) {

    @Operation(summary = "Информация о моём филиале [BRANCH]")
    @GetMapping("/me")
    @PreAuthorize("hasRole('BRANCH')")
    fun getMyProfile(authentication: Authentication): ResponseEntity<Any> {
        val branchId = authentication.principal as Long
        return branchManagerService.getMyProfile(branchId)
    }

    @Operation(summary = "Заказы моего филиала [BRANCH]")
    @GetMapping("/orders")
    @PreAuthorize("hasRole('BRANCH')")
    fun getBranchOrders(authentication: Authentication): ResponseEntity<List<SellerOrderResponse>> {
        val branchId = authentication.principal as Long
        return branchManagerService.getBranchOrders(branchId)
    }

    @Operation(
        summary = "Обновить статус заказа [BRANCH]",
        description = "Допустимые статусы: CONFIRMED, COOKING, READY_FOR_PICKUP, DELIVERED"
    )
    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('BRANCH')")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody request: OrderStatusRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val branchId = authentication.principal as Long
        return branchManagerService.updateOrderStatus(branchId, orderId, request.status)
    }
}
