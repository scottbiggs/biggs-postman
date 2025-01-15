package com.sleepfuriously.mypostman

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class MainViewmodel : ViewModel() {

    /** reference to the okhttp library */
    private val okHttpClient = OkHttpClient.Builder()
        .callTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .connectTimeout(2, TimeUnit.SECONDS)
        .build()


    private val _responseBody = MutableStateFlow("")
    var responseBody = _responseBody.asStateFlow()

    private val _successful = MutableStateFlow(false)
    var successful = _successful.asStateFlow()

    private val _code = MutableStateFlow(0)
    var code = _code.asStateFlow()

    private val _message = MutableStateFlow("")
    var message = _message.asStateFlow()


    /** use THIS instead of the regular OkHttpClient if we want to trust everyone */
    private val allTrustingOkHttpClient: OkHttpClient

    init {
        /** an array of trust managers that does not validate certificate chains--it trusts everyone! */
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    // this is empty on purpose: I don't want this check executed
                }

                override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    // this is empty on purpose: I don't want this check executed
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf<X509Certificate>()
                }
            }
        )

        // implementation of a socket trust manager that blindly trusts all
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        // a socket factory that uses the all-trusting trust manager
        val sslSocketFactory = sslContext.socketFactory

        val allTrustingBuilder = OkHttpClient.Builder()

        allTrustingBuilder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        allTrustingBuilder.hostnameVerifier { _, _ -> true }

        // now to make our all-trusting okhttp client
        allTrustingOkHttpClient = allTrustingBuilder.build()
    }


    /**
     * Executes a GET to the given url.
     *
     * side effects
     *  The following will probably change:
     *
     *   [_responseBody]
     *   [_successful]
     *   [_code]
     *   [_message]
     */
    fun get(
        ctx: Context,
        url: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean = false
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            // go ahead and save our data
            saveData(
                ctx = ctx,
                url = url,
                bodyStr = "",
                headerList = headerList,
                trustAll = trustAll
            )

            val response = synchronousGet(url, headerList, trustAll)

            _successful.value = response.isSuccessful
            _code.value = response.code
            _message.value = response.message
            _responseBody.value = response.body

            Log.d(TAG, "get() response -> $response")
        }
    }


    /**
     * Similar to [get], this does a post.
     *
     * side effects
     *  The following will probably change:
     *
     *   [_responseBody]
     *   [_successful]
     *   [_code]
     *   [_message]
     */
    fun post(
        ctx: Context,
        url: String,
        bodyStr: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // go ahead and save our data
            saveData(
                ctx = ctx,
                url = url,
                bodyStr = bodyStr,
                headerList = headerList,
                trustAll = trustAll
            )

            val response = synchronousPost(url, bodyStr, headerList, trustAll)
            _successful.value = response.isSuccessful
            _code.value = response.code
            _message.value = response.message
            _responseBody.value = response.body

            Log.d(TAG, "post() response -> $response")
        }
    }

    fun put(
        ctx: Context,
        url: String,
        bodyStr: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // go ahead and save our data
            saveData(
                ctx = ctx,
                url = url,
                bodyStr = bodyStr,
                headerList = headerList,
                trustAll = trustAll
            )
            val response = synchronousPut(url, bodyStr, headerList, trustAll)
            _successful.value = response.isSuccessful
            _code.value = response.code
            _message.value = response.message
            _responseBody.value = response.body

            Log.d(TAG, "put() response -> $response")
        }
    }

    //--------------------------------------------------
    //  private functions
    //--------------------------------------------------

    /**
     * Sends the GET request with the supplied url.
     *
     * @return      The response from the call.  Could be anything.
     *              Use getBodyFromResponse() or getCodeFromResponse()
     *              to decipher it.
     *
     * NOTE
     *      You must call this from a coroutine (or any non Main thread,
     *      as if anyone uses other threads anymore!)
     */
    private suspend fun synchronousGet(
        url: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean) : MyResponse {

        val requestBuilder = Request.Builder()

        // Making the request is a little tricky because of the headers.
        // Rather than one chain of builds, I have to separate because the
        // first header must be added with .header(), whereas successive
        // headers need to be done with .addHeader().

        try {
            requestBuilder.url(url)
        }
        catch (e: IllegalArgumentException) {
            Log.e(TAG, "Can't make a real url with $url!")
            e.printStackTrace()
            return MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf())
        }

        // because of the need to break a good ol' for loop is the way to go
        for (i in headerList.indices) {
            val header = headerList[i]
            if (header.first.isEmpty()) {
                break   // empty header means nothing to do
            }
            if (i == 0) {
                // the first header will replace
                requestBuilder.header(header.first, header.second)
            }
            else {
                // successive headers add
                requestBuilder.addHeader(header.first, header.second)
            }
        }

        val request = requestBuilder.build()

        try {
            if (trustAll) {
                val response = allTrustingOkHttpClient.newCall(request).execute()
                return MyResponse(response)
            }
            else {
                val response = okHttpClient.newCall(request).execute()
                return MyResponse(response)
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "exception in synchronousGetRequest($url, $trustAll)")
            e.printStackTrace()
            return MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf())
        }
    }


    /**
     * This delivers a POST to the specified url with a String data.
     *
     * @param   url         Where to send the POST
     *
     * @param   bodyStr     The string to send as data
     *
     * @param   headerList  Any headers that needs to be attached.
     *                      This is a Map<String, String> as there
     *                      can be multiple headers.
     *                      Use null (default) if no headers are needed
     *                      (they rarely are in our case).
     *
     * @param   trustAll    When true, all ssl trust checks are skipped.
     *                      Hope you know what you're doing!
     *
     * @return  The response from the server
     *
     * NOTE
     *      May NOT be called on the main thread!
     */
    private suspend fun synchronousPost(
        url: String,
        bodyStr: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean
    ) : MyResponse {

        Log.d(TAG, "synchronousPost( url = $url, body = $bodyStr, headers = $headerList, trustAll = $trustAll )")

        // need to make a RequestBody from the string (complicated!)
        val body = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())

        val requestBuilder = Request.Builder()

        try {
            requestBuilder
                .url(url)
                .post(body)
        }
        catch (e: IllegalArgumentException) {
            Log.e(TAG, "Can't make a real url with $url!")
            e.printStackTrace()
            return MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf())
        }

        headerList.forEachIndexed() { i, header ->
            if (i == 0) {
                if (header.first.isNotEmpty()) {
                    requestBuilder.header(header.first, header.second)
                }
            }
            else {
                if (header.first.isNotEmpty()) {
                    requestBuilder.addHeader(header.first, header.second)
                }
            }
        }
        val request = requestBuilder.build()

        try {
            val myResponse: MyResponse

            // two kinds of requests: unsafe and regular
            if (trustAll) {
                myResponse = MyResponse(allTrustingOkHttpClient.newCall(request).execute())
                return myResponse
            }
            else {
                myResponse = MyResponse(okHttpClient.newCall(request).execute())
                return myResponse
            }

        }
        catch (e: Exception) {
            Log.e(TAG, "exception in synchronousPost($url, $bodyStr, $headerList, $trustAll)")
            e.printStackTrace()
            return MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf())
        }
    }


    private suspend fun synchronousPut(
        url: String,
        bodyStr: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean
    ) : MyResponse {
        Log.d(TAG, "synchronousPut( url = $url, body = $bodyStr, headers = $headerList, trustAll = $trustAll )")

        // need to make a RequestBody from the string (complicated!)
        val body = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())

        val requestBuilder = Request.Builder()

        try {
            requestBuilder
                .url(url)
                .put(body)
        }
        catch (e: IllegalArgumentException) {
            Log.e(TAG, "Can't make a real url with $url!")
            e.printStackTrace()
            return MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf())
        }

        headerList.forEachIndexed() { i, header ->
            if (i == 0) {
                if (header.first.isNotEmpty()) {
                    requestBuilder.header(header.first, header.second)
                }
            }
            else {
                if (header.first.isNotEmpty()) {
                    requestBuilder.addHeader(header.first, header.second)
                }
            }
        }
        val request = requestBuilder.build()

        try {
            val myResponse: MyResponse

            // two kinds of requests: unsafe and regular
            if (trustAll) {
                myResponse = MyResponse(allTrustingOkHttpClient.newCall(request).execute())
                return myResponse
            }
            else {
                myResponse = MyResponse(okHttpClient.newCall(request).execute())
                return myResponse
            }

        }
        catch (e: Exception) {
            Log.e(TAG, "exception in synchronousPostRequest($url, $bodyStr, $headerList, $trustAll)")
            e.printStackTrace()
            return MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf())
        }
    }

    /**
     * Save the given data to long-term storage.  This should be called
     * when the UI is about to exit (and thus kill the viewmodel).
     *
     * The data comes from UI components and should be loaded again once
     * the program re-starts.
     */
    fun saveData(
        ctx: Context,
        url: String,
        bodyStr: String,
        headerList: List<Pair<String, String>>,
        trustAll: Boolean
    ) {
        val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

        prefs.edit {
            putString(PREFS_URL_KEY, url)
            putString(PREFS_BODY_KEY, bodyStr)
            putBoolean(PREFS_TRUSTALL_KEY, trustAll)
        }

        // The headers are a little more complicated:
        // they are saved as a key-value pairs in a separate file.
        val headerPrefs = ctx.getSharedPreferences(PREFS_HEADER_FILENAME, Context.MODE_PRIVATE)
        headerPrefs.edit(commit = true) {   // commit because I want this done BEFORE the next bit
            clear()     // remove all previous data
        }
        headerPrefs.edit {
            headerList.forEach { headerPair ->
                putString(headerPair.first, headerPair.second)
            }
        }
    }

    /**
     * Loads the url that was saved the last run.  If nothing was
     * found, then empty string is returned.
     */
    fun loadUrlData(ctx: Context) : String {
        val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_URL_KEY, "") ?: ""
    }

    /**
     * Similar to [loadUrlData], but this returns body string.
     */
    fun loadBodyData(ctx: Context) : String {
        val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_BODY_KEY, "") ?: ""
    }

    /**
     * Similar to [loadUrlData], but this returns the trust all boolean.
     * Defaults to false.
     */
    fun loadTrustAllData(ctx: Context) : Boolean {
        val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREFS_TRUSTALL_KEY, false)
    }

    /**
     * Returns all the pairs of data in the headers file.
     *
     * The file will be a list of Pairs that correspond to the key-value
     * pairs of the header.
     */
    fun loadHeadersData(
        ctx: Context
    ) : List<Pair<String, String?>> {

        val prefs = ctx.getSharedPreferences(PREFS_HEADER_FILENAME, Context.MODE_PRIVATE)

        // find all the keys
        val keys = prefs.all.keys

        // create our list of headers
        val headersList = mutableListOf<Pair<String, String?>>()
        keys.forEach() { key ->
            val pair = Pair<String, String?>(
                key,
                prefs.getString(key, null)
            )
            headersList.add(pair)
        }

        return headersList
    }

}


//--------------------------------------------------
//  classes
//--------------------------------------------------


/**
 * Responses returned from OkHttp requests are stored in this data
 * structure.  This is done as the regular [Response] data is
 * very picky about how it is accessed (which thread, how many
 * times, etc.).  This is a regular data class that can be used
 * in normal ways.
 *
 * Note that this can be constructed in two ways: directly (as seen
 * below) and through the companion object invoke operator.  You'll
 * almost certainly want to use the operator (see [MyResponse.invoke]).
 */
data class MyResponse(
    val isSuccessful: Boolean,
    val code: Int,
    val message: String,
    val body: String,
    /**
     * List of headers.  The first of each pair is the name, the 2nd is the value.
     * I will probably never use these as these are technical details of the transmission.
     */
    val headers: List<Pair<String, String>>
) {

    companion object {
        /**
         * Create a 2nd way to construct a MyResponse.
         * This uses a Response as its input param.
         *
         * example:
         *      val response = some_network_call()
         *      val myresponse = MyResponse(response)
         *
         * WARNING
         *  Unlike the normal constructor, this needs to be called off the
         *  main thread as it accesses [Response] data.
         */
        operator fun invoke(response: Response) : MyResponse {

            // add the headers one-by-one to header list
            val headerList = mutableListOf<Pair<String, String>>()
            response.headers.forEach() { header ->
                val name = header.first
                val value = header.second
                headerList.add(Pair(name, value))
            }

            return MyResponse(
                response.isSuccessful,
                response.code,
                response.message,
                response.peekBody(BODY_PEEK_SIZE).string(),
                headerList
            )
        }
    }
}


private const val TAG = "MainViewmodel"

/** The max number of bytes that we'll look at for a [Response] body (100k) */
private const val BODY_PEEK_SIZE = 100000L


//------------------
//  prefs
//------------------

private const val PREFS_FILENAME = "postman_prefs"
private const val PREFS_HEADER_FILENAME = "postman_header_prefs"

private const val PREFS_URL_KEY = "url"
private const val PREFS_BODY_KEY = "body"
private const val PREFS_TRUSTALL_KEY = "trust_all"
