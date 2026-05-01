package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.FavoriteProductRequest
import io.github.sh1iba.dto.FavoriteProductResponse
import io.github.sh1iba.entity.FavoriteProduct
import io.github.sh1iba.repository.FavoriteProductRepository
import io.github.sh1iba.repository.ProductRepository
import io.github.sh1iba.repository.UserRepository

@Service
class FavoriteProductService(
    private val favoriteProductRepository: FavoriteProductRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) {

    fun getFavorites(userId: Long): ResponseEntity<List<FavoriteProductResponse>> {
        val favorites = favoriteProductRepository.findAllByUserId(userId)
        return ResponseEntity.ok(favorites.mapNotNull { fav ->
            val product = fav.product ?: return@mapNotNull null
            FavoriteProductResponse(id = product.id, selectedSize = fav.selectedSize)
        })
    }

    fun addToFavorites(userId: Long, request: FavoriteProductRequest): ResponseEntity<Any> {
        userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))

        val product = productRepository.findById(request.productId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Product not found"))

        if (product.variants.none { it.size == request.selectedSize }) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Invalid variant for this product"))
        }

        if (favoriteProductRepository.existsByUserIdAndProductIdAndSelectedSize(
                userId, request.productId, request.selectedSize)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Product with this variant already in favorites"))
        }

        favoriteProductRepository.save(
            FavoriteProduct(userId = userId, productId = request.productId, selectedSize = request.selectedSize)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "Product added to favorites"))
    }

    @Transactional
    fun removeFromFavorites(userId: Long, productId: Int, size: String?): ResponseEntity<Any> {
        if (size != null) {
            if (!favoriteProductRepository.existsByUserIdAndProductIdAndSelectedSize(userId, productId, size)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("message" to "Product with this variant not in favorites"))
            }
            favoriteProductRepository.deleteByUserIdAndProductIdAndSelectedSize(userId, productId, size)
        } else {
            if (favoriteProductRepository.findAllByUserIdAndProductId(userId, productId).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("message" to "Product not in favorites"))
            }
            favoriteProductRepository.deleteAllByUserIdAndProductId(userId, productId)
        }
        return ResponseEntity.ok(mapOf("message" to "Product removed from favorites"))
    }
}
