package com.developer.fabian.codescanner.client

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.util.Log

import com.developer.fabian.codescanner.R
import com.developer.fabian.codescanner.entity.TerminalScanner
import com.google.gson.Gson

import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.json.JSONException
import org.json.JSONObject

import java.io.IOException

class CodeScannerApiClient(private val context: Context, private val layout: CoordinatorLayout) : AsyncTask<TerminalScanner, String, HttpResponse>() {
    companion object {
        private const val URL_POST = "https://codescannerapi.herokuapp.com/api/codes"
        private val TAG = CodeScannerApiClient::class.java.simpleName
    }

    private var dialog: ProgressDialog? = null

    override fun onPreExecute() {
        super.onPreExecute()
        setUpProcessDialog()
    }

    override fun doInBackground(vararg params: TerminalScanner): HttpResponse? {
        val httpClient = DefaultHttpClient()
        val httpPost = HttpPost(URL_POST)
        var response: HttpResponse? = null

        try {
            val scanner = params[0]
            val entityJSON = buildJSON(scanner)
            val entity = StringEntity(entityJSON)

            httpPost.entity = entity
            httpPost.setHeader("Accept", "application/json")
            httpPost.setHeader("Content-type", "application/json")

            response = httpClient.execute(httpPost)
        } catch (e: ClientProtocolException) {
            Log.v(TAG, context.getString(R.string.exception_protocol))
        } catch (e: IOException) {
            Log.v(TAG, context.getString(R.string.exception_io))
        }

        return response
    }

    override fun onPostExecute(response: HttpResponse) {
        super.onPostExecute(response)

        try {
            val status = response.statusLine.statusCode

            if (status == 200) {
                val entity = response.entity

                val data = EntityUtils.toString(entity)
                val message = JSONObject(data)
                val result = message.getString("message")

                if (result == "success")
                    Snackbar.make(layout, R.string.messageTerminalAddSuccess, Snackbar.LENGTH_LONG).show()
            } else if (status == 500 || status == 400 || status == 404) {
                Snackbar.make(layout, R.string.messageErrorAPI, Snackbar.LENGTH_LONG).show()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        dialog!!.dismiss()
    }

    private fun setUpProcessDialog() {
        dialog = ProgressDialog(context)
        dialog!!.setMessage(context.getString(R.string.message_dialog))
        dialog!!.isIndeterminate = false
        dialog!!.setCancelable(true)
        dialog!!.show()
    }

    private fun buildJSON(scanner: TerminalScanner): String {
        val gson = Gson()
        return gson.toJson(scanner)
    }
}
