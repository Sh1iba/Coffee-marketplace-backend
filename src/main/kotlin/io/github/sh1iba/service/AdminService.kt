package io.github.sh1iba.service

import org.springframework.stereotype.Service
import io.github.sh1iba.dto.AdminUserResponse
import io.github.sh1iba.dto.SellerResponse
import io.github.sh1iba.entity.Role
import io.github.sh1iba.entity.Seller
import io.github.sh1iba.repository.SellerRepository
import io.github.sh1iba.repository.UserRepository

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val sellerRepository: SellerRepository
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
        sellerRepository.findAll().map { it.toResponse() }

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

    private fun Seller.toResponse() = SellerResponse(
        id = id, name = name, description = description, category = category,
        logoImage = logoImage, rating = rating, isActive = isActive,
        ownerId = user.id, ownerName = user.name
    )
}
