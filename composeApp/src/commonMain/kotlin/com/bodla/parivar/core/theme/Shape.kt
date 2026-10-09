package com.bodla.parivar.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardCornerRadius = 16.dp
val ButtonCornerRadius = 12.dp
val IconContainerRadius = 14.dp
val DialogCornerRadius = 20.dp
val ChipCornerRadius = 8.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(ChipCornerRadius),
    medium = RoundedCornerShape(ButtonCornerRadius),
    large = RoundedCornerShape(CardCornerRadius),
    extraLarge = RoundedCornerShape(DialogCornerRadius)
)
