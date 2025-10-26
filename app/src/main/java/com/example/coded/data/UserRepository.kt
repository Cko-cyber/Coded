// UserRepository.kt
package com.example.coded.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    suspend fun createUser(user: User): Boolean {
        return try {
            val userData = user.copy(
                created_at = Timestamp.now(),
                updated_at = Timestamp.now()
            )
            usersCollection.document(user.id).set(userData).await()
            true
        } catch (e: Exception) {
            println("Error creating user: ${e.message}")
            false
        }
    }

    suspend fun updateUser(user: User): Boolean {
        return try {
            val updatedUser = user.copy(updated_at = Timestamp.now())
            usersCollection.document(user.id).set(updatedUser).await()
            true
        } catch (e: Exception) {
            println("Error updating user: ${e.message}")
            false
        }
    }

    suspend fun getUser(userId: String): User? {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error getting user: ${e.message}")
            null
        }
    }
}