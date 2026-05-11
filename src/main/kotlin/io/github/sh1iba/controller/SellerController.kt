package io.github.sh1iba.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import io.github.sh1iba.dto.AssignManagerRequest
import io.github.sh1iba.dto.BranchRequest
import io.github.sh1iba.dto.BranchResponse
import io.github.sh1iba.dto.ProductManageRequest
import io.github.sh1iba.dto.OrderStatusRequest
import io.github.sh1iba.dto.SellerOrderResponse
import io.github.sh1iba.dto.SellerRequest
import io.github.sh1iba.dto.SellerResponse
import io.github.sh1iba.service.BranchManagerService
import io.github.sh1iba.service.BranchService
import io.github.sh1iba.service.ImageStorageService
import io.github.sh1iba.service.OrderService
import io.github.sh1iba.service.SellerService
import io.github.sh1iba.service.UserService

@RestController
@RequestMapping("/api/sellers")
@Tag(name = "Продавцы", description = "Управление магазином, товарами, заказами и филиалами")
class SellerController(
    private val sellerService: SellerService,
    private val branchService: BranchService,
    private val branchManagerService: BranchManagerService,
    private val userService: UserService,
    private val orderService: OrderService,
    private val imageStorageService: ImageStorageService
) {

    // ── Магазин ────────────────────────────────────────────────────────────

    @Operation(summary = "Стать продавцом — создаёт магазин и меняет роль BUYER → SELLER")
    @PostMapping("/become-seller")
    fun becomeSeller(
        @Valid @RequestBody request: SellerRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return when (val r = sellerService.becomeSeller(userId, request)) {
            is SellerService.BecomeSellerResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(r.response)
            is SellerService.BecomeSellerResult.AlreadyASeller ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to r.message))
            is SellerService.BecomeSellerResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
        }
    }

    @Operation(summary = "Создать магазин [SELLER]")
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    fun createSeller(
        @Valid @RequestBody request: SellerRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return when (val r = sellerService.createSeller(userId, request)) {
            is SellerService.SellerResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(r.response)
            is SellerService.SellerResult.AlreadyExists ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to r.message))
            is SellerService.SellerResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
        }
    }

    @Operation(summary = "Получить свой магазин [SELLER]")
    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    fun getMyShop(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return sellerService.getMyShop(userId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин ещё не создан"))
    }

    @Operation(summary = "Обновить свой магазин [SELLER]")
    @PutMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    fun updateMyShop(
        @Valid @RequestBody request: SellerRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return sellerService.updateMyShop(userId, request)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))
    }

    @Operation(summary = "Повторная заявка на модерацию [SELLER]")
    @PutMapping("/me/resubmit")
    @PreAuthorize("hasRole('SELLER')")
    fun resubmitSeller(
        @Valid @RequestBody request: SellerRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return sellerService.resubmitSeller(userId, request)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))
    }

    @Operation(summary = "Все активные магазины")
    @GetMapping
    fun getAllSellers(): ResponseEntity<List<SellerResponse>> =
        ResponseEntity.ok(sellerService.getAllActiveSellers())

    @Operation(summary = "Магазин по ID")
    @GetMapping("/{id}")
    fun getSellerById(@PathVariable id: Long): ResponseEntity<Any> =
        sellerService.getSellerById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))

    // ── Загрузка изображений ───────────────────────────────────────────────

    @Operation(
        summary = "Загрузить изображение товара [SELLER]",
        description = "Возвращает imageName для использования при создании/обновлении товара"
    )
    @PostMapping("/me/upload-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasRole('SELLER')")
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val url = imageStorageService.saveImage(file)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("imageUrl" to url))
    }

    // ── Товары ─────────────────────────────────────────────────────────────

    @Operation(summary = "Мои товары [SELLER]")
    @GetMapping("/me/products")
    @PreAuthorize("hasRole('SELLER')")
    fun getMyProducts(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return ResponseEntity.ok(sellerService.getMyProducts(userId))
    }

    @Operation(summary = "Добавить товар [SELLER]")
    @PostMapping("/me/products")
    @PreAuthorize("hasRole('SELLER')")
    fun createProduct(
        @Valid @RequestBody request: ProductManageRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return when (val r = sellerService.createProduct(userId, request)) {
            is SellerService.ProductResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(r.response)
            is SellerService.ProductResult.ShopNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
            is SellerService.ProductResult.CategoryNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
            is SellerService.ProductResult.ProductNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
            is SellerService.ProductResult.Forbidden ->
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to r.message))
        }
    }

    @Operation(summary = "Обновить товар [SELLER]")
    @PutMapping("/me/products/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    fun updateProduct(
        @PathVariable productId: Int,
        @Valid @RequestBody request: ProductManageRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return when (val r = sellerService.updateProduct(userId, productId, request)) {
            is SellerService.ProductResult.Success -> ResponseEntity.ok(r.response)
            is SellerService.ProductResult.ShopNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
            is SellerService.ProductResult.CategoryNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
            is SellerService.ProductResult.ProductNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
            is SellerService.ProductResult.Forbidden ->
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to r.message))
        }
    }

    @Operation(summary = "Удалить товар [SELLER]")
    @DeleteMapping("/me/products/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    fun deleteProduct(
        @PathVariable productId: Int,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return when (val r = sellerService.deleteProduct(userId, productId)) {
            is SellerService.DeleteResult.Success ->
                ResponseEntity.ok(mapOf("message" to "Товар удалён"))
            is SellerService.DeleteResult.ShopNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
            is SellerService.DeleteResult.ProductNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to r.message))
            is SellerService.DeleteResult.Forbidden ->
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to r.message))
        }
    }

    // ── Филиалы ────────────────────────────────────────────────────────────

    @Operation(summary = "Мои филиалы [SELLER]")
    @GetMapping("/me/branches")
    @PreAuthorize("hasRole('SELLER')")
    fun getMyBranches(authentication: Authentication): ResponseEntity<List<BranchResponse>> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchService.getMyBranches(userId)
    }

    @Operation(summary = "Создать филиал [SELLER]")
    @PostMapping("/me/branches")
    @PreAuthorize("hasRole('SELLER')")
    fun createBranch(
        @Valid @RequestBody request: BranchRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchService.createBranch(userId, request)
    }

    @Operation(summary = "Обновить филиал [SELLER]")
    @PutMapping("/me/branches/{branchId}")
    @PreAuthorize("hasRole('SELLER')")
    fun updateBranch(
        @PathVariable branchId: Long,
        @Valid @RequestBody request: BranchRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchService.updateBranch(userId, branchId, request)
    }

    @Operation(summary = "Активировать / деактивировать филиал [SELLER]")
    @PutMapping("/me/branches/{branchId}/toggle")
    @PreAuthorize("hasRole('SELLER')")
    fun toggleBranch(
        @PathVariable branchId: Long,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchService.toggleBranchActive(userId, branchId)
    }

    @Operation(summary = "Филиалы конкретного магазина (публично)")
    @GetMapping("/{id}/branches")
    fun getBranchesBySeller(@PathVariable id: Long): ResponseEntity<List<BranchResponse>> =
        branchService.getBranchesBySeller(id)

    // ── Менеджеры филиалов ─────────────────────────────────────────────────

    @Operation(summary = "Список менеджеров филиала [SELLER]")
    @GetMapping("/me/branches/{branchId}/managers")
    @PreAuthorize("hasRole('SELLER')")
    fun getBranchManagers(
        @PathVariable branchId: Long,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchManagerService.getBranchManagers(userId, branchId)
    }

    @Operation(
        summary = "Назначить менеджера филиала [SELLER]",
        description = "Передать email существующего пользователя — ему будет назначена роль BRANCH_MANAGER"
    )
    @PostMapping("/me/branches/{branchId}/managers")
    @PreAuthorize("hasRole('SELLER')")
    fun assignBranchManager(
        @PathVariable branchId: Long,
        @RequestBody request: AssignManagerRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchManagerService.assignManager(userId, branchId, request.email)
    }

    @Operation(summary = "Снять менеджера филиала [SELLER]")
    @DeleteMapping("/me/branches/{branchId}/managers/{managerId}")
    @PreAuthorize("hasRole('SELLER')")
    fun removeBranchManager(
        @PathVariable branchId: Long,
        @PathVariable managerId: Long,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return branchManagerService.removeManager(userId, managerId)
    }

    // ── Заказы продавца ────────────────────────────────────────────────────

    @Operation(summary = "Заказы с моими товарами [SELLER]")
    @GetMapping("/me/orders")
    @PreAuthorize("hasRole('SELLER')")
    fun getMyOrders(authentication: Authentication): ResponseEntity<List<SellerOrderResponse>> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return orderService.getOrdersForSeller(userId)
    }

    @Operation(summary = "Обновить статус заказа [SELLER]")
    @PutMapping("/me/orders/{orderId}/status")
    @PreAuthorize("hasRole('SELLER')")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody request: OrderStatusRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return orderService.updateSellerOrderStatus(userId, orderId, request.status)
    }
}
