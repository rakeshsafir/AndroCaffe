package com.gputech.androcaffe;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    enum Status {
        STATUS_SUCCESS,
        STATUS_ACF_SO_LOAD_FAILED,
        STATUS_LC_SO_LOAD_FAILED,
        STATUS_DPXT_LOAD_FAILED,

        STATUS_UNKNOWN
    }
    /* JNI Interfaces */
    public static native int jniCaffeInit();
    public static native int jniDoClassify(Bitmap bmpIn, int info[]);


    /* UI Components */
    TextView    textView;
    ImageView   imageView;

    protected static final String TAG = "AndroCaffeActivity";
    private int SELECT_PHOTO = 1;
    private final Context mContext = MainActivity.this;
    private Status mStatus = Status.STATUS_UNKNOWN;
    private Bitmap inBmp;
    private Bitmap outBmp;
    final int info[] = new int[3]; // Width, Height, Execution time (ms)
    private String dbDirPath;

    private boolean loadNativeLib(final String lib) {
        boolean ret = true;
        try {
            System.load(lib);
            Log.d( TAG, "Loaded file =" + lib);
        } catch (UnsatisfiedLinkError err) {
            ret = false;
            Log.e( TAG, "Failed to load file =" + lib);
        }
        return ret;
    }

    /* Replaces an exisiting training data set 'if present' with a new one from absPath */
    private boolean importCaffeTrainSet(final String absPath) {
        boolean ret = true;
        Toast.makeText(mContext, absPath, Toast.LENGTH_SHORT).show();
        return ret;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textMessage);
        imageView = (ImageView) findViewById(R.id.imageView);

        do {
            String nativeLib = mContext.getApplicationInfo().nativeLibraryDir;
            nativeLib += "/libandrocaffe.so";

            if (true != loadNativeLib(nativeLib)) {
                mStatus = Status.STATUS_ACF_SO_LOAD_FAILED;
                break;
            }

            nativeLib = mContext.getApplicationInfo().nativeLibraryDir;
            nativeLib += "/libcaffe.so";
            if (true != loadNativeLib(nativeLib)) {
                mStatus = Status.STATUS_LC_SO_LOAD_FAILED;
                break;
            }

            mStatus = Status.STATUS_SUCCESS;

        }while(false);
    }

    public void doClassify(View v) {
        if(mStatus != Status.STATUS_SUCCESS) {
            Toast.makeText(mContext, mStatus.toString(), Toast.LENGTH_LONG).show();
            return;
        }
        try {
            /* Execute JNI Processing Layer */
            if (0 == jniDoClassify(inBmp, info)) {
                textView.setText("Image(" + info[0] + "px," + info[1] + "px). Processing time:" + info[3] + "ms");
                imageView.setImageBitmap(inBmp);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doSelectTrainingData(View v) {
        if(mStatus != Status.STATUS_SUCCESS) {
            Toast.makeText(mContext, mStatus.toString(), Toast.LENGTH_LONG).show();
            return;
        }
        Arrays.fill(info, 0);

        importCaffeTrainSet("/storage/ECC9-5128/Caffe");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK && data != null) {

            Uri pickedImage = data.getData();
            Toast.makeText(mContext, "Select image...", Toast.LENGTH_LONG).show();
            String[] filePath = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(pickedImage, filePath, null, null, null);
            cursor.moveToFirst();
            String imagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try {
                inBmp = BitmapFactory.decodeFile(imagePath, options);
                info[0] = inBmp.getWidth();
                info[1] = inBmp.getHeight();
                imageView.setImageBitmap(inBmp);
            }catch (Exception e) {
                e.printStackTrace();
                cursor.close();
                return;
            }
            cursor.close();
        }
    }
}
