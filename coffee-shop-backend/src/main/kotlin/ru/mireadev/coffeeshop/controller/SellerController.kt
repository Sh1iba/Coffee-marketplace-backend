package ru.mireadev.coffeeshop.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import ru.mireadev.coffeeshop.dto.SellerRequest
import ru.mireadev.coffeeshop.dto.SellerResponse
import ru.mireadev.coffeeshop.service.SellerService
import ru.mireadev.coffeeshop.service.UserService

@RestController
@RequestMapping("/api/sellers")
@Tag(name = "Продавцы", description = "Управление магазинами продавцов")
class SellerController(
    private val sellerService: SellerService,
    private val userService: UserService
) {

    @Operation(summary = "Создать магазин (только для SELLER)")
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    fun createSeller(
        @Valid @RequestBody request: SellerRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return when (val result = sellerService.createSeller(userId, request)) {
            is SellerService.SellerResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(result.response)
            is SellerService.SellerResult.AlreadyExists ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to result.message))
            is SellerService.SellerResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to result.message))
        }
    }

    @Operation(summary = "Получить свой магазин (только для SELLER)")
    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    fun getMyShop(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        val seller = sellerService.getMyShop(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "Магазин ещё не создан"))
        return ResponseEntity.ok(seller)
    }

    @Operation(summary = "Обновить свой магазин (только для SELLER)")
    @PutMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    fun updateMyShop(
        @Valid @RequestBody request: SellerRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        val seller = sellerService.updateMyShop(userId, request)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "Магазин не найден"))
        return ResponseEntity.ok(seller)
    }

    @Operation(summary = "Получить все активные магазины (для всех авторизованных)")
    @GetMapping
    fun getAllSellers(): ResponseEntity<List<SellerResponse>> {
        return ResponseEntity.ok(sellerService.getAllActiveSellers())
    }

    @Operation(summary = "Получить магазин по ID (для всех авторизованных)")
    @GetMapping("/{id}")
    fun getSellerById(@PathVariable id: Long): ResponseEntity<Any> {
        val seller = sellerService.getSellerById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "Магазин не найден"))
        return ResponseEntity.ok(seller)
    }
}
