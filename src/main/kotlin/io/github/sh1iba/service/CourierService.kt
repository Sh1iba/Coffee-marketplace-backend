package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.CourierResponse
import io.github.sh1iba.dto.OrderResponse
import io.github.sh1iba.entity.Courier
import io.github.sh1iba.entity.OrderStatus
import io.github.sh1iba.entity.Role
import io.github.sh1iba.repository.CourierRepository
import io.github.sh1iba.repository.OrderRepository
import io.github.sh1iba.repository.UserRepository

@Service
class CourierService(
    private val courierRepository: CourierRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val orderService: OrderService
) {

    @Transactional
    fun becomeCourier(userId: Long): ResponseEntity<Any> {
        if (courierRepository.existsByUserId(userId))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Вы уже зарегистрированы как курьер"))

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Пользователь не найден"))

        user.role = Role.COURIER
        userRepository.save(user)

        val courier = courierRepository.save(Courier(user = user))
        return ResponseEntity.status(HttpStatus.CREATED).body(courier.toResponse())
    }

    fun getMyProfile(userId: Long): ResponseEntity<Any> {
        val courier = courierRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Профиль курьера не найден"))
        return ResponseEntity.ok(courier.toResponse())
    }

    @Transactional
    fun updateAvailability(userId: Long, isAvailable: Boolean): ResponseEntity<Any> {
        val courier = courierRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Профиль курьера не найден"))
        courier.isAvailable = isAvailable
        courierRepository.save(courier)
        return ResponseEntity.ok(mapOf("message" to "Статус обновлён", "isAvailable" to isAvailable))
    }

    @Transactional
    fun updateLocation(userId: Long, latitude: Double, longitude: Double): ResponseEntity<Any> {
        val courier = courierRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Профиль курьера не найден"))
        courier.latitude = latitude
        courier.longitude = longitude
        courierRepository.save(courier)
        return ResponseEntity.ok(mapOf("message" to "Местоположение обновлено"))
    }

    fun getAvailableOrders(): ResponseEntity<List<OrderResponse>> =
        orderService.getAvailableOrdersForCourier()

    @Transactional
    fun takeOrder(userId: Long, orderId: Long): ResponseEntity<Any> {
        val courier = courierRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Профиль курьера не найден"))

        if (!courier.isAvailable)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Вы недоступны. Сначала смените статус на 'доступен'"))

        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Заказ не найден"))

        if (order.status != OrderStatus.READY_FOR_PICKUP)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Заказ недоступен для взятия (статус: ${order.status})"))

        if (order.courierId != null)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Заказ уже взят другим курьером"))

        order.courierId = courier.id
        order.status = OrderStatus.PICKED_UP
        courier.isAvailable = false
        orderRepository.save(order)
        courierRepository.save(courier)

        return ResponseEntity.ok(mapOf("message" to "Заказ взят", "orderId" to orderId, "status" to order.status))
    }

    @Transactional
    fun updateDeliveryStatus(userId: Long, orderId: Long, status: OrderStatus): ResponseEntity<Any> {
        val courier = courierRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Профиль курьера не найден"))

        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Заказ не найден"))

        if (order.courierId != courier.id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Этот заказ не ваш"))

        val allowed = setOf(OrderStatus.DELIVERING, OrderStatus.DELIVERED)
        if (status !in allowed)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Курьер может устанавливать только: DELIVERING, DELIVERED"))

        order.status = status
        orderRepository.save(order)

        if (status == OrderStatus.DELIVERED) {
            courier.isAvailable = true
            courierRepository.save(courier)
        }

        return ResponseEntity.ok(mapOf("message" to "Статус обновлён", "status" to status))
    }

    fun getCurrentOrder(userId: Long): ResponseEntity<Any> {
        val courier = courierRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Профиль курьера не найден"))

        val order = orderRepository.findByCourierIdAndStatusIn(
            courier.id,
            listOf(OrderStatus.PICKED_UP, OrderStatus.DELIVERING)
        ).firstOrNull()
            ?: return ResponseEntity.ok(mapOf("message" to "Нет активного заказа"))

        return ResponseEntity.ok(orderService.buildOrderResponse(order))
    }

    fun getAllCouriers(): List<CourierResponse> =
        courierRepository.findAll().map { it.toResponse() }

    @Transactional
    fun toggleCourierActive(courierId: Long): ResponseEntity<Any> {
        val courier = courierRepository.findById(courierId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Курьер не найден"))
        courier.isAvailable = !courier.isAvailable
        courierRepository.save(courier)
        return ResponseEntity.ok(mapOf("message" to "Статус курьера обновлён", "isAvailable" to courier.isAvailable))
    }

    private fun Courier.toResponse() = CourierResponse(
        id = id,
        userId = user.id,
        userName = user.name,
        userEmail = user.email,
        isAvailable = isAvailable,
        latitude = latitude,
        longitude = longitude
    )
}
