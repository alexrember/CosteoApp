package com.mg.costeoapp.core.ui.viewmodel

sealed interface UiEvent {
    data object SaveSuccess : UiEvent
    data class SaveSuccessWithId(val id: Long) : UiEvent
    data class ShowError(val message: String) : UiEvent
    data class ShowMessage(val message: String) : UiEvent
    data class ConfirmRemoval(val index: Int) : UiEvent
    data object NavigateBack : UiEvent
}
