package com.anicli.tv.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.anicli.tv.data.local.entity.FavoriteEntity
import com.anicli.tv.data.local.entity.WatchProgressEntity

@Singleton
class FirebaseRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous == true

    suspend fun signInAnonymously() {
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncFavoritesToCloud(favorites: List<FavoriteEntity>) {
        val uid = getCurrentUserId() ?: return
        if (isAnonymous()) return // Only sync if real user
        try {
            val batch = db.batch()
            val favRef = db.collection("users").document(uid).collection("favorites")
            
            for (fav in favorites) {
                val docRef = favRef.document(fav.mediaId)
                batch.set(docRef, fav)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeFavoriteFromCloud(favorite: FavoriteEntity) {
        val uid = getCurrentUserId() ?: return
        if (isAnonymous()) return // Only sync if real user
        try {
            val favRef = db.collection("users").document(uid).collection("favorites").document(favorite.mediaId)
            favRef.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncWatchProgressToCloud(progressList: List<WatchProgressEntity>) {
        val uid = getCurrentUserId() ?: return
        if (isAnonymous()) return // Only sync if real user
        try {
            val batch = db.batch()
            val progRef = db.collection("users").document(uid).collection("watch_progress")
            
            for (prog in progressList) {
                val docRef = progRef.document(prog.mediaId)
                batch.set(docRef, prog)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchCloudFavorites(): List<FavoriteEntity> {
        val uid = getCurrentUserId() ?: return emptyList()
        if (isAnonymous()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("favorites").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(FavoriteEntity::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchCloudWatchProgress(): List<WatchProgressEntity> {
        val uid = getCurrentUserId() ?: return emptyList()
        if (isAnonymous()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("watch_progress").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(WatchProgressEntity::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
