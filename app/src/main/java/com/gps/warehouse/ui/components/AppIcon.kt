package com.gps.warehouse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.gps.warehouse.R

@Composable
fun AppIconProd() {
    Image(
        painter = painterResource(id = R.mipmap.ic_background_prod_full_size),
        contentDescription = "Иконка приложения",
    )
}

@Composable
fun AppIconTest() {
    Image(
        painter = painterResource(id = R.mipmap.ic_background_test_full_size),
        contentDescription = "Иконка приложения",
    )
}

@Preview
@Composable
fun PreviewAppIconProd(){
    AppIconProd()
}

@Preview
@Composable
fun PreviewAppIconTest(){
    AppIconTest()
}