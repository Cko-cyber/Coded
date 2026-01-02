package com.herdmat.coded.models

data class AnonymousClient(
    val transactionId: String,
    val deviceHash: String?,
    val riskScore: Int
)