package io.github.sh1iba.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RejectSellerRequest(
    @field:Schema(description = "Причина отклонения", example = "Не предоставлены необходимые документы")
    @field:NotBlank @field:Size(max = 500)
    val reason: String
)
