package me.chenfuduo.myviewdraghelperusage.drag;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by Administrator on 2015/6/29.
 */
public class DragLayout extends FrameLayout {


    private ViewDragHelper dragHelper;

    private ViewGroup mLeftContent;
    private ViewGroup mMainContent;//红色

    int mHeight;
    int mWidth;

    int mRange;
    private onDragStatusChangedListener listener;

    Status mStatus = Status.Close;

    /**
     * 状态枚举
     */
    public enum Status {
        Close, Open, Draging
    }

    public interface onDragStatusChangedListener {
        void onClose();

        void onOpen();

        void onDraging(float percent);
    }

    public void setDragStatusListener(onDragStatusChangedListener listener) {
        this.listener = listener;
    }

    public Status getStatus() {
        return mStatus;
    }

    public void setStatus(Status mStatus) {
        this.mStatus = mStatus;
    }

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //创建ViewDragHelper的静态工厂方法有两个，一个带float sensitivity参数，一个不带，这个参数的默认值是1.0f
        //第一个参数是父view，也就是这里的DragLayout，给个this即可
        //第二个参数(若有),是灵敏度，默认值是1.0f
        //第三个参数是回调接口
        //ViewDragHelper.create(this,1.0f,mCallback);
        dragHelper = ViewDragHelper.create(this, mCallback);
    }

    /**
     * 该接口默认实现了public boolean tryCaptureView(View child, int pointerId)方法
     * 当然还需要实现很多其他的方法
     */
    private ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {
        /**
         * 根据返回结果决定当前child是否可以拖拽
         * @param child 当前被拖拽的view
         * @param pointerId 区分多点触摸的id
         * @return
         */
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            Log.e("Test", "child view：" + child.toString());
            //  return child == mMainContent;
            return true;
        }

        /**
         * 当capturedChild被捕获的时候调用
         * @param capturedChild
         * @param activePointerId
         */
        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            Log.d("Test", "onViewCaptured--->capturedChild:" + capturedChild);
            super.onViewCaptured(capturedChild, activePointerId);
        }


        /**
         * 返回拖拽的范围，不对拖拽进行真正的限制，仅仅决定了动画执行速度
         * @param child
         * @return
         */
        @Override
        public int getViewHorizontalDragRange(View child) {
            return mRange;
        }

        /**
         * 根据建议值修正将要移动到的位置(水平)
         * 此时没有发生真正的移动
         * @param child 当前拖拽的view
         * @param left 新的位置的建议值 left = oldLeft + dx;  其中oldLeft = child.getLeft().
         * @param dx 位置变化量
         * @return
         */
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            super.clampViewPositionHorizontal(child, left, dx);
            //向左  负的
            // Log.e("Test","left:" + left + "  dx:" + dx);

            if (child == mMainContent) {
                left = fixLeft(left);
            }
            return left;
        }

      /*  @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            super.clampViewPositionVertical(child, top, dy);
            //向上  负Log.e("Test","top:" + top + "  dy:" + dy);
            return top;
        }*/


        /**
         * 当View位置改变的时候，处理要做的事情(更新状态，伴随动画，重绘界面)
         * @param changedView
         * @param left
         * @param top
         * @param dx
         * @param dy
         */
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);

            int newLeft = left;

            if (changedView == mLeftContent) {
                //把当前的变化量传递给mMainContent
                newLeft = mMainContent.getLeft() + dx;
            }

            newLeft = fixLeft(newLeft);

            if (changedView == mLeftContent) {
                //当左面板移动之后，再强制放回去
                mLeftContent.layout(0, 0, 0 + mWidth, 0 + mHeight);
                mMainContent.layout(newLeft, 0, newLeft + mWidth, 0 + mHeight);
            }
            dispatchDragEvent(newLeft);
            //为了兼容低版本，每次修改值，进行重绘
            invalidate();
        }

        /**
         * 当View被释放的时候，执行动画
         * @param releasedChild 被释放的View
         * @param xvel 水平方向的速度  向右为正
         * @param yvel 竖直方向的速度  向下为正
         */
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (xvel == 0 && mMainContent.getLeft() > mRange / 2.0f) {
                open();
            } else if (xvel > 0) {
                open();
            } else {
                close();
            }
        }
    };

    /**
     * 更新状态，执行动画
     *
     * @param newLeft
     */
    public void dispatchDragEvent(int newLeft) {
        float percent = newLeft * 1.0f / mRange;

        if (listener!=null){
            listener.onDraging(percent);
        }

        //上一个状态
        Status preStatus = mStatus;

        //更新状态，执行回调
        mStatus = updateStatus(percent);
        if (mStatus != preStatus){
            //状态发生变化
            if (mStatus == Status.Close){
                if (listener != null){
                    listener.onClose();
                }
            }else if (mStatus == Status.Open){
                if (listener != null){
                    listener.onOpen();
                }
            }

        }
        animViews(percent);
    }

    private Status updateStatus(float percent) {
        if (percent == 0.0f) {
            return Status.Close;
        } else if (percent == 1.0f) {
            return Status.Open;
        }
        return Status.Draging;
    }

    private void animViews(float percent) {
        //左面板缩放动画
        //mLeftContent.setScaleX(0.5f + 0.5f*percent);
        //上面注释的效果和下面的一模一样
        mLeftContent.setScaleX(evaluate(percent, 0.5f, 1.0f));
        mLeftContent.setScaleY(0.5f + 0.5f * percent);

        //左面板平移动画
        mLeftContent.setTranslationX(evaluate(percent, -mWidth / 2.0f, 0));


        //左面板透明度变化
        mLeftContent.setAlpha(evaluate(percent, 0.5f, 1.0f));

        //主面板缩放动画
        mMainContent.setScaleX(evaluate(percent, 1.0f, 0.8f));
        mMainContent.setScaleY(evaluate(percent, 1.0f, 0.8f));

        //背景：亮度变化(颜色变化)
        getBackground().setColorFilter((Integer) evaluateColor(percent, Color.BLACK, Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
    }

    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }

    public Object evaluateColor(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int) ((startA + (int) (fraction * (endA - startA))) << 24) |
                (int) ((startR + (int) (fraction * (endR - startR))) << 16) |
                (int) ((startG + (int) (fraction * (endG - startG))) << 8) |
                (int) ((startB + (int) (fraction * (endB - startB))));
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        //持续平滑动画(高频率调用)
        if (dragHelper.continueSettling(true)) {
            //如果返回true，动画还需要继续执行
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void close() {
        close(true);
    }

    /**
     * 关闭
     */
    public void close(boolean isSmooth) {
        int finalLeft = 0;
        if (isSmooth) {
            //触发一个平滑的动画
            if (dragHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)) {

                //返回true表示还没有移动到指定的位置，需要刷新界面
                //参数传this,(child所在的ViewGroup)
                ViewCompat.postInvalidateOnAnimation(this);

            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }


    public void open() {
        open(true);
    }

    /**
     * 打开
     */
    public void open(boolean isSmooth) {
        int finalLeft = mRange;
        if (isSmooth) {
            //触发一个平滑的动画
            if (dragHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)) {

                //返回true表示还没有移动到指定的位置，需要刷新界面
                //参数传this,(child所在的ViewGroup)
                ViewCompat.postInvalidateOnAnimation(this);

            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }

    /**
     * 根据范围修正左边值
     *
     * @param left
     * @return
     */
    private int fixLeft(int left) {
        if (left < 0) {
            return 0;
        } else if (left > mRange) {
            return mRange;
        }
        return left;
    }

    //下面重写的方法[onInterceptTouchEvent,onTouchEvent]可以理解为传递触摸事件(没有重写dispatchTouchEvent)

    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }*/

    /**
     * 将事件拦截下来,相当于把自定义控件的事件交给ViewDragHelper去处理
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //传递给ViewDragHelper
        return dragHelper.shouldInterceptTouchEvent(ev);
    }

    /**
     * 将拦截下来的事件做处理
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //对多点触摸有点问题
        try {
            dragHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回true，持续接收事件
        return true;
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLeftContent = (ViewGroup) getChildAt(0);
        mMainContent = (ViewGroup) getChildAt(1);
    }


    /**
     * 当尺寸有变化的时候调用,在onMeasure(...)方法后调用
     *
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();
        mRange = (int) (mWidth * 0.6);
    }
}
