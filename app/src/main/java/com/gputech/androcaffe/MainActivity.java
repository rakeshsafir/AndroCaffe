package com.gputech.androcaffe;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    public static native int jniDoClassify(String imgPath, int info[]);
    public static native void jniCaffeDeInit();

    /* UI Components */
    TextView caffeModelPath;
    TextView caffeTrainPath;
    TextView caffeMeanPath;
    TextView caffeLabelPath;
    TextView caffeImgPath;
    TextView caffeResultText;

    protected static final String TAG = "AndroCaffeActivity";
    private final int SELECT_MODEL_FILE = 1;
    private final int SELECT_TRAIN_FILE = 2;
    private final int SELECT_MEAN_FILE = 3;
    private final int SELECT_LABEL_FILE = 4;
    private final int SELECT_IMAGE_FILE = 5;
    private final Context mContext = MainActivity.this;
    private Status mStatus = Status.STATUS_UNKNOWN;
    final int info[] = new int[3]; // Width, Height, Execution time (ms)

    private boolean loadNativeLib(final String lib) {
        boolean ret = true;
        try {
            System.load(lib);
            Log.d( TAG, "Loaded file =[" + lib + "]" );
        } catch (UnsatisfiedLinkError err) {
            ret = false;
            Log.e( TAG, "Failed to load file =[" + lib + "]" );
        }
        return ret;
    }

    /* Imports an external Caffe Trained model into internal data directory */
    private boolean copyExtFileToData(final String destFName, final String srcFPath, boolean force) {
        boolean ret = true;

        try {
            /* Check if file exists */
            File f = new File(getDir("execdir", MODE_PRIVATE), destFName);
            if(f.exists() && force && f.delete()) {
                Log.i(TAG, "Deleted existing " + f.getAbsolutePath());
            } else {
                Log.e(TAG, "Cannot replace file " + f.getAbsolutePath());
                return false;
            }

            FileInputStream in = new FileInputStream(srcFPath);
            final File of = new File(getDir("execdir", MODE_PRIVATE), destFName);
            final OutputStream out = new FileOutputStream(of);

            final byte b[] = new byte[65535];
            int sz = 0;
            while ((sz = in.read(b)) > 0) {
                out.write(b, 0, sz);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        }
        return ret;
    }

    /* Replaces an exisiting training data set 'if present' with a new one from selected paths */
    private boolean importCaffeTrainSet() {
        boolean ret = false;
        do {
            File f = new File(caffeModelPath.getText().toString());
            if(f.exists() && copyExtFileToData("caffe.prototxt", caffeModelPath.getText().toString(), true)) {
                Log.e( TAG, "Failed to load Model(prototxt)=" + caffeModelPath.getText().toString());
                break;
            }

            f = new File(caffeTrainPath.getText().toString());
            if(f.exists() && copyExtFileToData("caffe.caffemodel", caffeTrainPath.getText().toString(), true)) {
                Log.e( TAG, "Failed to load Training data(caffemodel)=" + caffeTrainPath.getText().toString());
                break;
            }

            f = new File(caffeMeanPath.getText().toString());
            if(f.exists() && copyExtFileToData("caffe.binaryproto", caffeMeanPath.getText().toString(), true)) {
                Log.e( TAG, "Failed to load Mean file(binaryproto)=" + caffeMeanPath.getText().toString());
                break;
            }

            f = new File(caffeLabelPath.getText().toString());
            if(f.exists() && copyExtFileToData("caffe.txt", caffeLabelPath.getText().toString(), true)) {
                Log.e( TAG, "Failed to load Label file(txt)=" + caffeLabelPath.getText().toString());
                break;
            }
            ret = true;
        }while(false);

        return ret;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        caffeModelPath = (TextView) findViewById(R.id.caffeModelText);
        caffeTrainPath = (TextView) findViewById(R.id.caffeTrainText);
        caffeMeanPath = (TextView) findViewById(R.id.caffeMeanText);
        caffeLabelPath = (TextView) findViewById(R.id.caffeLabelText);
        caffeImgPath = (TextView) findViewById(R.id.caffeImageText);
        caffeResultText = (TextView) findViewById(R.id.caffeResultText);

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
        if(0 != jniCaffeInit()) {
            Toast.makeText(mContext, "Caffe Failed to init...", Toast.LENGTH_LONG).show();
            return;
        }
        if(mStatus != Status.STATUS_SUCCESS) {
            Toast.makeText(mContext, mStatus.toString(), Toast.LENGTH_LONG).show();
            return;
        }
        Arrays.fill(info, 0);
        try {

            if(true != importCaffeTrainSet()) {
                Log.e( TAG, "Failed to import Caffe Training Data Set...");
            }
            /* Execute JNI Processing Layer */
            if (0 == jniDoClassify(caffeImgPath.getText().toString(), info)) {
                caffeResultText.setText("Image(" + info[0] + "px," + info[1] + "px). Processing time:" + info[3] + "ms");
            }
        }catch (Exception e) {
            jniCaffeDeInit();
            e.printStackTrace();
        }
    }

    public void doSelectModelFile(View v) {
        Log.d( TAG, "doSelectModelFile()");
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, SELECT_MODEL_FILE);
    }

    public void doSelectTrainFile(View v) {
        Log.d( TAG, "doSelectModelFile()");
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, SELECT_TRAIN_FILE);
    }

    public void doSelectMeanFile(View v) {
        Log.d( TAG, "doSelectModelFile()");
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, SELECT_MEAN_FILE);
    }

    public void doSelectLabelFile(View v) {
        Log.d( TAG, "doSelectModelFile()");
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, SELECT_LABEL_FILE);
    }

    public void doSelectImgFile(View v) {
        Log.d( TAG, "doSelectModelFile()");
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, SELECT_IMAGE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && data != null) {
            Uri file = data.getData();
            switch (requestCode) {
            case SELECT_MODEL_FILE:
                caffeModelPath.setText(file.getPath());
                break;
            case SELECT_TRAIN_FILE:
                caffeTrainPath.setText(file.getPath());
                break;
            case SELECT_MEAN_FILE:
                caffeMeanPath.setText(file.getPath());
                break;
            case SELECT_LABEL_FILE:
                caffeLabelPath.setText(file.getPath());
                break;
            case SELECT_IMAGE_FILE:
                caffeImgPath.setText(file.getPath());
                break;
            default:
                break;
            }
        }
    }
}
