package cn.yusheng123.xycamera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

public class AutoFitTextureView extends TextureView {
    private int mRatioW = 0;
    private int mRatioH = 0;

    public AutoFitTextureView(Context context) {
        super(context);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置宽高比
     * @param width
     * @param height
     */
    public void setAspectRation(int width, int height){
        if (width < 0 || height < 0){
            throw new IllegalArgumentException("width or height can not be negative.");
        }

        mRatioW = width;
        mRatioH = height;
        //请求重新布局
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);

        if (0 == mRatioW || 0 == mRatioW) {
            //未设定宽高比，使用预览窗口默认宽高
            setMeasuredDimension(width, height);
        } else {
            //设定宽高比，调整预览窗口大小（调整后窗口大小不超过默认值）
            if (width < height * mRatioW / mRatioH) {
                setMeasuredDimension(width, width * mRatioH / mRatioW);
            } else {
                setMeasuredDimension(height * mRatioW / mRatioH, height);
            }
        }

    }
}
