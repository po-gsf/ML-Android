package `in`.surajsau.jisho.ui.styletransfer

import `in`.surajsau.jisho.base.SingleFlowViewModel
import `in`.surajsau.jisho.data.StyleTransferProvider
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class StyleTransferViewModelImpl @Inject constructor(
    private val styleTransferProvider: StyleTransferProvider,
) : ViewModel(), StyleTransferViewModel {

    private val _cameraImage = MutableStateFlow<String>("")
    private val _styleImage = MutableStateFlow<String>("")

    private val _styleTransferedImage = combine(
        _cameraImage.filter { it.isNotEmpty() },
        _styleImage.filter { it.isNotEmpty() },
        transform = { cameraImage, styleImage -> Pair(cameraImage, styleImage) }
    ).flatMapLatest { (targetPath, stylePath) ->
        styleTransferProvider.process(targetPath, stylePath)
    }

    override val state: StateFlow<StyleTransferViewModel.State>
        get() = _styleTransferedImage
            .map { StyleTransferViewModel.State(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, StyleTransferViewModel.State())

    override fun onEvent(event: StyleTransferViewModel.Event) {
        when(event) {
            is StyleTransferViewModel.Event.CameraResultReceived -> _cameraImage.value = event.fileName
            is StyleTransferViewModel.Event.StyleSelected -> _styleImage.value = event.fileName
        }
    }

}

interface StyleTransferViewModel: SingleFlowViewModel<StyleTransferViewModel.Event, StyleTransferViewModel.State> {

    sealed class Event {
        data class CameraResultReceived(val fileName: String): Event()
        data class StyleSelected(val fileName: String): Event()
    }

    data class State(
        val output: Bitmap? = null,
    )
}

val LocalStyleTransferViewModel = compositionLocalOf<StyleTransferViewModel> {
    error("StyleTransferViewModel not provided")
}

@Composable
fun provideStyleTransferViewModel(factory: @Composable () -> StyleTransferViewModel)
    = LocalStyleTransferViewModel provides factory.invoke()