package com.mg.costeoapp.core.ui.viewmodel

sealed interface UiEvent {
    data object SaveSuccess : UiEvent
    data class ShowError(val message: String) : UiEvent
    data class ConfirmRemoval(val index: Int) : UiEvent
}
