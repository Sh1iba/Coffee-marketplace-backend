package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.AdminUserResponse
import io.github.sh1iba.dto.BranchResponse
import io.github.sh1iba.dto.CourierResponse
import io.github.sh1iba.dto.ProductCategoryResponse
import io.github.sh1iba.dto.ProductResponse
import io.github.sh1iba.dto.ProductVariantResponse
import io.github.sh1iba.dto.SellerResponse
import io.github.sh1iba.entity.Branch
import io.github.sh1iba.entity.BranchStatus
import io.github.sh1iba.entity.Courier
import io.github.sh1iba.entity.Product
import io.github.sh1iba.entity.ProductStatus
import io.github.sh1iba.entity.Role
import io.github.sh1iba.entity.Seller
import io.github.sh1iba.entity.SellerStatus
import io.github.sh1iba.repository.BranchManagerRepository
import io.github.sh1iba.repository.BranchRepository
import io.github.sh1iba.repository.CourierRepository
import io.github.sh1iba.repository.ProductRepository
import io.github.sh1iba.repository.SellerRepository
import io.github.sh1iba.repository.UserRepository

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val sellerRepository: SellerRepository,
    private val courierRepository: CourierRepository,
    private val productRepository: ProductRepository,
    private val branchRepository: BranchRepository,
    private val branchManagerRepository: BranchManagerRepository
) {

    sealed class AdminResult {
        object Success : AdminResult()
        data class NotFound(val message: String) : AdminResult()
    }

    fun getAllUsers(): List<AdminUserResponse> =
        userRepository.findAll().map {
            AdminUserResponse(id = it.id, email = it.email, name = it.name, role = it.role)
        }

    fun changeUserRole(userId: Long, role: Role): AdminResult {
        val user = userRepository.findById(userId).orElse(null)
            ?: return AdminResult.NotFound("Пользователь не найден")
        user.role = role
        userRepository.save(user)
        return AdminResult.Success
    }

    fun getAllSellers(): List<SellerResponse> =
        sellerRepository.findAll().map { it.toSellerResponse() }

    fun getPendingSellers(): List<SellerResponse> =
        sellerRepository.findAllByStatus(SellerStatus.PENDING).map { it.toSellerResponse() }

    @Transactional
    fun approveSeller(sellerId: Long): AdminResult {
        val seller = sellerRepository.findById(sellerId).orElse(null)
            ?: return AdminResult.NotFound("Магазин не найден")
        seller.status = SellerStatus.APPROVED
        seller.rejectionReason = null
        sellerRepository.save(seller)
        return AdminResult.Success
    }

    @Transactional
    fun rejectSeller(sellerId: Long, reason: String): AdminResult {
        val seller = sellerRepository.findById(sellerId).orElse(null)
            ?: return AdminResult.NotFound("Магазин не найден")
        seller.status = SellerStatus.REJECTED
        seller.rejectionReason = reason
        sellerRepository.save(seller)
        return AdminResult.Success
    }

    fun activateSeller(sellerId: Long): AdminResult {
        val seller = sellerRepository.findById(sellerId).orElse(null)
            ?: return AdminResult.NotFound("Магазин не найден")
        seller.isActive = true
        sellerRepository.save(seller)
        return AdminResult.Success
    }

    fun deactivateSeller(sellerId: Long): AdminResult {
        val seller = sellerRepository.findById(sellerId).orElse(null)
            ?: return AdminResult.NotFound("Магазин не найден")
        seller.isActive = false
        sellerRepository.save(seller)
        return AdminResult.Success
    }

    fun getAllCouriers(): List<CourierResponse> =
        courierRepository.findAll().map { it.toCourierResponse() }

    fun toggleCourierAvailability(courierId: Long): AdminResult {
        val courier = courierRepository.findById(courierId).orElse(null)
            ?: return AdminResult.NotFound("Курьер не найден")
        courier.isAvailable = !courier.isAvailable
        courierRepository.save(courier)
        return AdminResult.Success
    }

    @Transactional
    fun removeCourierRole(courierId: Long): AdminResult {
        val courier = courierRepository.findById(courierId).orElse(null)
            ?: return AdminResult.NotFound("Курьер не найден")
        val user = courier.user
        user.role = Role.BUYER
        userRepository.save(user)
        courierRepository.delete(courier)
        return AdminResult.Success
    }

    // ── Модерация товаров ──────────────────────────────────────────────────

    fun getPendingProducts(): List<ProductResponse> =
        productRepository.findAllByStatus(ProductStatus.PENDING).map { it.toProductResponse() }

    fun getSellerProducts(sellerId: Long): List<ProductResponse> =
        productRepository.findAllBySellerId(sellerId).map { it.toProductResponse() }

    fun approveProduct(productId: Int): AdminResult {
        val product = productRepository.findById(productId).orElse(null)
            ?: return AdminResult.NotFound("Товар не найден")
        product.status = ProductStatus.APPROVED
        product.rejectionReason = null
        productRepository.save(product)
        return AdminResult.Success
    }

    fun rejectProduct(productId: Int, reason: String): AdminResult {
        val product = productRepository.findById(productId).orElse(null)
            ?: return AdminResult.NotFound("Товар не найден")
        product.status = ProductStatus.REJECTED
        product.rejectionReason = reason
        productRepository.save(product)
        return AdminResult.Success
    }

    fun deleteProduct(productId: Int): AdminResult {
        val product = productRepository.findById(productId).orElse(null)
            ?: return AdminResult.NotFound("Товар не найден")
        productRepository.delete(product)
        return AdminResult.Success
    }

    // ── Модерация филиалов ─────────────────────────────────────────────────

    fun getPendingBranches(): List<BranchResponse> =
        branchRepository.findAllByStatus(BranchStatus.PENDING).map { branch ->
            val managerEmail = branchManagerRepository.findAllByBranchId(branch.id).firstOrNull()?.user?.email
            branch.toBranchResponse(managerEmail)
        }

    @Transactional
    fun approveBranch(branchId: Long): AdminResult {
        val branch = branchRepository.findById(branchId).orElse(null)
            ?: return AdminResult.NotFound("Филиал не найден")
        branch.status = BranchStatus.APPROVED
        branch.rejectionReason = null
        branchRepository.save(branch)
        return AdminResult.Success
    }

    @Transactional
    fun rejectBranch(branchId: Long, reason: String): AdminResult {
        val branch = branchRepository.findById(branchId).orElse(null)
            ?: return AdminResult.NotFound("Филиал не найден")
        branch.status = BranchStatus.REJECTED
        branch.rejectionReason = reason
        branchRepository.save(branch)
        return AdminResult.Success
    }

    private fun Seller.toSellerResponse() = SellerResponse(
        id = id, name = name, description = description, category = category,
        logoUrl = logoUrl, rating = rating, isActive = isActive,
        phone = phone, website = website,
        ownerId = user.id, ownerName = user.name,
        status = status.name, rejectionReason = rejectionReason
    )

    private fun Courier.toCourierResponse() = CourierResponse(
        id = id, userId = user.id, userName = user.name, userEmail = user.email,
        isAvailable = isAvailable, latitude = latitude, longitude = longitude
    )

    private fun Branch.toBranchResponse(managerEmail: String?) = BranchResponse(
        id = id, sellerId = seller.id, sellerName = seller.name,
        name = name, address = address, city = city,
        latitude = latitude, longitude = longitude,
        deliveryFee = deliveryFee, minOrderAmount = minOrderAmount,
        workingHours = workingHours, isActive = isActive,
        managerEmail = managerEmail, status = status.name, rejectionReason = rejectionReason
    )

    private fun Product.toProductResponse() = ProductResponse(
        id = id,
        category = ProductCategoryResponse(id = category.id, type = category.type),
        name = name, description = description, imageUrl = imageUrl,
        variants = variants.map { ProductVariantResponse(size = it.size, price = it.price.toFloat(), volume = it.volume) },
        sellerId = seller?.id, sellerName = seller?.name,
        status = status.name, rejectionReason = rejectionReason
    )
}
