package io.github.sh1iba.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.github.sh1iba.dto.ReviewRequest
import io.github.sh1iba.dto.ReviewResponse
import io.github.sh1iba.service.ReviewService
import io.github.sh1iba.service.UserService

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Отзывы", description = "Создание и просмотр отзывов о магазинах")
class ReviewController(
    private val reviewService: ReviewService,
    private val userService: UserService
) {

    @Operation(summary = "Оставить отзыв (только после DELIVERED заказа)")
    @PostMapping
    fun createReview(
        @Valid @RequestBody request: ReviewRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return reviewService.createReview(userId, request)
    }

    @Operation(summary = "Отзывы о магазине (публично)")
    @GetMapping("/seller/{sellerId}")
    fun getSellerReviews(@PathVariable sellerId: Long): ResponseEntity<List<ReviewResponse>> =
        reviewService.getReviewsBySeller(sellerId)

    @Operation(summary = "Мои отзывы")
    @GetMapping("/my")
    fun getMyReviews(authentication: Authentication): ResponseEntity<List<ReviewResponse>> {
        val userId = userService.getUserIdFromAuthentication(authentication)
        return reviewService.getMyReviews(userId)
    }
}
