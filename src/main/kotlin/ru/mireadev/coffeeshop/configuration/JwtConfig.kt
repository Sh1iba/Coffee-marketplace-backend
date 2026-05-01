package ru.mireadev.coffeeshop.configuration

import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.crypto.SecretKey

@Configuration
class JwtConfig {

    @Value("\${jwt.secret}")
    private lateinit var secretString: String

    @Bean
    fun secretKey(): SecretKey {
        return Keys.hmacShaKeyFor(secretString.toByteArray())
    }

    @Bean
    fun jwtParser(secretKey: SecretKey): JwtParser {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
    }
}