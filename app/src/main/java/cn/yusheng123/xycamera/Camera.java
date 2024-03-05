package cn.yusheng123.xycamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Camera extends AppCompatActivity {
    private static final String TAG = "Camera";

    private static final SparseIntArray PHOTO_ORI = new SparseIntArray();

    static {
        PHOTO_ORI.append(Surface.ROTATION_0, 90);
        PHOTO_ORI.append(Surface.ROTATION_90, 0);
        PHOTO_ORI.append(Surface.ROTATION_180, 270);
        PHOTO_ORI.append(Surface.ROTATION_270, 180);
    }

    int cameraOri;
    int displayRotation;
    private CameraManager mCameraManager;
    private String cameraId;
    private List<Size> outputSizes;
    private Size photoSize;
    private Button btnPhoto;
    private AutoFitTextureView previewView;

    private CameraDevice cameraDevice;
    private int cameraOrientation;
    private CaptureRequest.Builder photoRequestBuilder;

    private Surface photoSurface;
    private CaptureRequest photoRequest;

    private CameraCaptureSession captureSession;
    public static final int PERMISSION_REQUEST_CODE = 200;
    private ImageReader photoReader;
    private int displayOrientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化CameraUnit对象
        CameraUtils.init(this);
        initCamera();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_main);
        btnPhoto = findViewById(R.id.btn_photo);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
        previewView = findViewById(R.id.preview_view);
    }

    private void takePhoto() {
        try {
            photoRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            cameraOrientation = PHOTO_ORI.get(displayRotation);
            photoRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,cameraOrientation);
            photoRequestBuilder.addTarget(photoSurface);
            photoRequest = photoRequestBuilder.build();
            captureSession.stopRepeating();
            captureSession.capture(photoRequest,sessionCaptureCallback,null);
            // 启动一个新线程来播放声音
            new Thread(this::shutterSound).start();

        } catch (CameraAccessException e) {
            Log.e(TAG, "takePhoto: 相机访问异常",e);
        }
    }

    private void initCamera(){
        mCameraManager = CameraUtils.getInstance().getCameraManager();
        cameraId = CameraUtils.getInstance().getBackCameraId();
        outputSizes = CameraUtils.getInstance().getCameraOutputSizes(cameraId, SurfaceTexture.class);
        photoSize = outputSizes.get(0);
    }


    @SuppressLint("MissingPermission")
    private void openCamera(){
        try {
            displayOrientation = this.getWindowManager().getDefaultDisplay().getOrientation();
            if(displayOrientation == Surface.ROTATION_0 || displayOrientation == Surface.ROTATION_180){
                previewView.setAspectRation(photoSize.getHeight(),photoSize.getWidth());
            }else {
                previewView.setAspectRation(photoSize.getWidth(),photoSize.getHeight());
            }
            configureTransform(previewView.getWidth(),previewView.getHeight());
            mCameraManager.openCamera(cameraId,cameraStateCallback,null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera: 相机访问异常",e);
        }
    }

    private void releaseCamera(){
        CameraUtils.getInstance().releaseImageReader(photoReader);
        CameraUtils.getInstance().releaseCameraSession(captureSession);
        CameraUtils.getInstance().releaseCameraDevice(cameraDevice);
    }

    private void configureTransform(int viewWidth,int viewHeight){
        if(null == previewView || null == photoSize){
            return;
        }
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, photoSize.getHeight(), photoSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if(Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation){
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / photoSize.getHeight(),
                    (float) viewWidth / photoSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180,centerX,centerY);
        }
        previewView.setTag(matrix);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(checkPermission()){
            if(previewView.isAvailable()){
                openCamera();
            }else {
                previewView.setSurfaceTextureListener(surfaceTextureListener);
            }
        }else {
            requestPermission();
        }
    }

    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    private boolean checkPermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "相机权限已授权", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "已拒绝相机权限", Toast.LENGTH_SHORT).show();
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        showMessageOkCancel("你应该授权相机权限，否则无法使用相机",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestPermission();
                                    }
                                });
                    }
                }
        }
    }

    private void showMessageOkCancel(String message, DialogInterface.OnClickListener okListener){
        new AlertDialog.Builder(Camera.this)
                .setMessage(message)
                .setPositiveButton("ok",okListener)
                .setNegativeButton("Cancel",null)
                .create()
                .show();
    }

    private void initReaderAndSurface(){
        photoReader = ImageReader.newInstance(photoSize.getWidth(), photoSize.getHeight(), ImageFormat.JPEG, 2);
        photoReader.setOnImageAvailableListener(photoReaderImgListener,null);
        photoSurface = photoReader.getSurface();
    }

    private void writeImageToFile() {
        String fileName = System.currentTimeMillis() + ".jpg";
        String filePath = Environment.getExternalStorageDirectory() + File.separator + "DCIM" +
                File.separator + "Camera" + File.separator + fileName;

        Image image = photoReader.acquireNextImage();
        if (image == null) {
            return;
        }

        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);

        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            Log.i(TAG,"图片写入成功");

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found for writing image data", e);
        } catch (IOException e) {
            Log.e(TAG, "Error accessing file", e);
        } finally {
            image.close();
        }
    }

    //播放快门声
    private void shutterSound(){
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.xycamera_shutter);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
    }

    /********* 监听器&回调 *******/
    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            configureTransform(width,height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    private Surface previewSurface;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;
    CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened: 相机已经启动");
            // 初始化ImageReader和Surface
            initReaderAndSurface();
            cameraDevice = camera;
            try {
                SurfaceTexture surfaceTexture = previewView.getSurfaceTexture();
                if(surfaceTexture == null){
                    return;
                }
                surfaceTexture.setDefaultBufferSize(photoSize.getWidth(),photoSize.getHeight());
                previewSurface = new Surface(surfaceTexture);
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(previewSurface);
                previewRequest = previewRequestBuilder.build();
                cameraDevice.createCaptureSession(Arrays.asList(previewSurface,photoSurface),sessionsStateCallback,null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "onOpened: 相机访问异常",e);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "onDisconnected: 相机已断开连接");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, "onError: 相机打开出错");
        }
    };

    CameraCaptureSession.StateCallback sessionsStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if(null == cameraDevice){
                return;
            }
            captureSession = session;
            try {
                captureSession.setRepeatingRequest(previewRequest,null,null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "onConfigured: 相机访问异常",e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    CameraCaptureSession.CaptureCallback sessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            try {
                captureSession.setRepeatingRequest(previewRequest,null,null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "onCaptureCompleted: 相机访问异常",e);
            }
        }
    };

    ImageReader.OnImageAvailableListener photoReaderImgListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            writeImageToFile();

        }
    };


}