package io.github.sh1iba.service

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import io.github.sh1iba.dto.*
import io.github.sh1iba.entity.User
import io.github.sh1iba.repository.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsServiceImpl
) {

    sealed class RegistrationResult {
        data class Success(val response: RegisterResponse) : RegistrationResult()
        data class Conflict(val error: ErrorResponse) : RegistrationResult()
        data class ValidationError(val error: ErrorResponse) : RegistrationResult()
    }

    sealed class AuthResult {
        data class Success(val response: LoginResponse) : AuthResult()
        data class InvalidCredentials(val error: ErrorResponse) : AuthResult()
    }

    fun registerUser(request: RegisterRequest): RegistrationResult {
        if (userRepository.existsByEmail(request.email)) {
            return RegistrationResult.Conflict(
                ErrorResponse(success = false, message = "Email уже зарегистрирован", errorCode = "EMAIL_EXISTS")
            )
        }
        val role = if (request.role == io.github.sh1iba.entity.Role.ADMIN) io.github.sh1iba.entity.Role.BUYER else request.role
        val saved = userRepository.save(
            User(email = request.email, passwordHash = passwordEncoder.encode(request.password),
                name = request.name, role = role)
        )
        return RegistrationResult.Success(
            RegisterResponse(userID = saved.id, name = saved.name, email = saved.email, role = saved.role)
        )
    }

    fun authenticate(request: LoginRequest): AuthResult {
        val user = userRepository.findByEmail(request.email)
            ?: return AuthResult.InvalidCredentials(
                ErrorResponse(success = false, message = "Пользователь с такой почтой не найден", errorCode = "AUTH_FAILED")
            )
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            return AuthResult.InvalidCredentials(
                ErrorResponse(success = false, message = "Неверный пароль", errorCode = "AUTH_FAILED")
            )
        }
        val userDetails = userDetailsService.loadUserByUsername(user.email)
        return AuthResult.Success(
            LoginResponse(userId = user.id, token = "Bearer " + jwtService.generateToken(userDetails),
                name = user.name, email = user.email, role = user.role)
        )
    }

    fun getProfile(userId: Long): ResponseEntity<Any> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Пользователь не найден"))
        return ResponseEntity.ok(
            AdminUserResponse(id = user.id, email = user.email, name = user.name, role = user.role)
        )
    }

    fun updateProfile(userId: Long, request: UpdateProfileRequest): ResponseEntity<Any> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Пользователь не найден"))

        if (request.newPassword != null) {
            if (request.currentPassword == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("message" to "Для смены пароля укажите текущий пароль"))
            }
            if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("message" to "Неверный текущий пароль"))
            }
            user.passwordHash = passwordEncoder.encode(request.newPassword)
        }

        if (request.name != null) user.name = request.name
        userRepository.save(user)

        return ResponseEntity.ok(mapOf("message" to "Профиль обновлён"))
    }

    fun getUserIdFromAuthentication(authentication: Authentication): Long {
        val email = (authentication.principal as org.springframework.security.core.userdetails.User).username
        return userRepository.findByEmail(email)!!.id
    }
}
