package ru.mireadev.coffeeshop.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.JwtParser
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    private val secretKey: SecretKey,
    private val jwtParser: JwtParser
) {

    fun generateToken(userDetails: UserDetails): String {
        return Jwts.builder()
            .subject(userDetails.username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 604800000 )) // 7 дней
            .signWith(secretKey)
            .compact()
    }

    fun extractUsername(token: String): String {
        return jwtParser
            .parseSignedClaims(token)
            .payload
            .subject
    }

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return username == userDetails.username && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        return jwtParser
            .parseSignedClaims(token)
            .payload
            .expiration
            .before(Date())
    }
}