package com.baseform.power;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class ApiCall {
    private final HashMap<String, String> params;
    private final Map<String, String> headers;
    private String challenges_url = "api.jsp?api=challenges";
    private String comment_url = "api.jsp?api=comment";
    String url;
    String method;

    ApiCall(String url, String method, HashMap<String, String> params, Map<String, String> headers) {
        this.url = url;
        this.method = method;
        this.params = params;
        this.headers = headers;
    }

    public String performGetCall(String requestURL) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestProperty("Cookie", CookieManager.getInstance().getCookie(requestURL));
            for (String s : this.headers.keySet()) {
                conn.setRequestProperty(s, this.headers.get(s));
            }
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public String performPostCall(String requestURL, HashMap<String, String> postDataParams) {

        URL url;
        String response = "";
        String boundary =  "*****";
        String crlf = "\r\n";
        String twoHyphens = "--";

        try {
            url = new URL(requestURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDoOutput(true); // indicates POST method
            connection.setDoInput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            connection.setRequestProperty("Cookie", CookieManager.getInstance().getCookie(requestURL));
            for (String s : this.headers.keySet()) {
                connection.setRequestProperty(s, this.headers.get(s));
            }

            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());

            for (String s : postDataParams.keySet()) {
                dataOutputStream.writeBytes(twoHyphens + boundary + crlf);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + s + "\""+ crlf);
                dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + crlf);
                dataOutputStream.writeBytes(crlf);
                dataOutputStream.writeBytes(postDataParams.get(s) + crlf);
                dataOutputStream.flush();
            }


            if (postDataParams.get("path") != null) {
                File uploadFile = new File(postDataParams.get("path"));
                String fileName = uploadFile.getName();
                dataOutputStream.writeBytes(twoHyphens + boundary + crlf);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" +
                        "photo" + "\";filename=\"" +
                        fileName + "\"" + crlf);

                dataOutputStream.writeBytes("Content-Type:" + postDataParams.get("mime") + crlf);
                dataOutputStream.writeBytes(crlf);

                byte[] bytes = new byte[(int) uploadFile.length()];
                new FileInputStream(uploadFile).read(bytes);

                dataOutputStream.write(bytes);
            }


            dataOutputStream.writeBytes(crlf);
            dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
            dataOutputStream.flush();
            dataOutputStream.close();

            // checks server's status code first
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                InputStream responseStream = new
                        BufferedInputStream(connection.getInputStream());

                BufferedReader responseStreamReader =
                        new BufferedReader(new InputStreamReader(responseStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                responseStreamReader.close();

                response = stringBuilder.toString();
                connection.disconnect();
            } else {
                throw new IOException("Server returned non-OK status: " + status);
            }

            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }


    public String getPostJson(String requestUrl) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();


            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuffer buffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            return buffer.toString();


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String postComment(HashMap<String, String> params) {
        return performPostCall(this.url + comment_url, params);
    }

    public String getLoginToken() {
        URL url;
        try {
            url = new URL(this.url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Cookie", CookieManager.getInstance().getCookie(this.url));
            conn.connect();

            String token = conn.getHeaderField("tok");
            conn.disconnect();

            return token;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String getChallenges() {
        return getPostJson(this.url + challenges_url);
    }


    public String parse(String action) {
        if (action.equals("challenges")) {
            return getChallenges();
        } else if (action.equals("token")) {
            return getLoginToken();
        } else if (action.equals("comment")) {
            return postComment(this.params);
        }

        return "";
    }

}
