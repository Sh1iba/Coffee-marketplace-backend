package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.AdminUserResponse
import io.github.sh1iba.dto.CourierResponse
import io.github.sh1iba.dto.SellerResponse
import io.github.sh1iba.entity.Courier
import io.github.sh1iba.entity.Role
import io.github.sh1iba.entity.Seller
import io.github.sh1iba.entity.SellerStatus
import io.github.sh1iba.repository.CourierRepository
import io.github.sh1iba.repository.SellerRepository
import io.github.sh1iba.repository.UserRepository

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val sellerRepository: SellerRepository,
    private val courierRepository: CourierRepository
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
}
