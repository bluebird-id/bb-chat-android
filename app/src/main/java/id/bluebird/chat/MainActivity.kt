package id.bluebird.chat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import id.bluebird.chat.methods.login
import id.bluebird.chat.methods.toMessageScreen
import id.bluebird.chat.sdk.Const
import id.bluebird.chat.sdk.db.BaseDb
import id.bluebird.chat.sdk.demos.login.LoginActivity
import id.bluebird.chat.sdk.demos.message.MessageActivity
import id.bluebird.chat.ui.theme.BluebirdChatTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluebirdChatTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val db = BaseDb.getInstance()
    val isLoading = remember { mutableStateOf(false) }
    val isLoginSuccess = remember { mutableStateOf(db.isReady) }

    val context = LocalContext.current as Activity

    LoadingSurface(isLoading.value) {
        Column(
            modifier = modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    modifier = modifier.size(75.dp),
                    painter = painterResource(id = R.drawable.ic_logo_bluebird),
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = modifier
                )
                Spacer(modifier = modifier.height(32.dp))
            }

            Column(
                modifier = Modifier.weight(1f, false),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        login(
                            activity = context,
                            isLoginSuccess = isLoginSuccess,
                            isLoading = isLoading
                        )
                    },
                    enabled = !isLoginSuccess.value,
                ) {
                    Text(
                        text = stringResource(id = R.string.login_hint),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(modifier = modifier.height(16.dp))
                Button(
                    onClick = {
                        toMessageScreen(context)
                    },
                    enabled = isLoginSuccess.value,
                ) {
                    Text(
                        text = "Open Chat",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}
