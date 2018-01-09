package com.developer.fabian.codescanner.client;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.developer.fabian.codescanner.R;
import com.developer.fabian.codescanner.entity.TerminalScanner;
import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CodeScannerApiClient extends AsyncTask<TerminalScanner, String, HttpResponse> {

    private static final String URL_POST = "https://codescannerapi.herokuapp.com/api/codes";
    private static final String TAG = CodeScannerApiClient.class.getSimpleName();

    private CoordinatorLayout layout;
    private ProgressDialog dialog;
    private Context context;

    public CodeScannerApiClient(Context context, CoordinatorLayout layout) {
        this.context = context;
        this.layout = layout;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        setUpProcessDialog();
    }

    @Override
    protected HttpResponse doInBackground(TerminalScanner... params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(URL_POST);
        HttpResponse response = null;

        try {
            TerminalScanner scanner = params[0];
            String entityJSON = buildJSON(scanner);
            StringEntity entity = new StringEntity(entityJSON);

            httppost.setEntity(entity);
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-type", "application/json");

            response = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
            Log.v(TAG, context.getString(R.string.exception_protocol));
        } catch (IOException e) {
            Log.v(TAG, context.getString(R.string.exception_io));
        }

        return response;
    }

    @Override
    protected void onPostExecute(HttpResponse response) {
        super.onPostExecute(response);

        try {
            int status = response.getStatusLine().getStatusCode();

            if (status == 200) {
                HttpEntity entity = response.getEntity();
                String data = EntityUtils.toString(entity);

                JSONObject message = new JSONObject(data);

                String result = message.getString("message");

                if (result.equals("success"))
                    Snackbar.make(layout, R.string.messageTerminalAddSuccess, Snackbar.LENGTH_LONG).show();
            } else if (status == 500 || status == 400 || status == 404) {
                Snackbar.make(layout, R.string.messageErrorAPI, Snackbar.LENGTH_LONG).show();
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        dialog.dismiss();
    }

    private void setUpProcessDialog() {
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.message_dialog));
        dialog.setIndeterminate(false);
        dialog.setCancelable(true);
        dialog.show();
    }

    private String buildJSON(TerminalScanner scanner) {
        Gson gson = new Gson();

        return gson.toJson(scanner);
    }
}
