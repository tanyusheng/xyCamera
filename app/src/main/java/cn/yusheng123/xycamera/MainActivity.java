package cn.yusheng123.xycamera;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Surface;

public class MainActivity extends AppCompatActivity {

    private static final SparseIntArray PHOTO_ORI = new SparseIntArray();

    static {
        PHOTO_ORI.append(Surface.ROTATION_0, 90);
        PHOTO_ORI.append(Surface.ROTATION_90, 0);
        PHOTO_ORI.append(Surface.ROTATION_180, 270);
        PHOTO_ORI.append(Surface.ROTATION_270, 180);
    }

    int cameraOri;
    int displayRotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    private void initCamera(){

    }
}