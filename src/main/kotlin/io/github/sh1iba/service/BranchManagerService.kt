package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.BranchManagerResponse
import io.github.sh1iba.dto.SellerOrderResponse
import io.github.sh1iba.entity.BranchManager
import io.github.sh1iba.entity.OrderStatus
import io.github.sh1iba.entity.Role
import io.github.sh1iba.repository.BranchManagerRepository
import io.github.sh1iba.repository.BranchRepository
import io.github.sh1iba.repository.OrderRepository
import io.github.sh1iba.repository.SellerRepository
import io.github.sh1iba.repository.UserRepository

@Service
class BranchManagerService(
    private val branchManagerRepository: BranchManagerRepository,
    private val branchRepository: BranchRepository,
    private val sellerRepository: SellerRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val orderService: OrderService
) {

    @Transactional
    fun assignManager(sellerUserId: Long, branchId: Long, email: String): ResponseEntity<Any> {
        val seller = sellerRepository.findByUserId(sellerUserId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))
        val branch = branchRepository.findById(branchId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Филиал не найден"))
        if (branch.seller.id != seller.id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Нет доступа к этому филиалу"))
        val user = userRepository.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Пользователь с email '$email' не найден"))
        if (branchManagerRepository.existsByUserId(user.id))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Этот пользователь уже является менеджером филиала"))

        user.role = Role.BRANCH_MANAGER
        userRepository.save(user)
        val manager = branchManagerRepository.save(BranchManager(user = user, branch = branch))
        return ResponseEntity.status(HttpStatus.CREATED).body(manager.toResponse())
    }

    @Transactional
    fun removeManager(sellerUserId: Long, managerId: Long): ResponseEntity<Any> {
        val seller = sellerRepository.findByUserId(sellerUserId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))
        val manager = branchManagerRepository.findById(managerId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Менеджер не найден"))
        if (manager.branch.seller.id != seller.id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Нет доступа"))

        val user = manager.user
        user.role = Role.BUYER
        userRepository.save(user)
        branchManagerRepository.delete(manager)
        return ResponseEntity.ok(mapOf("message" to "Менеджер удалён, роль → BUYER"))
    }

    @Transactional
    fun getBranchManagers(sellerUserId: Long, branchId: Long): ResponseEntity<Any> {
        val seller = sellerRepository.findByUserId(sellerUserId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))
        val branch = branchRepository.findById(branchId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Филиал не найден"))
        if (branch.seller.id != seller.id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Нет доступа к этому филиалу"))
        return ResponseEntity.ok(branchManagerRepository.findAllByBranchId(branchId).map { it.toResponse() })
    }

    @Transactional
    fun getMyProfile(userId: Long): ResponseEntity<Any> {
        val manager = branchManagerRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Профиль менеджера не найден"))
        return ResponseEntity.ok(manager.toResponse())
    }

    @Transactional
    fun getBranchOrders(userId: Long): ResponseEntity<List<SellerOrderResponse>> {
        val manager = branchManagerRepository.findByUserId(userId)
            ?: return ResponseEntity.ok(emptyList())
        return ResponseEntity.ok(orderService.getOrdersForBranch(manager.branch.id))
    }

    @Transactional
    fun updateOrderStatus(userId: Long, orderId: Long, status: OrderStatus): ResponseEntity<Any> {
        val manager = branchManagerRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Профиль менеджера не найден"))
        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Заказ не найден"))
        if (order.branchId != manager.branch.id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("message" to "Этот заказ не принадлежит вашему филиалу"))

        val allowed = setOf(OrderStatus.CONFIRMED, OrderStatus.COOKING, OrderStatus.READY_FOR_PICKUP)
        if (status !in allowed)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Менеджер может устанавливать только: CONFIRMED, COOKING, READY_FOR_PICKUP"))

        order.status = status
        orderRepository.save(order)
        return ResponseEntity.ok(mapOf("message" to "Статус обновлён", "status" to status))
    }

    private fun BranchManager.toResponse() = BranchManagerResponse(
        id = id,
        userId = user.id,
        userName = user.name,
        userEmail = user.email,
        branchId = branch.id,
        branchName = branch.name,
        branchAddress = branch.address,
        branchCity = branch.city
    )
}
