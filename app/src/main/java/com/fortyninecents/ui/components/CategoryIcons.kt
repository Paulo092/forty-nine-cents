package com.fortyninecents.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import com.fortyninecents.domain.Category

val Category.icon: ImageVector
    get() = when (this) {
        Category.MERCADO -> Icons.Filled.ShoppingCart
        Category.ALIMENTACAO -> Icons.Filled.Restaurant
        Category.TRANSPORTE -> Icons.Filled.DirectionsCar
        Category.MORADIA -> Icons.Filled.Home
        Category.SAUDE -> Icons.Filled.LocalHospital
        Category.LAZER -> Icons.Filled.Movie
        Category.COMPRAS -> Icons.Filled.ShoppingBag
        Category.EDUCACAO -> Icons.Filled.School
        Category.SERVICOS -> Icons.Filled.LocalLaundryService
        Category.TRANSFERENCIA -> Icons.Filled.SwapHoriz
        Category.OUTROS -> Icons.Filled.MoreHoriz
        Category.SALARIO -> Icons.Filled.Payments
        Category.RECEITA -> Icons.Filled.Savings
    }
