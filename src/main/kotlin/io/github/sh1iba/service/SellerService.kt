package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.*
import io.github.sh1iba.entity.Product
import io.github.sh1iba.entity.ProductVariant
import io.github.sh1iba.entity.Seller
import io.github.sh1iba.repository.*

@Service
class SellerService(
    private val sellerRepository: SellerRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val productVariantRepository: ProductVariantRepository
) {

    // ── Результаты операций ────────────────────────────────────────────────

    sealed class SellerResult {
        data class Success(val response: SellerResponse) : SellerResult()
        data class AlreadyExists(val message: String) : SellerResult()
        data class UserNotFound(val message: String) : SellerResult()
    }

    sealed class ProductResult {
        data class Success(val response: ProductResponse) : ProductResult()
        data class ShopNotFound(val message: String) : ProductResult()
        data class CategoryNotFound(val message: String) : ProductResult()
        data class ProductNotFound(val message: String) : ProductResult()
        data class Forbidden(val message: String) : ProductResult()
    }

    sealed class DeleteResult {
        object Success : DeleteResult()
        data class ShopNotFound(val message: String) : DeleteResult()
        data class ProductNotFound(val message: String) : DeleteResult()
        data class Forbidden(val message: String) : DeleteResult()
    }

    // ── Управление магазином ───────────────────────────────────────────────

    fun createSeller(userId: Long, request: SellerRequest): SellerResult {
        if (sellerRepository.existsByUserId(userId)) {
            return SellerResult.AlreadyExists("Магазин для этого пользователя уже существует")
        }
        val user = userRepository.findById(userId).orElse(null)
            ?: return SellerResult.UserNotFound("Пользователь не найден")

        val saved = sellerRepository.save(
            Seller(user = user, name = request.name, description = request.description,
                category = request.category, logoImage = request.logoImage)
        )
        return SellerResult.Success(saved.toResponse())
    }

    fun getMyShop(userId: Long): SellerResponse? =
        sellerRepository.findByUserId(userId)?.toResponse()

    fun updateMyShop(userId: Long, request: SellerRequest): SellerResponse? {
        val seller = sellerRepository.findByUserId(userId) ?: return null
        seller.name = request.name
        seller.description = request.description
        seller.category = request.category
        seller.logoImage = request.logoImage
        return sellerRepository.save(seller).toResponse()
    }

    fun getAllActiveSellers(): List<SellerResponse> =
        sellerRepository.findAllByIsActiveTrue().map { it.toResponse() }

    fun getSellerById(id: Long): SellerResponse? =
        sellerRepository.findById(id).orElse(null)?.toResponse()

    // ── Управление товарами продавца ───────────────────────────────────────

    fun getMyProducts(userId: Long): List<ProductResponse> {
        val seller = sellerRepository.findByUserId(userId) ?: return emptyList()
        return productRepository.findAllBySellerId(seller.id).map { it.toProductResponse() }
    }

    @Transactional
    fun createProduct(userId: Long, request: ProductManageRequest): ProductResult {
        val seller = sellerRepository.findByUserId(userId)
            ?: return ProductResult.ShopNotFound("Магазин не найден. Сначала создайте магазин")

        val category = productCategoryRepository.findById(request.categoryId).orElse(null)
            ?: return ProductResult.CategoryNotFound("Категория с id=${request.categoryId} не найдена")

        val product = productRepository.save(
            Product(category = category, seller = seller, name = request.name,
                description = request.description, imageName = request.imageName)
        )

        val variants = productVariantRepository.saveAll(
            request.variants.map { ProductVariant(product = product, size = it.size, price = it.price) }
        )

        return ProductResult.Success(product.copy(variants = variants.toMutableList()).toProductResponse())
    }

    @Transactional
    fun updateProduct(userId: Long, productId: Int, request: ProductManageRequest): ProductResult {
        val seller = sellerRepository.findByUserId(userId)
            ?: return ProductResult.ShopNotFound("Магазин не найден")

        val product = productRepository.findById(productId).orElse(null)
            ?: return ProductResult.ProductNotFound("Товар не найден")

        if (product.seller?.id != seller.id) {
            return ProductResult.Forbidden("Этот товар принадлежит другому магазину")
        }

        val category = productCategoryRepository.findById(request.categoryId).orElse(null)
            ?: return ProductResult.CategoryNotFound("Категория с id=${request.categoryId} не найдена")

        product.name = request.name
        product.description = request.description
        product.imageName = request.imageName
        product.category = category
        productRepository.save(product)

        productVariantRepository.deleteAllByProductId(productId)
        val variants = productVariantRepository.saveAll(
            request.variants.map { ProductVariant(product = product, size = it.size, price = it.price) }
        )

        return ProductResult.Success(product.copy(variants = variants.toMutableList()).toProductResponse())
    }

    @Transactional
    fun deleteProduct(userId: Long, productId: Int): DeleteResult {
        val seller = sellerRepository.findByUserId(userId)
            ?: return DeleteResult.ShopNotFound("Магазин не найден")

        val product = productRepository.findById(productId).orElse(null)
            ?: return DeleteResult.ProductNotFound("Товар не найден")

        if (product.seller?.id != seller.id) {
            return DeleteResult.Forbidden("Этот товар принадлежит другому магазину")
        }

        productRepository.delete(product)
        return DeleteResult.Success
    }

    // ── Маппинг ───────────────────────────────────────────────────────────

    private fun Seller.toResponse() = SellerResponse(
        id = id, name = name, description = description, category = category,
        logoImage = logoImage, rating = rating, isActive = isActive,
        ownerId = user.id, ownerName = user.name
    )

    private fun Product.toProductResponse() = ProductResponse(
        id = id,
        category = ProductCategoryResponse(id = category.id, type = category.type),
        name = name, description = description, imageName = imageName,
        variants = variants.map { ProductVariantResponse(size = it.size, price = it.price.toFloat()) },
        sellerId = seller?.id, sellerName = seller?.name
    )
}
