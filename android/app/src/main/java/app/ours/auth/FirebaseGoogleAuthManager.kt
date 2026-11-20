package app.ours.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Auth Google Sign-In manager.
 *
 * Flow:
 *  1. [getGoogleIdToken] — Credential Manager shows the native Google account picker.
 *  2. [firebaseSignIn]   — exchanges the Google ID token for a Firebase ID token.
 *  3. The Firebase ID token (a JWT) is sent to the OURS server for session creation.
 *
 * Firebase must be initialised before use (requires google-services.json). Check
 * [isAvailable] before calling [signIn].
 */
object FirebaseGoogleAuthManager {

    /** Returns true if Firebase was initialised (google-services.json present). */
    fun isAvailable(context: Context): Boolean = try {
        !com.google.firebase.FirebaseApp.getApps(context).isEmpty()
    } catch (_: Exception) {
        false
    }

    /**
     * Shows the Google account picker and returns a Firebase ID token.
     *
     * @param webClientId The OAuth web client ID from Firebase console (SHA-1 registered).
     * @return [FirebaseResult.Authorized] with the Firebase ID token + user profile,
     *         or [FirebaseResult.Error] with a reason code.
     */
    suspend fun signIn(activity: Activity, webClientId: String): FirebaseResult =
        withContext(Dispatchers.Main) {
            try {
                // Step 1: Google account picker via Credential Manager
                val googleIdToken = getGoogleIdToken(activity, webClientId)
                    ?: return@withContext FirebaseResult.Error("no_credential")

                // Step 2: exchange for Firebase ID token
                val fbResult = firebaseSignIn(googleIdToken)
                return@withContext fbResult
            } catch (e: GetCredentialCancellationException) {
                FirebaseResult.Error("cancelled")
            } catch (e: Exception) {
                FirebaseResult.Error("exception:${e.message?.take(80) ?: "unknown"}")
            }
        }

    /** Shows the Google picker and returns the raw Google ID token (not Firebase). */
    private suspend fun getGoogleIdToken(activity: Activity, webClientId: String): String? =
        withContext(Dispatchers.Main) {
            val cm = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                // `false` lists every account on the device in the picker. Using
                // `true` instead makes GMS pre-mint tokens for ALL accounts just
                // to decide which are "authorized" — and if the device's accounts
                // are in a BadAuthentication hold, that shows an empty picker.
                // `false` at least surfaces the account list; token mint happens
                // on selection.
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = cm.getCredential(context = activity, request = request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                null
            }
        }

    /**
     * Exchanges a raw Google ID token (from any Google sign-in source: Credential
     * Manager, legacy GoogleSignInClient, or a web PKCE code exchange that yielded
     * an id_token) for a Firebase ID token + user profile.
     *
     * This is public so callers that obtain a Google ID token through a different
     * code path than [getGoogleIdToken] (e.g. legacy GoogleSignInClient.signInIntent)
     * can still exchange it for a Firebase token without re-triggering the native
     * account picker.
     */
    suspend fun firebaseSignIn(googleIdToken: String): FirebaseResult =
        suspendCancellableCoroutine { cont ->
            val auth = FirebaseAuth.getInstance()
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        cont.resume(FirebaseResult.Error("null_user"))
                    } else {
                        user.getIdToken(true).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val idToken = task.result?.token ?: ""
                                if (idToken.isBlank()) {
                                    cont.resume(FirebaseResult.Error("empty_token"))
                                } else {
                                    cont.resume(
                                        FirebaseResult.Authorized(
                                            idToken = idToken,
                                            email = user.email ?: "",
                                            displayName = user.displayName ?: "",
                                            photoUrl = user.photoUrl?.toString() ?: "",
                                            firebaseUid = user.uid,
                                        )
                                    )
                                }
                            } else {
                                cont.resume(FirebaseResult.Error("token_failure"))
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    cont.resume(FirebaseResult.Error("firebase_error:${e.message?.take(80)}"))
                }
        }

    /** Launches sign-out to clear the Firebase session. */
    fun signOut() {
        try { FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}
    }
}

sealed class FirebaseResult {
    data class Authorized(
        val idToken: String,
        val email: String,
        val displayName: String,
        val photoUrl: String,
        val firebaseUid: String,
    ) : FirebaseResult()

    data class Error(val reason: String) : FirebaseResult()
}
