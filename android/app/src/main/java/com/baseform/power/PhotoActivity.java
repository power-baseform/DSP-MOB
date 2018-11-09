package com.baseform.power;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class PhotoActivity extends BaseActivity {

    private static final int REQUEST_TAKE_PHOTO = 11;
    private View actions;
    private View rotateLeft;
    private View rotateRight;
    private View submit;
    private String mCurrentPhotoPath;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        initBindings();

        imageView = findViewById(R.id.photoView);
        PhotoActivity scope = this;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ignored) {
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(scope, "com.baseform.power.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void initBindings() {
        actions = findViewById(R.id.actions);
        rotateLeft = findViewById(R.id.rotateLeft);
        rotateRight = findViewById(R.id.rotateRight);
        submit = findViewById(R.id.submit);

        rotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            rotateImage(mCurrentPhotoPath, false, null);
            }
        });

        rotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            rotateImage(mCurrentPhotoPath, true, null);
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent intent = new Intent();
            intent.putExtra("photo",  mCurrentPhotoPath);

            setResult(Activity.RESULT_OK,intent);
            finish();
        }
        });
    }


    public void rotateImage(String path, boolean clock, Integer force){
        File file = new File(path);
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        if (force != null) {
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + force);
        } else {
            if ((orientation == ExifInterface.ORIENTATION_NORMAL) | (orientation == 0)) {
                if (clock) {
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_90);
                } else {
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_270);
                }
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                if (clock) {
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_180);
                } else {
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_NORMAL);
                }
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                if (clock) {
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_270);
                } else {
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_90);
                }
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                if (clock) {
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_NORMAL);
                } else {
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_180);
                }
            }
        }
        try {
            exifInterface.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        imageView.setImageBitmap(getBitmap(path));
    }

    private Bitmap getBitmap(String path) {
        Log.e("inside of", "getBitmap = "+path);
        try {
            Bitmap b = null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            Matrix matrix = new Matrix();
            ExifInterface exifReader = new ExifInterface(path);
            int orientation = exifReader.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            int rotate = 0;
            if (orientation ==ExifInterface.ORIENTATION_NORMAL) {
                // Do nothing. The original image is fine.
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotate = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotate = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotate = 270;
            }
            matrix.postRotate(rotate);
            try {
                DisplayMetrics displaymetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int screenWidth = displaymetrics.widthPixels;
                int screenHeight = displaymetrics.heightPixels;
                b = loadBitmap(path, rotate, screenWidth, screenHeight);
            } catch (OutOfMemoryError e) {
            }
            System.gc();
            return b;
        } catch (Exception e) {
            Log.e("my tag", e.getMessage(), e);
            return null;
        }
    }


    public static Bitmap loadBitmap(String path, int orientation, final int targetWidth, final int targetHeight) {
        Bitmap bitmap = null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            int sourceWidth, sourceHeight;
            if (orientation == 90 || orientation == 270) {
                sourceWidth = options.outHeight;
                sourceHeight = options.outWidth;
            } else {
                sourceWidth = options.outWidth;
                sourceHeight = options.outHeight;
            }
            if (sourceWidth > targetWidth || sourceHeight > targetHeight) {
                float widthRatio = (float)sourceWidth / (float)targetWidth;
                float heightRatio = (float)sourceHeight / (float)targetHeight;
                float maxRatio = Math.max(widthRatio, heightRatio);
                options.inJustDecodeBounds = false;
                options.inSampleSize = (int)maxRatio;
                bitmap = BitmapFactory.decodeFile(path, options);
            } else {
                bitmap = BitmapFactory.decodeFile(path);
            }
            if (orientation > 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            sourceWidth = bitmap.getWidth();
            sourceHeight = bitmap.getHeight();
            if (sourceWidth != targetWidth || sourceHeight != targetHeight) {
                float widthRatio = (float)sourceWidth / (float)targetWidth;
                float heightRatio = (float)sourceHeight / (float)targetHeight;
                float maxRatio = Math.max(widthRatio, heightRatio);
                sourceWidth = (int)((float)sourceWidth / maxRatio);
                sourceHeight = (int)((float)sourceHeight / maxRatio);
                bitmap = Bitmap.createScaledBitmap(bitmap, sourceWidth, sourceHeight, true);
            }
        } catch (Exception e) {
        }
        return bitmap;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int screenWidth = displaymetrics.widthPixels;
            int screenHeight = displaymetrics.heightPixels;

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            int scaleFactor = Math.min(photoW/screenWidth, photoH/screenHeight);

            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;

            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            imageView.setImageBitmap(bitmap);

            actions.setVisibility(View.VISIBLE);
            rotateImage(mCurrentPhotoPath, true, ExifInterface.ORIENTATION_NORMAL);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


}
