package id.bluebird.chat.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The Loading Component for [Surface] layout composable
 */
@Composable
fun LoadingSurface(
    loading: Boolean,
    overlayColor: Color = Color.White.copy(alpha = 0.54f),
    content: @Composable () -> Unit
) {
    content()

    if (loading) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = overlayColor
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = "Loading...",
                )
            }
        }
    }
}
