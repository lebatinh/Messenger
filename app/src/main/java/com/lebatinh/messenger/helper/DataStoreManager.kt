package com.lebatinh.messenger.helper

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.lebatinh.messenger.Key_Password.NAME_DATASTORE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = NAME_DATASTORE)

object DataStoreManager {

    /**
     * Lưu dữ liệu vào DataStore
     * @param key : khóa của giá trị cần lưu
     * @param value : giá trị lưu
     */
    suspend fun <T> setData(context: Context, key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Đọc dữ liệu từ DataStore
     * @param key : khóa của giá trị cần lấy
     * @param defaultValue : giá trị mặc định
     * trả về giá trị cuối qua Flow
     */
    fun <T> getData(context: Context, key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    /**
     * Xóa dữ liệu khỏi DataStore
     * @param key : khóa của giá trị cần xóa
     */
    suspend fun <T> removeData(context: Context, key: Preferences.Key<T>) {
        context.dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
