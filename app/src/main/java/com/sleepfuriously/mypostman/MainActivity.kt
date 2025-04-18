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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepfuriously.mypostman.ui.theme.MyPostmanTheme
import com.sleepfuriously.mypostman.ui.verticalColumnScrollbar

class MainActivity : ComponentActivity() {

    private lateinit var viewmodel: MainViewmodel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MyPostmanTheme {

                viewmodel = MainViewmodel()
                viewmodel.loadData(this)

                // flow results
                val responseSuccess by viewmodel.successful.collectAsStateWithLifecycle()
                val responseBody by viewmodel.responseBody.collectAsStateWithLifecycle()
                val responseCode by viewmodel.code.collectAsStateWithLifecycle()
                val responseMessage by viewmodel.message.collectAsStateWithLifecycle()
                val backActive by viewmodel.backActive.collectAsStateWithLifecycle()
                val forwardActive by viewmodel.forwardActive.collectAsStateWithLifecycle()


                // text fields
                val url by viewmodel.uiUrl.collectAsStateWithLifecycle()
                val sendBody by viewmodel.uiBody.collectAsStateWithLifecycle()

                val headers by viewmodel.headers.collectAsStateWithLifecycle()

                // for slider
                val trustAll by viewmodel.uiTrustAll.collectAsStateWithLifecycle()

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
                        ColumnStuff(
                            viewmodel = viewmodel,
                            backActive = backActive,
                            forwardActive = forwardActive,
                            url = url,
                            sendBody = sendBody,
                            headers = headers,
                            landscape = landscape,
                            trustAll = trustAll,
                            responseSuccess = responseSuccess,
                            responseCode = responseCode,
                            responseMessage = responseMessage,
                            responseBody = responseBody
                        )
                    }

                }
            }
        }
    }

}

/**
 * This is the meat of the UI
 */
@Composable
private fun ColumnStuff(
    viewmodel: MainViewmodel,
    backActive: Boolean,
    forwardActive: Boolean,
    url: String,
    sendBody: String,
    headers: List<Pair<String, String>>,
    landscape: Boolean,
    trustAll: Boolean,
    responseSuccess: Boolean,
    responseCode: Int,
    responseMessage: String,
    responseBody: String,

) {
    val ctx = LocalContext.current

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            // these two additions are defined in VerticalColumnScrollbar
            .verticalColumnScrollbar(scrollState)   // this MUST come before .verticalScroll() !!!
            .verticalScroll(scrollState)
            .fillMaxSize()
            .padding(8.dp)
            .safeContentPadding()      // takes the insets into account (nav bars, etc)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                onClick = { viewmodel.goBackUi() },
                enabled = backActive
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "")
            }

            Button(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                onClick = { viewmodel.goForwardUi() },
                enabled = forwardActive
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "")
            }


            Text(
                stringResource(R.string.title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(start = 16.dp)
            )
            Text(
                " v${BuildConfig.VERSION_NAME}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(alignment = Alignment.Top)
                    .padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        TextField(
            textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
            value = url,
            onValueChange = { viewmodel.changeUrl(it) },
            label = { Text(stringResource(R.string.url)) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextField(
            textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
            value = sendBody,
            onValueChange = { viewmodel.changeUiBody(it) },
            label = { Text(stringResource(R.string.body)) }
        )

        Button(
            onClick = { viewmodel.addHeader("", "") },
            modifier = Modifier
                .padding(top = 18.dp)
        ) { Text(stringResource(R.string.add_header)) }

        // the headers
        headers.forEach { header ->
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
                        value = header.first,
                        onValueChange = { viewmodel.changeUiHeaderKey(header.first, it) },
                        label = { Text(stringResource(R.string.header_key)) }
                    )
                    OutlinedTextField(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                        value = header.second,
                        onValueChange = { viewmodel.changeUiHeaderValue(header.first, it) },
                        label = { Text(stringResource(R.string.header_value)) }
                    )
                    Button(
                        onClick = { viewmodel.removeHeader(header.first) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(1.dp),
                        modifier = Modifier
                            .padding(end = 8.dp, top = 8.dp)
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "remove"
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    OutlinedTextField(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                        value = header.first,
                        onValueChange = { viewmodel.changeUiHeaderKey(header.first, it) },
                        label = { Text(stringResource(R.string.header_key)) }
                    )
                    OutlinedTextField(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textStyle = TextStyle(fontSize = BIG_FONT_SIZE.sp),
                        value = header.second,
                        onValueChange = { viewmodel.changeUiHeaderValue(header.first, it) },
                        label = { Text(stringResource(R.string.header_value)) }
                    )

                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    viewmodel.get(
                        ctx = ctx,
                        url,
                        body = sendBody,
                        headerList = headers,
                        trustAll
                    )
                }
            ) { Text(stringResource(R.string.get)) }

            Spacer(modifier = Modifier.width(24.dp))

            Button(
                onClick = {
                    Log.d(TAG, "POST button click. bodyStr = $sendBody, headers = $headers")
                    viewmodel.post(
                        ctx = ctx,
                        url = url,
                        bodyStr = sendBody,
                        headerList = headers,
                        trustAll = trustAll
                    )
                }
            ) { Text(stringResource(R.string.post)) }

            Spacer(modifier = Modifier.width(24.dp))

            Button(
                onClick = {
                    Log.d(TAG, "PUT button click. bodyStr = $sendBody, headers = $headers")
                    viewmodel.put(
                        ctx = ctx,
                        url = url,
                        bodyStr = sendBody,
                        headerList = headers,
                        trustAll = trustAll
                    )
                }
            ) { Text(stringResource(R.string.put)) }
        }

        Spacer(modifier = Modifier.height(4.dp))

        SwitchWithLabel(
            state = trustAll,
            label = stringResource(R.string.trust_all),
            onStateChange = { checked ->
                viewmodel.changeUiTrustAll(checked)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text(
                stringResource(
                    R.string.success_equals
                ),
                fontWeight = FontWeight.Bold
            )
            SelectionContainer {
                Text(responseSuccess.toString())
            }
        }
        Row {
            Text(
                stringResource(R.string.code_equals),
                fontWeight = FontWeight.Bold
            )
            SelectionContainer {
                Text(responseCode.toString())
            }
        }
        Row {
            Text(
                stringResource(R.string.message_equals),
                fontWeight = FontWeight.Bold
            )
            SelectionContainer {
                Text(responseMessage)
            }
        }
        Row {
            Text(
                stringResource(R.string.body_equals),
                fontWeight = FontWeight.Bold
            )
            SelectionContainer {
                Text(
                    responseBody,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

}


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