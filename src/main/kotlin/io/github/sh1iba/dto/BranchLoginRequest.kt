package io.github.sh1iba.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class BranchLoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String
)
