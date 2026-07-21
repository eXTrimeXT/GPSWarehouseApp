package com.gps.warehouse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CustomLoadingView(status: String? = null){
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        if (status != null){
            Text(status)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoadingPreview(){
    CustomLoadingView()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoadingPreviewWithStatus(){
    CustomLoadingView("Status")
}