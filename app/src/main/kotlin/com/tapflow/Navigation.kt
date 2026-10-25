package com.tapflow

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tapflow.ui.main.MainScreen
import com.tapflow.ui.main.MainScreenViewModel

@Composable
fun MainNavigation(container: AppContainer) {
    val viewModel: MainScreenViewModel = viewModel(
        factory = MainScreenViewModel.Factory(container)
    )
    MainScreen(viewModel = viewModel)
}
