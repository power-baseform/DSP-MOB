package com.baseform.power;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;


public class CommentActivity extends BaseActivity {
    private String MIME_TEXT_PLAIN = "text/plain";
    public static final int PHOTO_TAKEN = 1;
    private HashMap<String, String> challenges;
    private String currentChallenge;
    private String currentPhoto;
    private int SELECT_FILE = 2;
    private String currentAttachment;
    private String mime;
    private NfcAdapter defaultAdapter;
    private PendingIntent pendingIntent;


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs == null) return;
            NdefMessage[] msgs = new NdefMessage[rawMsgs.length];

            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }

            String text = "";
            if (msgs.length > 0)
                for (NdefMessage msg : msgs) {
                    byte[] bytes = msg.toByteArray();
                    text += new String(bytes);
                }

            if (text.length() == 0) return;

            String bf_p_1 = "BF_P_";
            int bf_p_ = text.indexOf(bf_p_1);
            int p_fb = text.indexOf("_P_FB");
            if (bf_p_ > -1 && p_fb > -1) {
                int beginIndex = bf_p_ + bf_p_1.length();
                text = text.substring(beginIndex, p_fb);

                int idx = -1;
                Object[] objects = challenges.keySet().toArray();
                for (int i = 0; i < objects.length; i++) {
                    String id = (String) objects[i];
                    if (text.equals(id)) {
                        idx = i;
                        break;
                    }
                }

                if (idx > -1) {
                    currentChallenge = text;

                    EditText viewById = findViewById(R.id.body);
                    viewById.setText("Attendance confirmed with NFC! \n\n" + viewById.getText());
                    Spinner spinner = findViewById(R.id.spinner);
                    spinner.setSelection(idx);

                }

            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        defaultAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        changeTab(R.id.comment);
        fetchChallenges();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (defaultAdapter != null) {
            if (defaultAdapter.isEnabled())
                defaultAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    private void fetchChallenges() {
        class ReceiveChallengesCallback implements ApiAsyncTask.ReceiveCallback {

            @Override
            public void execute(String result, Activity activity) {
                try {
                    ((CommentActivity)activity).receiveChallenges(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        new ApiAsyncTask(this, url, new ReceiveChallengesCallback(), "GET", "challenges", null, getLoginHeaders()).execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PHOTO_TAKEN) {
            if (data == null) return;

            String photo = data.getStringExtra("photo");

            this.mime = "image/jpeg";
            currentPhoto = photo;
            currentAttachment = null;
            findViewById(R.id.removeAttachment).setVisibility(View.VISIBLE);

        } else if (requestCode == SELECT_FILE) {
            if (data == null) return;

            ContentResolver cR = this.getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(cR.getType(data.getData()));
            this.mime = mime.getMimeTypeFromExtension(type);

            try {

                File attachment = File.createTempFile("attachment", null, getCacheDir());

                InputStream in = getContentResolver().openInputStream(data.getData());
                try {
                    OutputStream out = new FileOutputStream(attachment);
                    try {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }

                currentAttachment = attachment.getPath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            findViewById(R.id.removeAttachment).setVisibility(View.VISIBLE);
        }
    }


    public void receiveChallenges(String result) throws JSONException {
        JSONArray jsonArray = new JSONArray(result);
        String[] challengesValues = new String[jsonArray.length()];
        challenges = new HashMap<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            challenges.put(jsonObject.getString("id"), jsonObject.getString("title"));
            challengesValues[i] = jsonObject.getString("title");
        }

        final Spinner spinner = findViewById(R.id.spinner);
        View spinnerArrow = findViewById(R.id.spinnerArrow);

        currentChallenge = (String) challenges.keySet().toArray()[0];

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(CommentActivity.this, R.layout.spinner_item, challengesValues);

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                currentChallenge = (String) challenges.keySet().toArray()[position];
                findViewById(R.id.commentEditor).setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                currentChallenge = null;
                findViewById(R.id.commentEditor).setVisibility(View.GONE);
            }

        });
        spinnerArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spinner.performClick();
            }
        });

        final CommentActivity scope = this;
        findViewById(R.id.photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent intent = new Intent(scope, PhotoActivity.class);
            intent.putExtra("URL", scope.url);
            startActivityForResult(intent, PHOTO_TAKEN);
            }
        });

        findViewById(R.id.attach).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("*/*");
            startActivityForResult(Intent.createChooser(chooseFile, "Choose a file"), SELECT_FILE);
            }
        });

        findViewById(R.id.comment_submit).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                scope.sendRequest();
            }
        });

        findViewById(R.id.removeAttachment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentAttachment = null;
                currentPhoto = null;
                findViewById(R.id.removeAttachment).setVisibility(View.GONE);
            }
        });
    }

    private void sendRequest() {
        HashMap<String, String> params = new HashMap<>();
        params.put("challenge", currentChallenge);
        params.put("title", ((EditText)findViewById(R.id.title)).getText().toString());
        params.put("body", ((EditText)findViewById(R.id.body)).getText().toString());
        params.put("mime", mime);
        params.put("path", currentPhoto != null ? currentPhoto : currentAttachment);

        class PostComment implements ApiAsyncTask.ReceiveCallback {

            @Override
            public void execute(String result, Activity activity) {
                ProgressDialog pd = new ProgressDialog(activity);
                pd.setMessage("Your comment was successfully registered");
                pd.setCancelable(true);
                pd.show();
                clearView();
            }
        }

        new ApiAsyncTask(this, url, new PostComment(), "POST", "comment", params, getLoginHeaders()).execute();
    }

    private void clearView() {
        currentPhoto = null;
        currentAttachment = null;
        EditText body = findViewById(R.id.body);
        body.setText("");
        EditText title = findViewById(R.id.title);
        title.setText("");
        findViewById(R.id.removeAttachment).setVisibility(View.GONE);
    }
}
