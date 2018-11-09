package com.baseform.power;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

public class ApiAsyncTask extends AsyncTask<String, String, String> {

    private final Activity activity;
    private final String url;
    private final ReceiveCallback callback;
    private final HashMap<String, String> params;
    private final Map<String, String> headers;
    private ProgressDialog pd;
    private String method;
    private String action;

    ApiAsyncTask(Activity activity, String url, ReceiveCallback callback, String method, String action, HashMap<String, String> params, Map<String, String> loginHeaders) {
        this.activity = activity;
        this.url = url;
        this.callback = callback;
        this.method = method;
        this.action = action;
        this.params = params;
        this.headers = loginHeaders;
    }

    protected void onPreExecute() {
        super.onPreExecute();

        pd = new ProgressDialog(activity);
        pd.setMessage("Please wait");
        pd.setCancelable(false);
        pd.show();
    }

    protected String doInBackground(String... params) {
        ApiCall apiCall = new ApiCall(this.url, this.method, this.params, this.headers);
        return apiCall.parse(this.action);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (pd.isShowing()) pd.dismiss();
        this.callback.execute(result, this.activity);
    }

    public interface ReceiveCallback {
        void execute(String result, Activity activity);
    }
}