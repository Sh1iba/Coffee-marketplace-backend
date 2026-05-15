package io.github.sh1iba.service

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.github.sh1iba.dto.*
import io.github.sh1iba.entity.DeliveryType
import io.github.sh1iba.entity.Order
import io.github.sh1iba.entity.OrderItem
import io.github.sh1iba.entity.OrderStatus
import io.github.sh1iba.repository.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val userRepository: UserRepository,
    private val sellerRepository: SellerRepository,
    private val branchRepository: BranchRepository
) {

    @Transactional
    fun createOrder(userId: Long, request: OrderRequest): ResponseEntity<Any> {
        userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")

        if (request.deliveryType == DeliveryType.DELIVERY && request.deliveryAddress.isNullOrBlank())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Адрес доставки обязателен для заказа с доставкой")

        if (request.items.isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Не выбраны товары для заказа")

        val allCartItems = cartItemRepository.findAllByUserId(userId)
        if (allCartItems.isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Корзина пуста")

        val selectedCartItems = allCartItems.filter { cartItem ->
            request.items.any { it.productId == cartItem.productId && it.selectedSize == cartItem.selectedSize }
        }

        if (selectedCartItems.isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Выбранные товары не найдены в корзине")

        val order = orderRepository.save(
            Order(
                userId = userId,
                totalAmount = BigDecimal.ZERO,
                deliveryFee = request.deliveryFee,
                deliveryAddress = request.deliveryAddress,
                branchId = request.branchId,
                deliveryType = request.deliveryType,
                orderDate = LocalDateTime.now(),
                status = OrderStatus.PENDING
            )
        )

        var itemsAmount = BigDecimal.ZERO
        val orderItems = mutableListOf<OrderItem>()

        for (cartItem in selectedCartItems) {
            val product = cartItem.product ?: continue
            val variant = productVariantRepository.findAllByProductId(product.id)
                .find { it.size == cartItem.selectedSize } ?: continue

            val itemTotal = variant.price.multiply(BigDecimal.valueOf(cartItem.quantity.toLong()))
            itemsAmount = itemsAmount.add(itemTotal)

            orderItems.add(
                OrderItem(
                    order = order,
                    productName = product.name,
                    selectedSize = cartItem.selectedSize,
                    unitPrice = variant.price,
                    quantity = cartItem.quantity,
                    totalPrice = itemTotal,
                    sellerId = product.seller?.id,
                    productId = product.id
                )
            )

            cartItemRepository.deleteByUserIdAndProductIdAndSelectedSize(
                userId, cartItem.productId, cartItem.selectedSize
            )
        }

        order.totalAmount = itemsAmount.add(request.deliveryFee)
        orderRepository.save(order)
        orderItemRepository.saveAll(orderItems)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "message" to "Заказ создан",
                "orderId" to order.id,
                "totalAmount" to order.totalAmount,
                "deliveryFee" to request.deliveryFee,
                "itemsAmount" to itemsAmount,
                "deliveryType" to order.deliveryType,
                "status" to order.status
            )
        )
    }

    fun getOrderHistory(userId: Long): ResponseEntity<List<OrderResponse>> =
        ResponseEntity.ok(orderRepository.findAllByUserIdOrderByOrderDateDesc(userId).map { buildOrderResponse(it) })

    fun getOrderDetails(userId: Long, orderId: Long): ResponseEntity<Any> {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Заказ не найден")
        if (order.userId != userId)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Нет доступа")
        return ResponseEntity.ok(buildOrderResponse(order))
    }

    @Transactional
    fun cancelOrder(userId: Long, orderId: Long): ResponseEntity<Any> {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Заказ не найден"))
        if (order.userId != userId)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Нет доступа"))
        if (order.status == OrderStatus.CANCELLED)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to "Заказ уже отменён"))

        val cancellableStatuses = setOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)
        if (order.status !in cancellableStatuses)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Нельзя отменить заказ со статусом ${order.status}"))

        order.status = OrderStatus.CANCELLED
        orderRepository.save(order)
        return ResponseEntity.ok(mapOf("message" to "Заказ отменён"))
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, status: OrderStatus): ResponseEntity<Any> {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Заказ не найден"))
        order.status = status
        orderRepository.save(order)
        return ResponseEntity.ok(mapOf("message" to "Статус обновлён", "status" to status))
    }

    @Transactional
    fun updateSellerOrderStatus(userId: Long, orderId: Long, status: OrderStatus): ResponseEntity<Any> {
        val seller = sellerRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))

        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Заказ не найден"))

        val hasItems = orderItemRepository.findAllByOrderId(orderId).any { it.sellerId == seller.id }
        if (!hasItems)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("message" to "Этот заказ не содержит ваших товаров"))

        val allowedByStatus = setOf(OrderStatus.CONFIRMED, OrderStatus.COOKING, OrderStatus.READY_FOR_PICKUP)
        if (status !in allowedByStatus)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Ресторан может устанавливать только: CONFIRMED, COOKING, READY_FOR_PICKUP"))

        order.status = status
        orderRepository.save(order)
        return ResponseEntity.ok(mapOf("message" to "Статус обновлён", "status" to status))
    }

    fun getOrdersForSeller(userId: Long): ResponseEntity<List<SellerOrderResponse>> {
        val seller = sellerRepository.findByUserId(userId)
            ?: return ResponseEntity.ok(emptyList())

        val orderItems = orderItemRepository.findAllBySellerId(seller.id)
        val grouped = orderItems.groupBy { it.order.id }

        val result = grouped.map { (orderId, items) ->
            val order = items.first().order
            SellerOrderResponse(
                orderId = orderId,
                orderDate = order.orderDate,
                status = order.status,
                deliveryType = order.deliveryType.name,
                deliveryAddress = order.deliveryAddress,
                itemsTotal = items.sumOf { it.totalPrice },
                items = items.map {
                    OrderItemResponse(
                        id = it.id, productName = it.productName, selectedSize = it.selectedSize,
                        unitPrice = it.unitPrice, quantity = it.quantity, totalPrice = it.totalPrice
                    )
                }
            )
        }.sortedByDescending { it.orderDate }

        return ResponseEntity.ok(result)
    }

    fun getOrdersForBranch(branchId: Long): List<SellerOrderResponse> =
        orderRepository.findAllByBranchIdOrderByOrderDateDesc(branchId).map { order ->
            val items = orderItemRepository.findAllByOrderId(order.id)
            SellerOrderResponse(
                orderId = order.id,
                orderDate = order.orderDate,
                status = order.status,
                deliveryType = order.deliveryType.name,
                deliveryAddress = order.deliveryAddress,
                itemsTotal = items.sumOf { it.totalPrice },
                items = items.map {
                    OrderItemResponse(
                        id = it.id, productName = it.productName, selectedSize = it.selectedSize,
                        unitPrice = it.unitPrice, quantity = it.quantity, totalPrice = it.totalPrice
                    )
                }
            )
        }

    fun getAvailableOrdersForCourier(): ResponseEntity<List<OrderResponse>> =
        ResponseEntity.ok(
            orderRepository.findAllByStatusAndCourierIdIsNull(OrderStatus.READY_FOR_PICKUP)
                .map { buildOrderResponse(it) }
        )

    fun buildOrderResponse(order: Order): OrderResponse {
        val items = orderItemRepository.findAllByOrderId(order.id)
        val branchName = order.branchId?.let { branchRepository.findById(it).orElse(null)?.name }
        return OrderResponse(
            id = order.id,
            totalAmount = order.totalAmount,
            deliveryFee = order.deliveryFee,
            deliveryAddress = order.deliveryAddress,
            branchId = order.branchId,
            branchName = branchName,
            courierId = order.courierId,
            deliveryType = order.deliveryType,
            orderDate = order.orderDate,
            status = order.status,
            items = items.map {
                OrderItemResponse(
                    id = it.id, productName = it.productName, selectedSize = it.selectedSize,
                    unitPrice = it.unitPrice, quantity = it.quantity, totalPrice = it.totalPrice
                )
            }
        )
    }
}
