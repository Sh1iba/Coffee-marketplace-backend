package io.github.sh1iba.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.PagedResponse
import io.github.sh1iba.dto.ProductCategoryResponse
import io.github.sh1iba.dto.ProductResponse
import io.github.sh1iba.dto.ProductVariantResponse
import io.github.sh1iba.entity.InteractionType
import io.github.sh1iba.entity.Product
import io.github.sh1iba.entity.UserProductInteraction
import io.github.sh1iba.repository.OrderItemRepository
import io.github.sh1iba.repository.ProductCategoryRepository
import io.github.sh1iba.repository.ProductRepository
import io.github.sh1iba.repository.ProductVariantRepository
import io.github.sh1iba.repository.UserProductInteractionRepository
import java.time.LocalDateTime

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val interactionRepository: UserProductInteractionRepository,
    private val orderItemRepository: OrderItemRepository
) {

    fun getAllCategories(): ResponseEntity<List<ProductCategoryResponse>> =
        ResponseEntity.ok(productCategoryRepository.findAll().map {
            ProductCategoryResponse(id = it.id, type = it.type)
        })

    fun getAllProducts(
        categoryId: Int? = null,
        sellerId: Long? = null,
        name: String? = null,
        page: Int = 0,
        size: Int = 10
    ): ResponseEntity<PagedResponse<ProductResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("id").descending())
        val result = productRepository.findWithFilters(categoryId, sellerId, name, pageable)
        return ResponseEntity.ok(
            PagedResponse(
                content = result.content.map { it.toResponse() },
                currentPage = result.number,
                pageSize = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                isLast = result.isLast
            )
        )
    }

    fun logInteraction(userId: Long, productId: Int, type: InteractionType) {
        // VIEW: дедупликация 5 минут; FAVORITE/CART: всегда логируем (явное действие)
        if (type == InteractionType.VIEW) {
            val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
            if (interactionRepository.existsByUserIdAndProductIdAndTypeAndInteractedAtAfter(
                    userId, productId, type, fiveMinutesAgo)) return
        }
        interactionRepository.save(UserProductInteraction(userId = userId, productId = productId, type = type))
    }

    fun getPopular(limit: Int): ResponseEntity<List<ProductResponse>> {
        val since = LocalDateTime.now().minusDays(30)
        val pageRequest = PageRequest.of(0, 50)

        // Топ по взвешенным взаимодействиям (VIEW×1, CART×2, FAVORITE×3)
        val interactionTopIds = interactionRepository.findTopProductIdsByScore(since, pageRequest)
        // + заказы (ORDER×5)
        val orderTopIds = orderItemRepository.findTopProductIdsByOrderCount(since, pageRequest)

        val interactionScores = interactionTopIds.mapIndexed { idx, id -> id to (50 - idx).toDouble() }.toMap()
        val orderScores = orderTopIds.mapIndexed { idx, id -> id to (50 - idx).toDouble() }.toMap()
        val allIds = (interactionScores.keys + orderScores.keys).distinct()

        val scored = allIds
            .map { id -> id to ((interactionScores[id] ?: 0.0) + (orderScores[id] ?: 0.0) * 1.5) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        var products = scored.mapNotNull { productRepository.findById(it).orElse(null) }

        if (products.size < limit) {
            val existingIds = products.map { it.id }.toSet()
            val filler = productRepository.findAll(Sort.by("id").descending())
                .filter { it.id !in existingIds }
                .take(limit - products.size)
            products = products + filler
        }

        return ResponseEntity.ok(products.map { it.toResponse() })
    }

    fun getRecommended(userId: Long, limit: Int): ResponseEntity<List<ProductResponse>> {
        val since = LocalDateTime.now().minusDays(30)

        val interactedIds = interactionRepository.findProductIdsByUserSince(userId, since)
        val orderedIds = orderItemRepository.findProductIdsByUserId(userId)
        val allInteractedIds = (interactedIds + orderedIds).distinct()

        if (allInteractedIds.isEmpty()) return getPopular(limit)

        val interactedProducts = allInteractedIds.mapNotNull { productRepository.findById(it).orElse(null) }
        val categoryIds = interactedProducts.map { it.category.id }.distinct()

        if (categoryIds.isEmpty()) return getPopular(limit)

        val excludeIds = interactedIds.ifEmpty { listOf(-1) }
        val recommended = productRepository.findByCategoryIdInAndIdNotIn(
            categoryIds, excludeIds, PageRequest.of(0, limit)
        )

        var result = recommended.content

        if (result.size < limit) {
            val existingIds = result.map { it.id }.toSet() + interactedIds.toSet()
            val filler = productRepository.findAll(Sort.by("id").descending())
                .filter { it.id !in existingIds }
                .take(limit - result.size)
            result = result + filler
        }

        return ResponseEntity.ok(result.map { it.toResponse() })
    }

    fun Product.toResponse() = ProductResponse(
        id = id,
        category = ProductCategoryResponse(id = category.id, type = category.type),
        name = name,
        description = description,
        imageName = imageName,
        variants = variants.map { ProductVariantResponse(size = it.size, price = it.price.toFloat(), volume = it.volume) },
        sellerId = seller?.id,
        sellerName = seller?.name
    )
}
