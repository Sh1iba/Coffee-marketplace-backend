package io.github.sh1iba.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import io.github.sh1iba.service.JwtService

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val jwt = authHeader.substring(7)
            val subject = jwtService.extractSubject(jwt)

            if (SecurityContextHolder.getContext().authentication == null) {
                if (subject.startsWith("branch:")) {
                    val branchId = subject.removePrefix("branch:").toLong()
                    if (jwtService.isTokenValid(jwt)) {
                        val authToken = UsernamePasswordAuthenticationToken(
                            branchId, null,
                            listOf(SimpleGrantedAuthority("ROLE_BRANCH"))
                        )
                        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authToken
                    }
                } else {
                    val userDetails = userDetailsService.loadUserByUsername(subject)
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        val authToken = UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.authorities
                        )
                        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authToken
                    }
                }
            }
        } catch (_: Exception) {}

        filterChain.doFilter(request, response)
    }
}