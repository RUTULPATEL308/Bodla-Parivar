package com.bodla.parivar

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Simple auth helper to retrieve the current user's ID.
 * Replace with your actual auth logic if you use Supabase or another provider.
 */
object AuthProvider {
    /** Returns the UID of the signed‑in Firebase user, or throws if none. */
    fun currentUserId(context: Context): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        requireNotNull(uid) { "User not logged in – cannot upload FCM token." }
        return uid
    }
}
