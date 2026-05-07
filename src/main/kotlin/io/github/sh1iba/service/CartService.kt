package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.CartItemRequest
import io.github.sh1iba.dto.CartItemResponse
import io.github.sh1iba.dto.CartSummaryResponse
import io.github.sh1iba.dto.UpdateCartQuantityRequest
import io.github.sh1iba.entity.CartItem
import io.github.sh1iba.entity.InteractionType
import io.github.sh1iba.repository.CartItemRepository
import io.github.sh1iba.repository.ProductRepository
import io.github.sh1iba.repository.ProductVariantRepository
import io.github.sh1iba.repository.UserRepository

@Service
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val productService: ProductService
) {

    fun getCart(userId: Long): ResponseEntity<CartSummaryResponse> {
        val items = cartItemRepository.findAllByUserId(userId)

        val cartResponses = items.mapNotNull { item ->
            val product = item.product ?: return@mapNotNull null
            val price = getProductPrice(product.id, item.selectedSize)

            CartItemResponse(
                id = product.id,
                name = product.name,
                selectedSize = item.selectedSize,
                price = price,
                quantity = item.quantity,
                totalPrice = price * item.quantity,
                imageUrl = product.imageUrl
            )
        }

        return ResponseEntity.ok(
            CartSummaryResponse(
                items = cartResponses,
                totalItems = cartResponses.sumOf { it.quantity },
                totalPrice = cartResponses.sumOf { it.totalPrice.toDouble() }.toFloat()
            )
        )
    }

    @Transactional
    fun addToCart(userId: Long, request: CartItemRequest): ResponseEntity<Any> {
        userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))

        productRepository.findById(request.productId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Product not found"))

        if (!isValidVariant(request.productId, request.selectedSize)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Invalid variant for this product"))
        }

        val existing = cartItemRepository.findByUserIdAndProductIdAndSelectedSize(
            userId, request.productId, request.selectedSize
        )

        return if (existing != null) {
            existing.quantity += request.quantity
            cartItemRepository.save(existing)
            ResponseEntity.ok(mapOf("message" to "Cart item quantity updated"))
        } else {
            cartItemRepository.save(
                CartItem(
                    userId = userId,
                    productId = request.productId,
                    selectedSize = request.selectedSize,
                    quantity = request.quantity
                )
            )
            productService.logInteraction(userId, request.productId, InteractionType.CART)
            ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "Product added to cart"))
        }
    }

    @Transactional
    fun updateQuantity(userId: Long, productId: Int, selectedSize: String, request: UpdateCartQuantityRequest): ResponseEntity<Any> {
        if (request.quantity <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Quantity must be greater than 0"))
        }

        val item = cartItemRepository.findByUserIdAndProductIdAndSelectedSize(userId, productId, selectedSize)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Cart item not found"))

        item.quantity = request.quantity
        cartItemRepository.save(item)
        return ResponseEntity.ok(mapOf("message" to "Cart item quantity updated"))
    }

    @Transactional
    fun removeFromCart(userId: Long, productId: Int, selectedSize: String): ResponseEntity<Any> {
        if (!cartItemRepository.existsByUserIdAndProductIdAndSelectedSize(userId, productId, selectedSize)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Cart item not found"))
        }
        cartItemRepository.deleteByUserIdAndProductIdAndSelectedSize(userId, productId, selectedSize)
        return ResponseEntity.ok(mapOf("message" to "Cart item removed"))
    }

    @Transactional
    fun clearCart(userId: Long): ResponseEntity<Any> {
        cartItemRepository.deleteAllByUserId(userId)
        return ResponseEntity.ok(mapOf("message" to "Cart cleared"))
    }

    private fun isValidVariant(productId: Int, size: String): Boolean =
        productVariantRepository.findAllByProductId(productId).any { it.size == size }

    private fun getProductPrice(productId: Int, size: String): Float =
        productVariantRepository.findAllByProductId(productId).find { it.size == size }?.price?.toFloat() ?: 0f
}
