package com.martinlutern.smartcalculator.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
abstract class BaseViewModel @Inject constructor() : ViewModel() {
    protected val job: Job by lazy { Job() }
    var retryFunction: (() -> Unit)? = null
    val shouldShowError: MutableState<Throwable> = mutableStateOf(Throwable(""))
    val shouldShowLoading: MutableState<Boolean> = mutableStateOf(false)

    override fun onCleared() {
        super.onCleared()
        retryFunction = null
        job.cancel()
    }

    open fun showLoading() {
        shouldShowLoading.value = true
    }

    open fun hideLoading() {
        shouldShowLoading.value = false
    }

    open fun handleError(e: Throwable) {
        shouldShowError.value = e
    }
}