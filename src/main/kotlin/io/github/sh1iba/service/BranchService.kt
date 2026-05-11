package io.github.sh1iba.service

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.BranchRequest
import io.github.sh1iba.dto.BranchResponse
import io.github.sh1iba.entity.Branch
import io.github.sh1iba.entity.BranchManager
import io.github.sh1iba.entity.Role
import io.github.sh1iba.entity.User
import io.github.sh1iba.repository.BranchManagerRepository
import io.github.sh1iba.repository.BranchRepository
import io.github.sh1iba.repository.SellerRepository
import io.github.sh1iba.repository.UserRepository

@Service
class BranchService(
    private val branchRepository: BranchRepository,
    private val sellerRepository: SellerRepository,
    private val userRepository: UserRepository,
    private val branchManagerRepository: BranchManagerRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun createBranch(userId: Long, request: BranchRequest): ResponseEntity<Any> {
        val seller = sellerRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "Магазин не найден. Сначала создайте магазин."))

        if (request.managerEmail.isNullOrBlank() || request.managerPassword.isNullOrBlank())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Укажите email и пароль для менеджера филиала"))

        if (userRepository.findByEmail(request.managerEmail) != null)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "Пользователь с email ${request.managerEmail} уже существует"))

        val branch = branchRepository.save(
            Branch(
                seller = seller,
                name = request.name,
                address = request.address,
                city = request.city,
                latitude = request.latitude,
                longitude = request.longitude,
                deliveryFee = request.deliveryFee,
                minOrderAmount = request.minOrderAmount,
                workingHours = request.workingHours
            )
        )

        val managerUser = userRepository.save(
            User(
                email = request.managerEmail!!,
                name = "${request.name} (менеджер)",
                passwordHash = passwordEncoder.encode(request.managerPassword!!),
                role = Role.BRANCH_MANAGER
            )
        )
        branchManagerRepository.save(BranchManager(user = managerUser, branch = branch))

        return ResponseEntity.status(HttpStatus.CREATED).body(branch.toResponse(managerUser.email))
    }

    fun getMyBranches(userId: Long): ResponseEntity<List<BranchResponse>> {
        val seller = sellerRepository.findByUserId(userId)
            ?: return ResponseEntity.ok(emptyList())
        val branches = branchRepository.findAllBySellerId(seller.id)
        return ResponseEntity.ok(branches.map { branch ->
            val managerEmail = branchManagerRepository.findAllByBranchId(branch.id)
                .firstOrNull()?.user?.email
            branch.toResponse(managerEmail)
        })
    }

    @Transactional
    fun updateBranch(userId: Long, branchId: Long, request: BranchRequest): ResponseEntity<Any> {
        val seller = sellerRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))
        val branch = branchRepository.findById(branchId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Филиал не найден"))
        if (branch.seller.id != seller.id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Нет доступа к этому филиалу"))

        branch.name = request.name
        branch.address = request.address
        branch.city = request.city
        branch.latitude = request.latitude
        branch.longitude = request.longitude
        branch.deliveryFee = request.deliveryFee
        branch.minOrderAmount = request.minOrderAmount
        branch.workingHours = request.workingHours

        val managerEmail = branchManagerRepository.findAllByBranchId(branchId)
            .firstOrNull()?.user?.email
        return ResponseEntity.ok(branchRepository.save(branch).toResponse(managerEmail))
    }

    @Transactional
    fun toggleBranchActive(userId: Long, branchId: Long): ResponseEntity<Any> {
        val seller = sellerRepository.findByUserId(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Магазин не найден"))
        val branch = branchRepository.findById(branchId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Филиал не найден"))
        if (branch.seller.id != seller.id)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Нет доступа к этому филиалу"))

        branch.isActive = !branch.isActive
        branchRepository.save(branch)
        val managerEmail = branchManagerRepository.findAllByBranchId(branchId).firstOrNull()?.user?.email
        return ResponseEntity.ok(branch.toResponse(managerEmail))
    }

    @Transactional
    fun getBranchesBySeller(sellerId: Long): ResponseEntity<List<BranchResponse>> =
        ResponseEntity.ok(
            branchRepository.findAllBySellerIdAndIsActiveTrue(sellerId).map { it.toResponse(null) }
        )

    @Transactional
    fun getBranchesByCity(city: String): ResponseEntity<List<BranchResponse>> =
        ResponseEntity.ok(
            branchRepository.findAllByCityIgnoreCaseAndIsActiveTrue(city).map { it.toResponse(null) }
        )

    @Transactional
    fun getAllActiveBranches(): ResponseEntity<List<BranchResponse>> =
        ResponseEntity.ok(branchRepository.findAllByIsActiveTrue().map { it.toResponse(null) })

    @Transactional
    fun getBranchById(branchId: Long): BranchResponse? =
        branchRepository.findById(branchId).orElse(null)?.toResponse(null)

    private fun Branch.toResponse(managerEmail: String?) = BranchResponse(
        id = id,
        sellerId = seller.id,
        sellerName = seller.name,
        name = name,
        address = address,
        city = city,
        latitude = latitude,
        longitude = longitude,
        deliveryFee = deliveryFee,
        minOrderAmount = minOrderAmount,
        workingHours = workingHours,
        isActive = isActive,
        managerEmail = managerEmail
    )
}
