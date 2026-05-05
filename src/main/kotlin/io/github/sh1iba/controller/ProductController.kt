package io.github.sh1iba.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.github.sh1iba.dto.*
import io.github.sh1iba.service.*

@RestController
@RequestMapping("/api/products")
@Tag(name = "Товары", description = "Каталог, избранное, корзина, заказы")
class ProductController(
    private val productService: ProductService,
    private val imageStorageService: ImageStorageService,
    private val favoriteProductService: FavoriteProductService,
    private val cartService: CartService,
    private val userService: UserService,
    private val orderService: OrderService
) {

    // ── Каталог ────────────────────────────────────────────────────────────

    @Operation(summary = "Все категории товаров")
    @GetMapping("/categories")
    fun getAllCategories(): ResponseEntity<List<ProductCategoryResponse>> =
        productService.getAllCategories()

    @Operation(
        summary = "Каталог товаров с фильтрацией и пагинацией",
        description = """
            Параметры:
            - page (default 0) — номер страницы
            - size (default 10) — размер страницы
            - categoryId — фильтр по категории
            - sellerId — фильтр по магазину
            - name — поиск по названию (частичное совпадение)
        """
    )
    @GetMapping
    fun getAllProducts(
        @RequestParam(required = false) categoryId: Int?,
        @RequestParam(required = false) sellerId: Long?,
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PagedResponse<ProductResponse>> =
        productService.getAllProducts(categoryId, sellerId, name, page, size)

    @Operation(summary = "Популярные товары за 30 дней (просмотры + заказы)")
    @GetMapping("/popular")
    fun getPopular(@RequestParam(defaultValue = "8") limit: Int): ResponseEntity<List<ProductResponse>> =
        productService.getPopular(limit)

    @Operation(summary = "Рекомендации для пользователя (по просмотрам и заказам)")
    @GetMapping("/recommended")
    fun getRecommended(
        authentication: Authentication,
        @RequestParam(defaultValue = "8") limit: Int
    ): ResponseEntity<List<ProductResponse>> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return productService.getRecommended(userId, limit)
    }

    @Operation(summary = "Логировать просмотр карточки товара")
    @PostMapping("/{productId}/view")
    fun logProductView(
        authentication: Authentication,
        @PathVariable productId: Int
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        productService.logInteraction(userId, productId, io.github.sh1iba.entity.InteractionType.VIEW)
        return ResponseEntity.ok(mapOf("message" to "ok"))
    }

    @Operation(summary = "Изображение товара")
    @GetMapping("/image/{imageName}")
    fun getImage(@PathVariable imageName: String): ResponseEntity<Resource> {
        val resource = imageStorageService.getImageResource(imageName)
        val contentType = when (imageName.substringAfterLast('.').lowercase()) {
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG
            "png" -> MediaType.IMAGE_PNG
            "gif" -> MediaType.IMAGE_GIF
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
        return ResponseEntity.ok().contentType(contentType).body(resource)
    }

    // ── Избранное ──────────────────────────────────────────────────────────

    @Operation(summary = "Получить избранное")
    @GetMapping("/favorites")
    fun getFavorites(authentication: Authentication): ResponseEntity<List<FavoriteProductResponse>> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return favoriteProductService.getFavorites(userId)
    }

    @Operation(summary = "Добавить товар в избранное")
    @PostMapping("/favorites")
    fun addToFavorites(
        authentication: Authentication,
        @RequestBody request: FavoriteProductRequest
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return favoriteProductService.addToFavorites(userId, request)
    }

    @Operation(summary = "Удалить товар из избранного (?size=L — конкретный вариант)")
    @DeleteMapping("/favorites/{productId}")
    fun removeFromFavorites(
        authentication: Authentication,
        @PathVariable productId: Int,
        @RequestParam(required = false) size: String?
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return favoriteProductService.removeFromFavorites(userId, productId, size)
    }

    // ── Корзина ────────────────────────────────────────────────────────────

    @Operation(summary = "Получить корзину")
    @GetMapping("/cart")
    fun getCart(authentication: Authentication): ResponseEntity<CartSummaryResponse> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return cartService.getCart(userId)
    }

    @Operation(summary = "Добавить товар в корзину")
    @PostMapping("/cart")
    fun addToCart(
        authentication: Authentication,
        @RequestBody request: CartItemRequest
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return cartService.addToCart(userId, request)
    }

    @Operation(summary = "Обновить количество в корзине")
    @PutMapping("/cart/{productId}/{selectedSize}")
    fun updateCartQuantity(
        authentication: Authentication,
        @PathVariable productId: Int,
        @PathVariable selectedSize: String,
        @RequestBody request: UpdateCartQuantityRequest
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return cartService.updateQuantity(userId, productId, selectedSize, request)
    }

    @Operation(summary = "Удалить товар из корзины")
    @DeleteMapping("/cart/{productId}/{selectedSize}")
    fun removeFromCart(
        authentication: Authentication,
        @PathVariable productId: Int,
        @PathVariable selectedSize: String
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return cartService.removeFromCart(userId, productId, selectedSize)
    }

    @Operation(summary = "Очистить корзину")
    @DeleteMapping("/cart")
    fun clearCart(authentication: Authentication): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return cartService.clearCart(userId)
    }

    // ── Заказы ─────────────────────────────────────────────────────────────

    @Operation(summary = "Оформить заказ")
    @PostMapping("/checkout")
    fun checkout(
        authentication: Authentication,
        @RequestBody request: OrderRequest
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return orderService.createOrder(userId, request)
    }

    @Operation(summary = "История заказов")
    @GetMapping("/orders/history")
    fun getOrderHistory(authentication: Authentication): ResponseEntity<List<OrderResponse>> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return orderService.getOrderHistory(userId)
    }

    @Operation(summary = "Детали заказа")
    @GetMapping("/orders/{orderId}")
    fun getOrderDetails(
        authentication: Authentication,
        @PathVariable orderId: Long
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return orderService.getOrderDetails(userId, orderId)
    }

    @Operation(summary = "Отменить заказ (только статус PENDING)")
    @PutMapping("/orders/{orderId}/cancel")
    fun cancelOrder(
        authentication: Authentication,
        @PathVariable orderId: Long
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return orderService.cancelOrder(userId, orderId)
    }
}
