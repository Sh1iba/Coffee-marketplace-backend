package io.github.sh1iba.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.PagedResponse
import io.github.sh1iba.dto.ProductCategoryResponse
import io.github.sh1iba.dto.ProductResponse
import io.github.sh1iba.dto.ProductVariantResponse
import io.github.sh1iba.entity.Product
import io.github.sh1iba.repository.ProductCategoryRepository
import io.github.sh1iba.repository.ProductRepository
import io.github.sh1iba.repository.ProductVariantRepository

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val productVariantRepository: ProductVariantRepository
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
