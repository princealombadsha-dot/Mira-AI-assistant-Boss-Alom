package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.example.R
import com.example.data.model.UserMemoryItem
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = try {
        val databaseId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    } catch (e: Exception) {
        FirebaseFirestore.getInstance()
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    suspend fun signInWithGoogle(): Boolean {
        return try {
            val credentialManager = CredentialManager.create(context)
            val serverClientId = context.getString(R.string.default_web_client_id)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                auth.signInWithCredential(authCredential).await()
                _authError.value = null
                true
            } else {
                _authError.value = "গুগল সাইন-ইন সম্পন্ন হয়নি।"
                false
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Google Sign-In failed", e)
            _authError.value = e.localizedMessage ?: "সাইন-ইনে ত্রুটি হয়েছে"
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun syncMemoryToCloud(memory: UserMemoryItem) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val data = mapOf(
                "key" to memory.key,
                "value" to memory.value,
                "category" to "PREFERENCE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("memories").document(memory.id)
                .set(data)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to sync memory", e)
        }
    }

    suspend fun fetchCloudMemories(): List<UserMemoryItem> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("memories")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val key = doc.getString("key") ?: return@mapNotNull null
                val value = doc.getString("value") ?: return@mapNotNull null
                UserMemoryItem(id = doc.id, key = key, value = value)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun recordCloudTask(query: String, status: String, resultSummary: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val data = mapOf(
                "query" to query,
                "status" to status,
                "resultSummary" to resultSummary.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("tasks")
                .add(data)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to record task", e)
        }
    }
}
