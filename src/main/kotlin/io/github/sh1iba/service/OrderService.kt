package io.github.sh1iba.service

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.github.sh1iba.dto.*
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
    private val sellerRepository: SellerRepository
) {

    @Transactional
    fun createOrder(userId: Long, request: OrderRequest): ResponseEntity<Any> {
        userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")

        if (request.deliveryAddress.isBlank())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Delivery address is required")

        if (request.items.isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No items selected for order")

        val allCartItems = cartItemRepository.findAllByUserId(userId)
        if (allCartItems.isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cart is empty")

        val selectedCartItems = allCartItems.filter { cartItem ->
            request.items.any { it.productId == cartItem.productId && it.selectedSize == cartItem.selectedSize }
        }

        if (selectedCartItems.isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Selected items not found in cart")

        val order = orderRepository.save(
            Order(userId = userId, totalAmount = BigDecimal.ZERO, deliveryFee = request.deliveryFee,
                deliveryAddress = request.deliveryAddress, orderDate = LocalDateTime.now(),
                status = OrderStatus.PENDING)
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
                OrderItem(order = order, productName = product.name, selectedSize = cartItem.selectedSize,
                    unitPrice = variant.price, quantity = cartItem.quantity, totalPrice = itemTotal,
                    sellerId = product.seller?.id, productId = product.id)
            )

            cartItemRepository.deleteByUserIdAndProductIdAndSelectedSize(userId, cartItem.productId, cartItem.selectedSize)
        }

        order.totalAmount = itemsAmount.add(request.deliveryFee)
        orderRepository.save(order)
        orderItemRepository.saveAll(orderItems)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("message" to "Order created successfully", "orderId" to order.id,
                "totalAmount" to order.totalAmount, "deliveryFee" to request.deliveryFee,
                "itemsAmount" to itemsAmount, "status" to order.status)
        )
    }

    fun getOrderHistory(userId: Long): ResponseEntity<List<OrderResponse>> =
        ResponseEntity.ok(orderRepository.findAllByUserIdOrderByOrderDateDesc(userId).map { it.toResponse() })

    fun getOrderDetails(userId: Long, orderId: Long): ResponseEntity<Any> {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found")
        if (order.userId != userId)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied")
        return ResponseEntity.ok(order.toResponse())
    }

    @Transactional
    fun cancelOrder(userId: Long, orderId: Long): ResponseEntity<Any> {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Заказ не найден"))
        if (order.userId != userId)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Нет доступа"))
        if (order.status == OrderStatus.CANCELLED)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to "Заказ уже отменён"))
        if (order.status != OrderStatus.PENDING)
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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Этот заказ не содержит ваших товаров"))

        val allowedByStatus = setOf(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.READY, OrderStatus.DELIVERED)
        if (status !in allowedByStatus)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Нельзя установить статус $status"))

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
                deliveryAddress = order.deliveryAddress,
                itemsTotal = items.sumOf { it.totalPrice },
                items = items.map {
                    OrderItemResponse(id = it.id, productName = it.productName, selectedSize = it.selectedSize,
                        unitPrice = it.unitPrice, quantity = it.quantity, totalPrice = it.totalPrice)
                }
            )
        }.sortedByDescending { it.orderDate }

        return ResponseEntity.ok(result)
    }

    private fun Order.toResponse(): OrderResponse {
        val items = orderItemRepository.findAllByOrderId(id)
        return OrderResponse(
            id = id, totalAmount = totalAmount, deliveryFee = deliveryFee,
            deliveryAddress = deliveryAddress, orderDate = orderDate, status = status,
            items = items.map {
                OrderItemResponse(id = it.id, productName = it.productName, selectedSize = it.selectedSize,
                    unitPrice = it.unitPrice, quantity = it.quantity, totalPrice = it.totalPrice)
            }
        )
    }
}
