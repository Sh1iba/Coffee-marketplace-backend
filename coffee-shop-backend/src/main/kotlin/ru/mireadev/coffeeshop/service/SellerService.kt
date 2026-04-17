package ru.mireadev.coffeeshop.service

import org.springframework.stereotype.Service
import ru.mireadev.coffeeshop.dto.SellerRequest
import ru.mireadev.coffeeshop.dto.SellerResponse
import ru.mireadev.coffeeshop.entity.Seller
import ru.mireadev.coffeeshop.repository.SellerRepository
import ru.mireadev.coffeeshop.repository.UserRepository

@Service
class SellerService(
    private val sellerRepository: SellerRepository,
    private val userRepository: UserRepository
) {

    sealed class SellerResult {
        data class Success(val response: SellerResponse) : SellerResult()
        data class AlreadyExists(val message: String) : SellerResult()
        data class UserNotFound(val message: String) : SellerResult()
    }

    // Создать магазин (вызывается при первом входе продавца)
    fun createSeller(userId: Long, request: SellerRequest): SellerResult {
        if (sellerRepository.existsByUserId(userId)) {
            return SellerResult.AlreadyExists("Магазин для этого пользователя уже существует")
        }

        val user = userRepository.findById(userId).orElse(null)
            ?: return SellerResult.UserNotFound("Пользователь не найден")

        val seller = Seller(
            user = user,
            name = request.name,
            description = request.description,
            category = request.category,
            logoImage = request.logoImage
        )

        val saved = sellerRepository.save(seller)
        return SellerResult.Success(saved.toResponse())
    }

    // Получить свой магазин
    fun getMyShop(userId: Long): SellerResponse? {
        return sellerRepository.findByUserId(userId)?.toResponse()
    }

    // Обновить свой магазин
    fun updateMyShop(userId: Long, request: SellerRequest): SellerResponse? {
        val seller = sellerRepository.findByUserId(userId) ?: return null
        seller.name = request.name
        seller.description = request.description
        seller.category = request.category
        seller.logoImage = request.logoImage
        return sellerRepository.save(seller).toResponse()
    }

    // Получить все активные магазины (для покупателей — лента маркетплейса)
    fun getAllActiveSellers(): List<SellerResponse> {
        return sellerRepository.findAllByIsActiveTrue().map { it.toResponse() }
    }

    // Получить магазин по ID
    fun getSellerById(id: Long): SellerResponse? {
        return sellerRepository.findById(id).orElse(null)?.toResponse()
    }

    private fun Seller.toResponse() = SellerResponse(
        id = id,
        name = name,
        description = description,
        category = category,
        logoImage = logoImage,
        rating = rating,
        isActive = isActive,
        ownerId = user.id,
        ownerName = user.name
    )
}
