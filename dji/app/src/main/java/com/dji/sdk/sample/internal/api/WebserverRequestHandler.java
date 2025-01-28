package com.dji.sdk.sample.internal.api;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class WebserverRequestHandler extends AsyncTask<String, Void, Map<String, String>> {
    private static final String TAG = "ZeroKeyRequest";
    private final OnRequestCompleteListener callback;

    // Base URL for testing with a local Python server
    private static final String MOCK_BASE_URL = "http://10.0.2.2:8080/mock_response.json"; // Replace localhost with 10.0.2.2 for Android emulator

    public WebserverRequestHandler(OnRequestCompleteListener callback) {
        this.callback = callback;
    }

    @Override
    protected Map<String, String> doInBackground(String... params) {
        Log.i(TAG, "Do in background started");

        // Use the first parameter if provided; otherwise, default to the mock server
        String urlString = params.length > 0 ? params[0] : MOCK_BASE_URL;
        Log.i(TAG, "Requesting URL: " + urlString);

        return makeHttpRequest(urlString);
    }

    @Override
    protected void onPostExecute(Map<String, String> resultMap) {
        if (callback != null) {
            callback.onRequestComplete(resultMap);
        }
    }

    private Map<String, String> makeHttpRequest(String urlString) {
        Map<String, String> resultMap = new HashMap<>();
        HttpURLConnection urlConnection = null;

        try {
            urlConnection = initializeConnection(urlString);
            String response = readResponse(urlConnection);
            resultMap.put("result", response);
            Log.i(TAG, "Response: " + response);
        } catch (IOException e) {
            Log.e(TAG, "Error making ZeroKey request", e);
            resultMap.put("error", "Failed to make request: " + e.getMessage());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return resultMap;
    }

    private HttpURLConnection initializeConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Authorization", "Bearer YOUR_API_KEY"); // Replace with actual key or mock key
        return urlConnection;
    }

    private String readResponse(HttpURLConnection urlConnection) throws IOException {
        InputStream inputStream = urlConnection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }

        return stringBuilder.toString();
    }

    public interface OnRequestCompleteListener {
        void onRequestComplete(Map<String, String> resultMap);
    }
}


