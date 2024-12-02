package com.example.farm2fork_sellers.utils.Data

data class ApiResponse(
    val status: String,
    val message: String,
    val user: User?
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)