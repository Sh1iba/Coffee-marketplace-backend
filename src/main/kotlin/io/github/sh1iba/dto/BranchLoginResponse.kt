package io.github.sh1iba.dto

data class BranchLoginResponse(
    val branchId: Long,
    val branchName: String,
    val branchAddress: String,
    val branchCity: String,
    val token: String
)
