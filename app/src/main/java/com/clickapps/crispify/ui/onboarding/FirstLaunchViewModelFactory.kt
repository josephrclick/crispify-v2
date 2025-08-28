package com.clickapps.crispify.ui.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clickapps.crispify.data.dataStore
import com.clickapps.crispify.engine.createModelInitializer

/**
 * Factory for creating FirstLaunchViewModel with proper dependencies
 */
class FirstLaunchViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FirstLaunchViewModel::class.java)) {
            val dataStore = context.dataStore
            val modelInitializer = createModelInitializer()
            
            return FirstLaunchViewModel(
                dataStore = dataStore,
                modelInitializer = modelInitializer
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}