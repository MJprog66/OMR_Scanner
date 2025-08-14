package thesis.project.omrscanner.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore instance
private val Context.dataStore by preferencesDataStore("user_prefs")

object AuthDataStore {

    private val IS_ADMIN = booleanPreferencesKey("is_admin")
    private val USER_EMAIL = stringPreferencesKey("user_email")

    /** Save admin login state */
    suspend fun saveAdminLogin(context: Context, loggedIn: Boolean) {
        context.dataStore.edit { it[IS_ADMIN] = loggedIn }
    }

    /** Check if admin is logged in */
    fun isAdminLoggedIn(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[IS_ADMIN] ?: false }

    /** Save user login */
    suspend fun saveUserLogin(context: Context, email: String, isAdmin: Boolean) {
        context.dataStore.edit {
            it[USER_EMAIL] = email
            it[IS_ADMIN] = isAdmin
        }
    }

    /** Get saved user email */
    fun getUserEmail(context: Context): Flow<String?> =
        context.dataStore.data.map { it[USER_EMAIL] }

    /** Clear user login (logout) */
    suspend fun clearUserLogin(context: Context) {
        context.dataStore.edit {
            it[USER_EMAIL] = ""
            it[IS_ADMIN] = false
        }
    }
}
