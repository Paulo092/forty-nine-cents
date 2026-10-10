package com.fortyninecents.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fortyninecents.FortyNineCentsApp
import com.fortyninecents.di.AppContainer
import com.fortyninecents.ui.aliases.AliasesViewModel
import com.fortyninecents.ui.home.HomeViewModel
import com.fortyninecents.ui.review.ReviewViewModel
import com.fortyninecents.ui.transactions.TransactionsViewModel

/** Fábrica única dos ViewModels, alimentada pelo [AppContainer]. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { HomeViewModel(container().repository, container().classifier) }
        initializer { TransactionsViewModel(container().repository, container().csvExporter) }
        initializer { AliasesViewModel(container().aliases) }
        initializer { ReviewViewModel(container().repository, container().notificationProcessor, container().settings) }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as FortyNineCentsApp).container
}
