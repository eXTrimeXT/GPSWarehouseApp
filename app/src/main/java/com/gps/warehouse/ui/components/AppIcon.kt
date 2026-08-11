package com.gps.warehouse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.gps.warehouse.R

@Composable
fun AppIconDisplay() {
    Image(
        painter = painterResource(id = R.mipmap.ic_background_remover),
        contentDescription = "Иконка приложения",
    )
}

@Preview
@Composable
fun PreviewAppIconDisplay(){
    AppIconDisplay()
}