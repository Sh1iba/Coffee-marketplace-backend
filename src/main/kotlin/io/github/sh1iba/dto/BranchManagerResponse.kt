package io.github.sh1iba.dto

data class BranchManagerResponse(
    val id: Long,
    val userId: Long,
    val userName: String,
    val userEmail: String,
    val branchId: Long,
    val branchName: String,
    val branchAddress: String,
    val branchCity: String
)
