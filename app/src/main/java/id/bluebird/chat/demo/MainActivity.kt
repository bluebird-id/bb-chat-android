package id.bluebird.chat.demo

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.messaging.FirebaseMessaging
import id.bluebird.chat.BBChat
import id.bluebird.chat.NotifPipeline
import id.bluebird.chat.Platform
import id.bluebird.chat.R
import id.bluebird.chat.demo.theme.BluebirdChatTheme
import id.bluebird.chat.sdk.db.BaseDb
import id.bluebird.chat.sdk.demos.message.UserType

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val db = BaseDb.getInstance()
    val isLoading = remember { mutableStateOf(false) }
    val isLogin = remember { mutableStateOf(db.isReady) }
    val chatTopicName = remember { mutableStateOf("") }
    val callTopicName = remember { mutableStateOf("") }

    /** Use this if getParticipants cant be access */
//    val chatTopicName = remember { mutableStateOf("grpm_6FL8t1wr4") }
//    val callTopicName = remember { mutableStateOf("usryGFySww-TSI") }

    val context = LocalContext.current
    val activity = LocalContext.current as Activity

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
            }

            val usernameState = remember { mutableStateOf("") }
            val passwordState = remember { mutableStateOf(usernameState.value) }
            val fullnameState = remember { mutableStateOf("") }

            val orderIdState = remember { mutableStateOf("") }

            Column(
                modifier = Modifier.weight(1f, false),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                OutlinedTextField(
                    value = usernameState.value,
                    onValueChange = { usernameState.value = it },
                    label = { Text("Username") },
                    modifier = Modifier.testTag("username_field"),
                    enabled = !isLogin.value,
                )
                OutlinedTextField(
                    value = fullnameState.value,
                    onValueChange = { fullnameState.value = it },
                    label = { Text("Fullname") },
                    modifier = Modifier.testTag("fullname_field"),
                    enabled = !isLogin.value,
                )
                if (isLogin.value && chatTopicName.value.isEmpty()) {
                    OutlinedTextField(
                        value = orderIdState.value,
                        onValueChange = { orderIdState.value = it },
                        label = { Text("Order Id") },
                        modifier = Modifier.testTag("order_id_field")
                    )
                }
                if (false) {
                    OutlinedTextField(
                        value = passwordState.value,
                        onValueChange = { passwordState.value = it },
                        label = { Text("Password") },
                        modifier = Modifier
                            .testTag("password_field")
                    )
                }
                Spacer(modifier = modifier.height(16.dp))
                MainButton(
                    modifier = modifier,
                    onClick = {
                        isLoading.value = true

                        BBChat.login(
                            username = usernameState.value,
                            fullname = fullnameState.value,
                            onSuccess = {
                                isLogin.value = true
                                isLoading.value = false
                            },
                            onError = {
                                isLogin.value = false
                                isLoading.value = false

                                Log.e("BBChat", "onError: $it")

                                activity.runOnUiThread {
                                    Toast.makeText(context, "onError: $it", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        )
                    },
                    enabled = !isLogin.value,
                    textButton = stringResource(id = R.string.login_hint)
                )
                MainButton(
                    modifier = modifier,
                    onClick = {
                        isLoading.value = true

                        BBChat.getRoom(
                            userId = usernameState.value,
                            orderId = orderIdState.value,
                            onSuccess = {
                                isLoading.value = false

                                chatTopicName.value = it?.chatRoomId ?: ""
                                callTopicName.value = it?.callRoomId ?: ""
                                fullnameState.value = it?.fullName ?: ""

                                activity.runOnUiThread {
                                    Toast.makeText(context, "Success", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            },
                            onError = {
                                Log.e("BBChat", "onError: $it")
                                isLoading.value = false

                                activity.runOnUiThread {
                                    Toast.makeText(context, "Failed", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        )
                    },
                    enabled = isLogin.value && chatTopicName.value.isEmpty(),
                    textButton = "Get Room / Participant"
                )
                MainButton(
                    modifier = modifier,
                    onClick = {
                        BBChat.toMessageScreen(
                            context = context,
                            opponentsName = fullnameState.value,
                            topicChatName = chatTopicName.value,
                            topicCallName = callTopicName.value,
                            userType = UserType.DRIVER
                        )
                    },
                    enabled = isLogin.value && chatTopicName.value.isNotEmpty(),
                    textButton = "Open Chat"
                )
                MainButton(
                    modifier = modifier,
                    onClick = {
                        BBChat.toCallScreen(
                            context = context,
                            topicName = callTopicName.value,
                        )
                    },
                    enabled = isLogin.value && callTopicName.value.isNotEmpty(),
                    textButton = "Open Call"
                )
                MainButton(
                    modifier = modifier,
                    onClick = {
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { task ->
                                if (!task.isSuccessful()) {
                                    Log.w(
                                        "BBchat",
                                        "Fetching FCM registration token failed",
                                        task.getException()
                                    )
                                    return@addOnCompleteListener
                                }

                                val token: String = task.getResult()
                                BBChat.saveDeviceToken(
                                    clientId = "6f436ccd-8041-4104-9688-8727882cf3da",
                                    deviceToken = token,
                                    platform = Platform.Android,
                                    notifPipeline = NotifPipeline.FCM,
                                    onSuccess = {},
                                    onError = {}
                                )
                            }

                    },
                    enabled = isLogin.value && callTopicName.value.isNotEmpty(),
                    textButton = "Save Device Token"
                )
                MainButton(
                    modifier = modifier,
                    onClick = {
                        isLogin.value = BBChat.logout()
                        chatTopicName.value = ""
                        callTopicName.value = ""
                    },
                    enabled = isLogin.value,
                    textButton = "Logout"
                )
            }
        }
    }
}

@Composable
fun MainButton(
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    textButton: String,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(
            text = textButton,
            style = MaterialTheme.typography.titleLarge,
        )
    }
    Spacer(modifier = modifier.height(16.dp))
}

@Preview()
@Composable
fun DefaultPreview() {
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
