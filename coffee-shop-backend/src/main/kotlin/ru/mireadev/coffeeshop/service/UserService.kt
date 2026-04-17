package ru.mireadev.coffeeshop.service

import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import ru.mireadev.coffeeshop.dto.*
import ru.mireadev.coffeeshop.entity.User
import ru.mireadev.coffeeshop.repository.UserRepository

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

    fun registerUser(request: RegisterRequest): RegistrationResult {
        // Проверка уникальности
        if (userRepository.existsByEmail(request.email)) {
            return RegistrationResult.Conflict(
                ErrorResponse(
                    success = false,
                    message = "Email уже зарегистрирован",
                    errorCode = "EMAIL_EXISTS"
                )
            )
        }

        // Создание пользователя
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            name = request.name,
            role = request.role
        )

        val savedUser = userRepository.save(user)

        return RegistrationResult.Success(
            RegisterResponse(
                userID = savedUser.id,
                name = savedUser.name,
                email = savedUser.email,
                role = savedUser.role
            )
        )
    }

    sealed class AuthResult {
        data class Success(val response: LoginResponse) : AuthResult()
        data class InvalidCredentials(val error: ErrorResponse) : AuthResult()
    }

    fun authenticate(request: LoginRequest): AuthResult {
        val user = userRepository.findByEmail(request.email)
            ?: return AuthResult.InvalidCredentials(
                ErrorResponse(
                    success = false,
                    message = "Пользователь с такой почтой не найден",
                    errorCode = "AUTH_FAILED"
                )
            )

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            return AuthResult.InvalidCredentials(
                ErrorResponse(
                    success = false,
                    message = "Неверный пароль",
                    errorCode = "AUTH_FAILED"
                )
            )

        }
        val userDetails = userDetailsService.loadUserByUsername(user.email)
        return AuthResult.Success(
            LoginResponse(
                userId = user.id,
                token = "Bearer " + jwtService.generateToken(userDetails),
                name = user.name,
                email = user.email,
                role = user.role
            )
        )
    }

    fun getUserIdFromAuthentication(authentication: Authentication): Long {
        val userName = (authentication.principal as org.springframework.security.core.userdetails.User).username
        val user = userRepository.findByEmail(userName)
        return user!!.id.toLong()
    }
}