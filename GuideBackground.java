package com.mobvoi.tickids.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.tencent.qcloud.uikit.common.component.video.util.LogUtil;

public class GuideBackground extends View {
    private static final String TAG = "TAG";
    private Guide.GuideParams params;
    private Paint paint;
    private boolean isNeedInit = true;
    private int mCurrIndex = 0;
    private GuideListener guideListener;
    private TextPaint textPaint;

    public GuideBackground(Context context) {
        super(context);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        textPaint = new TextPaint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(15 * getContext().getResources().getDisplayMetrics().density);
        textPaint.setAntiAlias(true);
    }

    /**
     * 设置Guide参数
     *
     * @param params
     */
    public void setGuideParams(Guide.GuideParams params) {
        this.params = params;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(params.width == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : params.width,
                params.height == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : params.height);
        setLayoutParams(lp);
        mCurrIndex = 0;
    }

    /**
     * 初始化所有targetView的位置信息，在onDraw初始的原因是控件都已经测量完毕
     */
    private void initRect() {
        if (!isNeedInit)
            return;
        isNeedInit = false;
        if (params.views != null && params.views.size() > 0) {
            int i = 0;
            for (Guide.ViewParams viewParams : params.views) {
                if (viewParams == null || viewParams.targetView == null)
                    continue;
                int[] locations = new int[2];
                viewParams.targetView.getLocationOnScreen(locations);
                int width = viewParams.targetView.getMeasuredWidth();
                int height = viewParams.targetView.getMeasuredHeight();
                if (i == 4) {
                    viewParams.rect = new RectF(locations[0], locations[1], locations[0] + width, locations[1] + height + 135);
                } else {
                    viewParams.rect = new RectF(locations[0], locations[1], locations[0] + width, locations[1] + height);
                }
                i++;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (params == null)
            return;
        initRect();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        //绘制背景
        int layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, null, Canvas.ALL_SAVE_FLAG);
        paint.setColor(params.backgroundColor);
        canvas.drawRect(0, 0, canvasWidth, canvasHeight, paint);

        if (params.views == null || params.views.size() <= 0) {
            canvas.restoreToCount(layerId);
            return;
        }
        if (mCurrIndex >= params.views.size()) {
            canvas.restoreToCount(layerId);
            return;
        }
        if (mCurrIndex == 2) {
            mCurrIndex = 3;
        }
        Guide.ViewParams p = this.params.views.get(mCurrIndex);
        if (params.oneByOne) {
            //只绘制当前引导
            if (mCurrIndex == 1) {
                drawCombineGuide(p, canvas, params.views.get(2));
            } else {
                drawGuide(p, canvas);
            }
        } else {
            //绘制所有引导
            for (Guide.ViewParams pm : params.views) {
                drawGuide(pm, canvas);
            }
        }
        canvas.restoreToCount(layerId);
    }

    /**
     * 绘制引导图层和挖洞，情况为索引为2的时候，挖洞牵扯到两次挖洞组合
     *
     * @param p ： 当前引导参数
     */
    private void drawCombineGuide(Guide.ViewParams p, Canvas canvas, Guide.ViewParams pm) {
        if (p == null)
            return;
        drawCombineRect(canvas, p.rect, pm.rect, p);
        if (p.guideRes == 0)
            return;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), p.guideRes);
        LogUtil.d(TAG, "图片宽是：" + bitmap.getWidth());
        LogUtil.d(TAG, "图片高是：" + bitmap.getHeight());
        if (bitmap == null)
            return;
        float x = canvas.getWidth() / 2 - bitmap.getWidth() / 6;
        float y = p.rect.top > canvas.getHeight() - p.rect.bottom ?
                p.rect.top - bitmap.getHeight() + p.offY :
                p.rect.bottom + p.offY + 5;
        canvas.drawBitmap(bitmap, x, y, null);
        bitmap.recycle();
        bitmap = null;
    }

    /**
     * 绘制引导图层和挖洞
     *
     * @param p ： 当前引导参数
     */
    private void drawGuide(Guide.ViewParams p, Canvas canvas) {
        if (p == null)
            return;
        drawRect(canvas, p.rect, p);
        //不同索引不同引导显示
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), p.guideRes);
        LogUtil.d(TAG, "图片宽是：" + bitmap.getWidth());
        LogUtil.d(TAG, "图片高是：" + bitmap.getHeight());
        if (p.guideRes == 0)
            return;
        if (bitmap == null)
            return;
        float x = 0;
        float y = 0;
        if (mCurrIndex == 0) {
            x = p.rect.right - bitmap.getWidth();
            y = p.rect.bottom - 15;
        } else if (mCurrIndex == 3) {
            x = p.rect.left - bitmap.getWidth() + 10;
            y = (p.rect.top + p.rect.bottom) / 2 - (p.rect.bottom - p.rect.top) / 2;
        } else if (mCurrIndex == 4) {
            x = p.rect.left + (p.rect.right - p.rect.left) / 9;
            y = p.rect.top > canvas.getHeight() - p.rect.bottom ?
                    p.rect.top - bitmap.getHeight() + p.offY + 15 :
                    p.rect.bottom + p.offY + 15;
        }
        canvas.drawBitmap(bitmap, x, y, null);
        bitmap.recycle();
        bitmap = null;
    }

    /**
     * 挖矩形洞
     *
     * @param rect ： targetView位置信息
     * @param
     */
    private void drawRect(Canvas canvas, RectF rect, Guide.ViewParams params) {
        if (rect == null || canvas == null)
            return;
        //挖洞
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(rect, params.xRadiu, params.yRadiu, paint);
        paint.setXfermode(null);
    }

    private void drawCombineRect(Canvas canvas, RectF rect, RectF rectFRound, Guide.ViewParams params) {
        if (rect == null || canvas == null)
            return;
        //挖洞
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(rect, params.xRadiu, params.yRadiu, paint);
        canvas.drawCircle((rectFRound.left + rectFRound.right) / 2
                , (rectFRound.top + rectFRound.bottom) / 2
                , (rectFRound.right - rectFRound.left) / 2, paint);
        paint.setXfermode(null);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        isNeedInit = true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.e(TAG, "dispatchTouchEvent: " + (MotionEvent.ACTION_DOWN == ev.getAction()));
        if (params == null) {
            if (MotionEvent.ACTION_DOWN == ev.getAction())
                onNext();
            return true;
        }
        //如果除了targetView也可以触发事件，点击任意区域都将进入下一个引导
        if (params.outsideTouchable) {
            if (MotionEvent.ACTION_DOWN == ev.getAction())
                onNext();
            return true;
        }
        boolean touchable = false;
        //如果是oneByOne只有点击当前targetView才可以触发事件，进入下一个引导
        if (params.oneByOne) {
            if (params.views != null && params.views.size() > 0 && mCurrIndex < params.views.size()) {
                Guide.ViewParams p = this.params.views.get(mCurrIndex);
                if (p != null && p.rect != null)
                    if (ev.getX() > p.rect.left && ev.getX() < p.rect.right
                            && ev.getY() > p.rect.top && ev.getY() < p.rect.bottom) {
                        touchable = true;
                    }
            }
        } else {
            //如果不是oneByOne点击所有targetView都可以触发事件，进入下一个引导
            if (params.views != null && params.views.size() > 0) {
                for (Guide.ViewParams p : params.views) {
                    if (p == null || p.rect == null)
                        continue;
                    if (ev.getX() > p.rect.left && ev.getX() < p.rect.right
                            && ev.getY() > p.rect.top && ev.getY() < p.rect.bottom) {
                        touchable = true;
                        break;
                    }
                }
            }
        }

        if (MotionEvent.ACTION_DOWN == ev.getAction() && touchable)
            onNext();

        return true;
    }

    /**
     * 触发事件，下一步引导或者结束引导
     */
    private void onNext() {
        if (params == null || !params.oneByOne || params.views == null) {
            if (guideListener != null)
                guideListener.onFinish();
        } else if (mCurrIndex < params.views.size() - 1) {
            mCurrIndex++;
            invalidate();
            if (guideListener != null)
                guideListener.onNext(mCurrIndex);
        } else {
            if (guideListener != null)
                guideListener.onFinish();
        }
    }

    /**
     * 设置监听
     */
    public void setGuideListener(GuideListener guideListener) {
        this.guideListener = guideListener;
    }

    public interface GuideListener {
        void onNext(int index);

        void onFinish();
    }
}
