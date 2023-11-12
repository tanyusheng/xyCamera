package cn.yusheng123.xycamera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraUtils {
    private static final String TAG = "CameraUtils";
    private static CameraUtils ourInstance = new CameraUtils();

    private static Context appContext;

    private static CameraManager cameraManager;

    //定义一个私有的构造函数，阻止了从类外部创建新的CameraUtils实例
    private CameraUtils(){

    }

    public static void init(Context context){
        if(appContext == null){
            // 由于该方法是静态的，将上下文生命周期与应用程序绑定，防止内存泄露
            appContext = context.getApplicationContext();
            cameraManager = (CameraManager) appContext.getSystemService(Context.CAMERA_SERVICE);
        }
    }
    
    // 通过单例模式创建实例，确保全局只有一个CameraUtils实例
    public static CameraUtils getInstance(){
        return ourInstance;
    }
    
    public CameraManager getCameraManager(){
        return cameraManager;
    }
    
    public String getFrontCameraId(){
        return getCameraId(true);
    }

    public String getBackCameraId(){
        return getCameraId(false);
    }


    /**
     * 获取相机ID
     * @param useFront 是否使用前置摄像头
     * @return 对应摄像头的ID，如果没找到符合要求的摄像头ID则返回null
     */
    public String getCameraId(boolean useFront){
        try {
            for (String cameraId:cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                //获取当前CameraId对应相机的朝向
                int cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(useFront){
                    if(cameraFacing == CameraCharacteristics.LENS_FACING_FRONT){
                        return cameraId;
                    }
                }else {
                    if(cameraFacing == CameraCharacteristics.LENS_FACING_BACK){
                        return cameraId;
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraId: 获取相机信息异常",e);
        }
        return null;
    }

    /**
     * 根据输出类指定相机的输出尺寸列表，按降序排列
     * @param cameraId 相机ID
     * @param clz 输出类
     * @return 相机输出尺寸列表
     */
    public List<Size> getCameraOutputSizes(String cameraId, Class clz){
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            List<Size> sizes = Arrays.asList(configs.getOutputSizes(clz));
            Collections.sort(sizes, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return o2.getWidth() * o2.getHeight() - o1.getWidth() * o1.getHeight();
                }
            });
            return sizes;

        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraOutputSizes: 获取相机信息异常", e);
        }
        return null;
    }

    public List<Size> getCameraOutputSizes(String cameraId,int format){
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return Arrays.asList(configs.getOutputSizes(format));
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraOutputSizes: 获取相机信息异常",e);
        }
        return null;
    }

    public void releaseCameraDevice(CameraDevice cameraDevice){
        if(cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    public void releaseCameraSession(CameraCaptureSession session){
        if(session != null){
            session.close();
            session = null;
        }
    }

    public void releaseImageReader(ImageReader reader){
        if(reader != null){
            reader.close();
            reader = null;
        }
    }

    public static class CompareSizesByArea implements Comparator<Size>{
        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long)o1.getWidth() * o1.getHeight() -
                    (long)o2.getWidth() * o2.getHeight());
        }
    }
}
