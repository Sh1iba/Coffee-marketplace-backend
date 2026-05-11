package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.ReviewRequest
import io.github.sh1iba.dto.ReviewResponse
import io.github.sh1iba.entity.OrderStatus
import io.github.sh1iba.entity.Review
import io.github.sh1iba.repository.OrderItemRepository
import io.github.sh1iba.repository.OrderRepository
import io.github.sh1iba.repository.ReviewRepository
import io.github.sh1iba.repository.SellerRepository
import io.github.sh1iba.repository.UserRepository

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val sellerRepository: SellerRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createReview(userId: Long, request: ReviewRequest): ResponseEntity<Any> {
        val order = orderRepository.findById(request.orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Заказ не найден"))

        if (order.userId != userId)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Нет доступа к этому заказу"))

        if (order.status != OrderStatus.DELIVERED)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Отзыв можно оставить только после доставки заказа"))

        if (reviewRepository.existsByOrderId(request.orderId))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Отзыв на этот заказ уже оставлен"))

        val items = orderItemRepository.findAllByOrderId(request.orderId)
        val sellerId = items.mapNotNull { it.sellerId }.firstOrNull()
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Не удалось определить магазин для отзыва"))

        val seller = sellerRepository.findById(sellerId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))

        val review = reviewRepository.save(
            Review(userId = userId, seller = seller, orderId = request.orderId,
                rating = request.rating, comment = request.comment)
        )

        val avgRating = reviewRepository.avgRatingBySellerId(seller.id) ?: request.rating.toDouble()
        seller.rating = avgRating
        sellerRepository.save(seller)

        val user = userRepository.findById(userId).orElse(null)
        return ResponseEntity.status(HttpStatus.CREATED).body(review.toResponse(user?.name ?: "Аноним", seller.name))
    }

    @Transactional
    fun getReviewsBySeller(sellerId: Long): ResponseEntity<List<ReviewResponse>> {
        val seller = sellerRepository.findById(sellerId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(emptyList())
        return ResponseEntity.ok(
            reviewRepository.findAllBySellerId(sellerId).map { it.toResponse(getUserName(it.userId), seller.name) }
        )
    }

    @Transactional
    fun getMyReviews(userId: Long): ResponseEntity<List<ReviewResponse>> =
        ResponseEntity.ok(
            reviewRepository.findAllByUserId(userId).map {
                it.toResponse(getUserName(userId), it.seller.name)
            }
        )

    private fun getUserName(userId: Long): String =
        userRepository.findById(userId).orElse(null)?.name ?: "Аноним"

    private fun Review.toResponse(userName: String, sellerName: String) = ReviewResponse(
        id = id,
        userId = userId,
        userName = userName,
        sellerId = seller.id,
        sellerName = sellerName,
        orderId = orderId,
        rating = rating,
        comment = comment,
        createdAt = createdAt
    )
}
