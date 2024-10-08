package com.martinlutern.smartcalculator.viewmodel

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.martinlutern.smartcalculator.BuildConfig
import com.martinlutern.smartcalculator.MainActivity
import com.martinlutern.smartcalculator.core.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : BaseViewModel() {
    val imageCalculation: MutableState<String> = mutableStateOf("")
    val imageResult: MutableState<String> = mutableStateOf("")
    val shouldShowCamera: MutableState<Boolean> = mutableStateOf(true)
    val imageUri: MutableState<Uri?> = mutableStateOf(null)
    var getContent: MainActivity.GetContentActivityResult? = null

    fun isCameraType(): Boolean {
        if (BuildConfig.FLAVOR === "built_in_camera") {
            return true
        }
        return false
    }

    fun isFilesystemType(): Boolean {
        if (BuildConfig.FLAVOR === "filesystem") {
            return true
        }
        return false
    }

    fun setImageResult(text: String) {
        imageResult.value = text
    }

    fun setImageCalculation(text: String) {
        imageCalculation.value = text
    }

    fun setImageUri(uri: Uri?) {
        imageUri.value = uri
    }

    fun resetImageResult() {
        imageResult.value = ""
    }

    fun resetImageCalculation() {
        imageCalculation.value = ""
    }

    fun hideCamera() {
        shouldShowCamera.value = false
    }

    fun showCamera() {
        shouldShowCamera.value = true
    }
}