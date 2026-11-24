package com.gps.warehouse.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.gps.warehouse.R
import java.io.File

@Composable
fun AppIconProd() {
    Image(
        painter = painterResource(id = R.mipmap.ic_background_prod_full_size),
        contentDescription = "Иконка приложения",
    )
}

//@Composable
//fun AppIconTest() {
//    Image(
//        painter = painterResource(id = R.mipmap.ic_background_test_full_size),
////        painter = painterResource(id = R.drawable.cow_flex_dev),
//        contentDescription = "Иконка приложения",
//    )
//}

@SuppressLint("SdCardPath")
@Composable
fun AppIconTest(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val gif = File(context.filesDir, "cow_flex_dev_cutter.gif")
    val mediaUri = FileProvider.getUriForFile(
        LocalContext.current,
        "${context.packageName}.provider",
        gif
    )
    Image(
        modifier = Modifier.size(800.dp, 240.dp).padding(10.dp),
        painter = rememberDrawablePainter(
            drawable = createAnimatedImageDrawableFromImageDecoder(context, mediaUri)
        ),
        contentDescription = "animated gif"
    )
}


private fun createAnimatedImageDrawableFromImageDecoder(context: Context, uri: Uri): AnimatedImageDrawable {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val drawable = ImageDecoder.decodeDrawable(source)
    return drawable as AnimatedImageDrawable
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