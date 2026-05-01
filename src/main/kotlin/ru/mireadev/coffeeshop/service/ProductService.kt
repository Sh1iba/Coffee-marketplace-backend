package ru.mireadev.coffeeshop.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import ru.mireadev.coffeeshop.dto.PagedResponse
import ru.mireadev.coffeeshop.dto.ProductCategoryResponse
import ru.mireadev.coffeeshop.dto.ProductResponse
import ru.mireadev.coffeeshop.dto.ProductVariantResponse
import ru.mireadev.coffeeshop.entity.Product
import ru.mireadev.coffeeshop.repository.ProductCategoryRepository
import ru.mireadev.coffeeshop.repository.ProductRepository
import ru.mireadev.coffeeshop.repository.ProductVariantRepository

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
        variants = variants.map { ProductVariantResponse(size = it.size, price = it.price.toFloat()) },
        sellerId = seller?.id,
        sellerName = seller?.name
    )
}
