// File: app/src/main/java/com/example/coded/data/models/UserRole.kt
package com.example.coded.data.models

enum class UserRole {
    ADMIN,        // Platform administrators
    PROVIDER,     // Verified service providers
    CLIENT,       // Registered clients (optional, most will be anonymous)
    ANONYMOUS,    // Anonymous client sessions
    GUEST;        // Unauthenticated browsing (optional)

    companion object {
        fun fromString(value: String): UserRole {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: GUEST
        }

        fun hasAdminAccess(role: UserRole): Boolean = role == ADMIN

        fun canCreateJobs(role: UserRole): Boolean = role == CLIENT || role == ANONYMOUS

        fun canAcceptJobs(role: UserRole): Boolean = role == PROVIDER || role == ADMIN
    }
}