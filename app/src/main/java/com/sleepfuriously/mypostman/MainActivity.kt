package com.sleepfuriously.mypostman

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepfuriously.mypostman.ui.theme.MyPostmanTheme

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MyPostmanTheme {

                val viewmodel = MainViewmodel()

                // flow results
                val responseSuccess by viewmodel.successful.collectAsStateWithLifecycle()
                val responseBody by viewmodel.responseBody.collectAsStateWithLifecycle()
                val responseCode by viewmodel.code.collectAsStateWithLifecycle()
                val responseMessage by viewmodel.message.collectAsStateWithLifecycle()


                // text fields
                var url by remember { mutableStateOf("") }
                var sendBody by remember { mutableStateOf("") }
                var headerKey by remember { mutableStateOf("") }
                var headerValue by remember { mutableStateOf("") }

                // sliders
                var trustAll by remember { mutableStateOf(false) }

//                /** When true, show all text with big fonts */
//                val bigFont = remember { mutableStateOf(true) }
//                val bigFontSize = remember { mutableIntStateOf(BIG_FONT_SIZE) }

                val landscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .safeContentPadding()      // takes the insets into account (nav bars, etc)
                        ) {
                            item {
                                Row {
                                    Text(
                                        stringResource(R.string.title),
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.alignBy(LastBaseline)
                                    )
                                    Text(
                                        " v${BuildConfig.VERSION_NAME}",
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .alignBy(LastBaseline)
                                            .padding(start = 8.dp)
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                            item {
                                TextField(
                                    textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                                    value = url,
                                    onValueChange = { url = it },
                                    label = { Text(stringResource(R.string.url)) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            item {
                                TextField(
                                    textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                                    value = sendBody,
                                    onValueChange = { sendBody = it },
                                    label = { Text(stringResource(R.string.body)) }
                                )
                            }

                            item {
                                if (landscape) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        OutlinedTextField(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                                            value = headerKey,
                                            onValueChange = { headerKey = it },
                                            label = { Text(stringResource(R.string.header_key)) }
                                        )
                                        OutlinedTextField(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                                            value = headerValue,
                                            onValueChange = { headerValue = it },
                                            label = { Text(stringResource(R.string.header_value)) }
                                        )
                                    }
                                }
                                else {
                                    Column(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        OutlinedTextField(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                                            value = headerKey,
                                            onValueChange = { headerKey = it },
                                            label = { Text(stringResource(R.string.header_key)) }
                                        )
                                        OutlinedTextField(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                                            value = headerValue,
                                            onValueChange = { headerValue = it },
                                            label = { Text(stringResource(R.string.header_value)) }
                                        )

                                    }
                                }
                            }

                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { viewmodel.get(
                                            url,
                                            headerList = listOf(Pair(headerKey, headerValue)),
                                            trustAll
                                        ) }
                                    ) { Text(stringResource(R.string.get)) }

                                    Spacer(modifier = Modifier.width(24.dp))

                                    Button(
                                        onClick = {
                                            Log.d(TAG, "POST button click. bodyStr = $sendBody, header = $headerKey, $headerValue")
                                            viewmodel.post(
                                                url = url,
                                                bodyStr = sendBody,
                                                headerList = listOf(Pair(headerKey, headerValue)),
                                                trustAll = trustAll
                                            )
                                        }
                                    ) { Text(stringResource(R.string.post)) }

                                    Spacer(modifier = Modifier.width(24.dp))

                                    Button(
                                        onClick = {
                                            Log.d(TAG, "PUT button click. bodyStr = $sendBody, header = $headerKey, $headerValue")
                                            viewmodel.put(
                                                url = url,
                                                bodyStr = sendBody,
                                                headerList = listOf(Pair(headerKey, headerValue)),
                                                trustAll = trustAll
                                            )
                                        }
                                    ) { Text(stringResource(R.string.put)) }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            item {
                                SwitchWithLabel(
                                    state = trustAll,
                                    label = stringResource(R.string.trust_all),
                                    onStateChange = { checked ->
                                        trustAll = checked
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            item {
                                Row {
                                    Text(stringResource(
                                        R.string.success_equals),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(responseSuccess.toString())
                                }
                            }
                            item {
                                Row {
                                    Text(
                                        stringResource(R.string.code_equals),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(responseCode.toString())
                                }
                            }
                            item {
                                Row {
                                    Text(
                                        stringResource(R.string.message_equals),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(responseMessage)
                                }
                            }
                            item {
                                Row {
                                    Text(
                                        stringResource(R.string.body_equals),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(responseBody)
                                }
                            }
                        }
                    }

//                    DrawBigFontButton(
//                        bigFont = bigFont.value,
//                        onClickAction = {
//                            bigFont.value = !bigFont.value
//                            if (bigFont.value) {
//                                bigFontSize.intValue = BIG_FONT_SIZE
//                            }
//                            else {
//                                bigFontSize.intValue = REGULAR_FONT_SIZE
//                            }
//                        }
//                    )
                }
            }
        }
    }


    @Composable
    private fun DrawBigFontButton(
        bigFont : Boolean,
        onClickAction: () -> Unit
    ) {
        // for the lower left switch
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Button(
                onClick = onClickAction,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomStart)
            ) {
                Icon(
                    painter = painterResource(
                        if (bigFont) R.drawable.baseline_text_increase_24
                        else R.drawable.baseline_text_decrease_24
                    ),
                    contentDescription = "big font button"
                )
            }
        }

    }
}


/*
@Composable
fun BigText(text : String, fontSize: Int = BIG_FONT_SIZE, bold: Boolean = false) {
    if (bold) {
        Text(text, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
    else {
        Text(text, fontSize = fontSize.sp)
    }
}
*/

@Composable
private fun SwitchWithLabel(modifier: Modifier = Modifier, label: String, state: Boolean, onStateChange: (Boolean) -> Unit) {

    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                // This is for removing ripple when Row is clicked
                indication = null,
                role = Role.Switch,
                onClick = {
                    onStateChange(!state)
                }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {

        Text(text = label)
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Switch(
            checked = state,
            onCheckedChange = {
                onStateChange(it)
            }
        )
    }
}

private const val TAG = "MainActivity"

private const val BIG_FONT_SIZE = 24
private const val REGULAR_FONT_SIZE = 14