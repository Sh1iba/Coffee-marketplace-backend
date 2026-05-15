package io.github.sh1iba.service

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.BranchLoginRequest
import io.github.sh1iba.dto.BranchLoginResponse
import io.github.sh1iba.repository.BranchRepository

@Service
class BranchAuthService(
    private val branchRepository: BranchRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun login(request: BranchLoginRequest): ResponseEntity<Any> {
        val branch = branchRepository.findByEmail(request.email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Неверный email или пароль"))

        val hash = branch.passwordHash
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Для этого филиала не настроен пароль"))

        if (!passwordEncoder.matches(request.password, hash))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Неверный email или пароль"))

        val token = jwtService.generateBranchToken(branch.id)
        return ResponseEntity.ok(
            BranchLoginResponse(
                branchId = branch.id,
                branchName = branch.name,
                branchAddress = branch.address,
                branchCity = branch.city,
                token = token
            )
        )
    }
}
