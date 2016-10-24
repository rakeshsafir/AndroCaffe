package com.gputech.androcaffe;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;

class CaffeModelFilter implements FileFilter {
    @Override
    public boolean accept(File f) {
        if ( f.isHidden() || !f.canRead() ) {
            return false;
        }

        if ( f.isDirectory() ) {
            return false;
        }
        return checkFileExtension( f );
    }

    private boolean checkFileExtension( File f ) {
        String ext = getFileExtension(f);
        if ( ext == null) return false;
        try {
            if ( SupportedFileFormat.valueOf(ext.toUpperCase()) != null ) {
                return true;
            }
        } catch(IllegalArgumentException e) {
            //Not known enum value
            return false;
        }
        return false;
    }

    private boolean checkFileExtension( String fileName ) {
        String ext = getFileExtension(fileName);
        if ( ext == null) return false;
        try {
            if ( SupportedFileFormat.valueOf(ext.toUpperCase()) != null ) {
                return true;
            }
        } catch(IllegalArgumentException e) {
            //Not known enum value
            return false;
        }
        return false;
    }
    public String getFileExtension( File f ) {
        return getFileExtension( f.getName() );
    }

    public String getFileExtension( String fileName ) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i+1);
        } else
            return null;
    }
    public enum SupportedFileFormat {
        MODEL("caffemodel"),
        PROTOTXT("prototxt"),
        BINPROTO("binaryproto"),
        TXT("txt");

        private String filesuffix;

        SupportedFileFormat(String filesuffix) {
            this.filesuffix = filesuffix;
        }

        public String getFilesuffix() {
            return filesuffix;
        }
    }
}

public class MainActivity extends AppCompatActivity {
    enum Status {
        STATUS_SUCCESS,
        STATUS_ACF_SO_LOAD_FAILED,
        STATUS_LC_SO_LOAD_FAILED,
        STATUS_DPXT_LOAD_FAILED,

        STATUS_UNKNOWN
    }
    /* JNI Interfaces */
    public static native int jniCaffeInit(String model_file,
                                          String trained_file,
                                          String mean_file,
                                          String label_file);

    public static native int jniDoClassify(String imgPath, int info[]);

    public static native void jniCaffeDeInit();

    /* UI Components */
    EditText    caffeModelPath;
    EditText    caffeTrainPath;
    EditText    caffeMeanPath;
    EditText    caffeLabelPath;
    EditText    caffeImagePath;
    TextView    resultTextView;

    protected static final String TAG = "AndroCaffeActivity";
    private int SELECT_MODEL_FILE = 1;
    private int SELECT_TRAIN_FILE = 2;
    private int SELECT_MEAN_FILE = 3;
    private int SELECT_LABEL_FILE = 4;
    private final Context mContext = MainActivity.this;
    private Status mStatus = Status.STATUS_UNKNOWN;
    final int info[] = new int[3]; // Width, Height, Execution time (ms)

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

    /* Copies file from assets directory to internal data dir */
    private boolean copyAssetToData(final String f) {
        boolean ret = true;
        InputStream in;
        try {
            in = getAssets().open(f);
            final File of = new File(getDir("execdir", MODE_PRIVATE), f);

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

    /* Replaces an exisiting training data set 'if present' with a new one from absPath */
    private boolean initCaffeTrainSet() {
        boolean ret = false;

        String model_file, trained_file, mean_file, label_file;

        copyAssetToData
        File baseDir= new File(absPath);

        File[] caffeFiles;
        do {

            if(true != baseDir.exists()) {
                break;
            }

            caffeFiles = baseDir.listFiles(new CaffeModelFilter());
            /* FIXME : Query based on file extension */
            model_file = absPath;
            model_file += "/deploy.prototxt";

            trained_file = absPath;
            trained_file += "/bvlc_reference_caffenet.caffemodel";

            mean_file = absPath;
            mean_file += "/imagenet_mean.binaryproto";

            label_file = absPath;
            label_file += "/synset_words.txt";

            if(0 != jniCaffeInit(model_file, trained_file, mean_file, label_file)) {
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

        caffeModelPath = (EditText) findViewById(R.id.caffeModelText);
        caffeTrainPath = (EditText) findViewById(R.id.caffeTrainText);
        caffeMeanPath = (EditText) findViewById(R.id.caffeMeanText);
        caffeLabelPath = (EditText) findViewById(R.id.caffeLabelText);
        caffeImagePath = (EditText) findViewById(R.id.caffeImageText);
        resultTextView = (TextView) findViewById(R.id.caffeResultTextView);

        /* Install On Click Listeners */
        caffeModelPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                Log.d("path", file.toString());

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setDataAndType(Uri.fromFile(file), "Caffe/prototxt");

                try {
                    startActivityForResult(i, SELECT_MODEL_FILE);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(mContext, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
                }
            }
        });

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
            if (0 == jniDoClassify(caffeImagePath.getText().toString(), info)) {
                resultTextView.setText("Image(" + info[0] + "px," + info[1] + "px). Processing time:" + info[3] + "ms");
//                imageView.setImageBitmap(inBmp);

                jniCaffeDeInit();
            }
        }catch (Exception e) {
            jniCaffeDeInit();
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

        if(resultCode == RESULT_OK && data != null) {
            Uri pickedImage = data.getData();
            Toast.makeText(mContext, "Select image...", Toast.LENGTH_LONG).show();
            String[] filePath = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(pickedImage, filePath, null, null, null);
            cursor.moveToFirst();
            String modelPath = cursor.getString(cursor.getColumnIndex(filePath[0]));
            switch (requestCode) {
                case SELECT_MODEL_FILE:
                    caffeModelPath.setText(modelPath);
                    break;
                default:
                    Log.w(TAG, "Invalid request code" + requestCode);
                    break;
            }
        }
    }
}
