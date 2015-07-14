title: "ViewDragHelper的用法2"
date: 2015-07-14 12:55:03
- 移动开发
- 自定义控件
- ViewDragHelper
categories: [移动开发]
---
**ViewDragHelper**是Google2013年IO大会提出来用于解决界面控件拖拽移动的问题(位于v4兼容包下)，最近在做QQ侧滑菜单那样的效果，用到了ViewDragHelper，做个笔记记录下。这部分是第二部分。[ViewDragHelper的第一部分](http://chenfuduo.me/2015/06/29/ViewDragHelper%E7%9A%84%E7%94%A8%E6%B3%95/)
![QQ侧滑菜单](http://1.infotravel.sinaapp.com/pic/40.gif)

其他的相关自定义控件：

> * [ListView的侧边栏快速索引](http://chenfuduo.me/2015/05/06/quick-index-listview/)
> * [类似于QQ空间的拖拽视差动画](http://chenfuduo.me/2015/05/13/ParallaxListView/)

<!--more-->



接着第一部分的继续，首先是Callback中的`onViewCaptured(View capturedChild, int activePointerId)`方法，该方法当capturedChild被捕获的时候调用。首先在其他的代码不变的情况下(`tryCaptureView(...)`返回true)
```java
 /**
         * 当capturedChild被捕获的时候调用
         * @param capturedChild
         * @param activePointerId
         */
        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            Log.d("Test","onViewCaptured--->capturedChild:" + capturedChild);
            super.onViewCaptured(capturedChild, activePointerId);
        }
```
那么这样两个View都是可以拖动的：
![1](http://1.infotravel.sinaapp.com/pic/80.PNG)
现在我们修改`tryCaptureView(...)`方法：
```java
 /**
         * 根据返回结果决定当前child是否可以拖拽
         * @param child 当前被拖拽的view
         * @param pointerId 区分多点触摸的id
         * @return
         */
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            Log.e("Test", "child view：" + child.toString());
            return child == mMainContent;
            //return true;
        }
```
`onViewCaptured(...)`方法保持不变。
![1](http://1.infotravel.sinaapp.com/pic/81.PNG)
ok,现在我们明白了`onViewCaptured(...)`方法的作用。

对`clampViewPositionHorizontal(...)`更深一层的认识。
```java
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
            return left;
        }
```
left新的位置的建议值`left = oldLeft + dx`，其中`oldLeft = child.getLeft()`.
那么`clampViewPositionVertical(...)`的原理一致。
接下来是`getViewHorizontalDragRange(View)`方法：
```java
        /**
         * 返回拖拽的范围，不对拖拽进行真正的限制，仅仅决定了动画执行速度
         * @param child
         * @return
         */
        @Override
        public int getViewHorizontalDragRange(View child) {
            return mRange;
        }
```
它返回拖拽的范围，不对拖拽进行真正的限制，仅仅决定了动画执行速度，在源码查看他的用法：
```java
 private int computeSettleDuration(View child, int dx, int dy, int xvel, int yvel) {
        xvel = clampMag(xvel, (int) mMinVelocity, (int) mMaxVelocity);
        yvel = clampMag(yvel, (int) mMinVelocity, (int) mMaxVelocity);
        final int absDx = Math.abs(dx);
        final int absDy = Math.abs(dy);
        final int absXVel = Math.abs(xvel);
        final int absYVel = Math.abs(yvel);
        final int addedVel = absXVel + absYVel;
        final int addedDistance = absDx + absDy;

        final float xweight = xvel != 0 ? (float) absXVel / addedVel :
                (float) absDx / addedDistance;
        final float yweight = yvel != 0 ? (float) absYVel / addedVel :
                (float) absDy / addedDistance;

        int xduration = computeAxisDuration(dx, xvel, mCallback.getViewHorizontalDragRange(child));
        int yduration = computeAxisDuration(dy, yvel, mCallback.getViewVerticalDragRange(child));

        return (int) (xduration * xweight + yduration * yweight);
    }
```
现在明白了它`仅仅决定了动画执行速度`的意义了。那么这个mRange怎么计算？在重写的`onSizeChanged(...)`方法中重写。
```java
    /**
     * 当尺寸有变化的时候调用,在onMeasure(...)方法后调用
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
```
`onSizeChanged(...)`方法在当尺寸有变化的时候调用,在onMeasure(...)方法后调用。


到这里为止，Demo没有任何的问题，新建一个2.3的模拟器，在该模拟器上运行本项目，发现居然不能拖动。这样就涉及到版本兼容的问题了。我们重写`onViewPositionChanged(...)`方法。
查看源码看看这个方法究竟干了什么：
```java
 private void dragTo(int left, int top, int dx, int dy) {
        int clampedX = left;
        int clampedY = top;
        final int oldLeft = mCapturedView.getLeft();
        final int oldTop = mCapturedView.getTop();
        if (dx != 0) {
            clampedX = mCallback.clampViewPositionHorizontal(mCapturedView, left, dx);
            mCapturedView.offsetLeftAndRight(clampedX - oldLeft);
        }
        if (dy != 0) {
            clampedY = mCallback.clampViewPositionVertical(mCapturedView, top, dy);
            mCapturedView.offsetTopAndBottom(clampedY - oldTop);
        }

        if (dx != 0 || dy != 0) {
            final int clampedDx = clampedX - oldLeft;
            final int clampedDy = clampedY - oldTop;
            mCallback.onViewPositionChanged(mCapturedView, clampedX, clampedY,
                    clampedDx, clampedDy);
        }
    }
```
我们发现了这样的代码：
```java
 mCallback.onViewPositionChanged(mCapturedView, clampedX, clampedY,
                    clampedDx, clampedDy);
```
其中`mCapturedView`就是我们拖动的View，再看看`clampedX`是什么：
```java
clampedX = mCallback.clampViewPositionHorizontal(mCapturedView, left, dx);
            mCapturedView.offsetLeftAndRight(clampedX - oldLeft);
```
发现了`mCapturedView`也就是我们拖动的View调用了View的`offsetLeftAndRight(int)`方法，那么在2.3版本和4.0以上的版本，这个方法肯定有蹊跷。
先看看高版本的：
```java
/**
     * Offset this view's horizontal location by the specified amount of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public void offsetLeftAndRight(int offset) {
        if (offset != 0) {
            final boolean matrixIsIdentity = hasIdentityMatrix();
            if (matrixIsIdentity) {
                if (isHardwareAccelerated()) {
                    invalidateViewProperty(false, false);
                } else {
                    final ViewParent p = mParent;
                    if (p != null && mAttachInfo != null) {
                        final Rect r = mAttachInfo.mTmpInvalRect;
                        int minLeft;
                        int maxRight;
                        if (offset < 0) {
                            minLeft = mLeft + offset;
                            maxRight = mRight;
                        } else {
                            minLeft = mLeft;
                            maxRight = mRight + offset;
                        }
                        r.set(0, 0, maxRight - minLeft, mBottom - mTop);
                        p.invalidateChild(this, r);
                    }
                }
            } else {
                invalidateViewProperty(false, false);
            }

            mLeft += offset;
            mRight += offset;
            mRenderNode.offsetLeftAndRight(offset);
            if (isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
            } else {
                if (!matrixIsIdentity) {
                    invalidateViewProperty(false, true);
                }
                invalidateParentIfNeeded();
            }
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }
```
再看看2.3版本的View这部分的源码：
```java
/**
     * Offset this view's horizontal location by the specified amount of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public void offsetLeftAndRight(int offset) {
        if (offset != 0) {
            mLeft += offset;
            mRight += offset;
    }
```
我靠，少了那么多，不过我们只要抓住核心的即可，就是在高版本的代码中，多了重绘的代码，这样我们在重写`onViewPositionChanged(...)`方法的时候：
```java
  @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            invalidate();
        }
```
这样即可。
为了达到QQ侧滑那样的效果，主面板的拖拽范围应该有一定的范围的：
```java

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

            if (child == mMainContent){
                left = fixLeft(left);
            }
            return left;
        }
```
fixLeft()为：
```java
/**
     * 根据范围修正左边值
     * @param left
     * @return
     */
    private int fixLeft(int left) {
        if (left < 0){
            return 0;
        }else if (left > mRange){
            return mRange;
        }
        return left;
    }
```
那么此时左面板还是可以拖动的：
```java
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

            if (changedView == mLeftContent){
                //把当前的变化量传递给mMainContent
                newLeft = mMainContent.getLeft() + dx;
            }

            newLeft = fixLeft(newLeft);

            if (changedView == mLeftContent){
                //当左面板移动之后，再强制放回去
                mLeftContent.layout(0,0,0+mWidth,0+mHeight);
                mMainContent.layout(newLeft,0,newLeft + mWidth,0 + mHeight);
            }
            //为了兼容低版本，每次修改值，进行重绘
            invalidate();
        }
```
现在左边的面板就拖不动了。而且保持了左面板的滑动dx的变化，这样左面板可以带动主面板动了。
ok,下一步要重写执行动画的方法了。`onViewReleased(...)`，首先我们得根据速度和移动的范围来判断是关闭还是打开。
```java
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
```
打开和关闭的方法：
```java
 /**
     * 关闭
     */
    public void close() {
        int finalLeft = 0;
        mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
    }

    /**
     * 打开
     */
    public void open() {
        int finalLeft = mRange;
        mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
    }
```
ok,这个完成了，现在我们需要平滑的打开和关闭：
```java
    @Override
    public void computeScroll() {
        super.computeScroll();
        //持续平滑动画(高频率调用)
        if (dragHelper.continueSettling(true)){
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
            if (dragHelper.smoothSlideViewTo(mMainContent,finalLeft,0)){

                //返回true表示还没有移动到指定的位置，需要刷新界面
                //参数传this,(child所在的ViewGroup)
                ViewCompat.postInvalidateOnAnimation(this);

            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }


    public void open(){
        open(true);
    }

    /**
     * 打开
     */
    public void open(boolean isSmooth) {
        int finalLeft = mRange;
        if (isSmooth) {
            //触发一个平滑的动画
            if (dragHelper.smoothSlideViewTo(mMainContent,finalLeft,0)){

                //返回true表示还没有移动到指定的位置，需要刷新界面
                //参数传this,(child所在的ViewGroup)
                ViewCompat.postInvalidateOnAnimation(this);

            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }
```
在前面的training部分，我们使用Scroller实现了反弹的效果，这里ViewDragHelper封装了Scroller。同时在Scroller中我们需要重写`computeScroll()`方法，这里我们也重写了。
这里需要注意的是几个api:

> * smoothSlideViewTo(...)
> * ViewCompat.postInvalidateOnAnimation(this)
> * computeScroll()
> * continueSettling(boolean)

下面处理动画，分析下QQ侧滑菜单的效果，我们发现：

> * 左面板：缩放动画，平移动画，透明度变化
> * 主面板：缩放动画
> * 背景：亮度变化(颜色变化)

那么我们在什么地方执行动画？这里在`onViewPositionChanged(...)`里面做动画。
```java
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
```
我们根据`newLeft`的值，传入到`dispatchDragEvent(int)`中。
```java
 /**
     * 更新状态，执行动画
     * @param newLeft
     */
    public void dispatchDragEvent(int newLeft) {
        float percent = newLeft * 1.0f / mRange;


        //左面板缩放动画
        //mLeftContent.setScaleX(0.5f + 0.5f*percent);
        //上面注释的效果和下面的一模一样
        mLeftContent.setScaleX(evaluate(percent,0.5f,1.0f));
        mLeftContent.setScaleY(0.5f + 0.5f*percent);

        //左面板平移动画
        mLeftContent.setTranslationX(evaluate(percent,-mWidth / 2.0f,0));


        //左面板透明度变化
        mLeftContent.setAlpha(evaluate(percent,0.5f,1.0f));

        //主面板缩放动画
        mMainContent.setScaleX(evaluate(percent,1.0f,0.8f));
        mMainContent.setScaleY(evaluate(percent,1.0f,0.8f));

        //背景：亮度变化(颜色变化)
        getBackground().setColorFilter((Integer)evaluateColor(percent,Color.BLACK,Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);


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

        return (int)((startA + (int)(fraction * (endA - startA))) << 24) |
                (int)((startR + (int)(fraction * (endR - startR))) << 16) |
                (int)((startG + (int)(fraction * (endG - startG))) << 8) |
                (int)((startB + (int)(fraction * (endB - startB))));
    }
```
后面的两个属性动画估值器不需要去关注。直接从属性动画的相应估值器的实现类中拿就可以了。
这样最后实现的效果如下图：
![QQ侧滑菜单初步](http://1.infotravel.sinaapp.com/pic/37.gif)
接下来便是侧滑菜单的状态回调，我们的侧滑菜单的状态包括Close、Open、Draging状态，然后设置监听接口：
```java
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
```
我们在`onViewPositionChanged(...)`方法中的`dispatchDragEvent(int)`方法中更新状态。
```java
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
```
那么这些监听也就设置好了，现在需要在MainActivity设置监听。
```xml
<me.chenfuduo.myviewdraghelperusage.drag.DragLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:id="@+id/dl"
    android:background="@drawable/bg"
    tools:context=".MainActivity">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#2196f3"></LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#FF4081"></LinearLayout>

</me.chenfuduo.myviewdraghelperusage.drag.DragLayout>
```
实现监听：
```java
//查找DragLayout，设置监听
        DragLayout mDragLayout = (DragLayout) findViewById(R.id.dl);
        mDragLayout.setDragStatusListener(new DragLayout.onDragStatusChangedListener() {
            @Override
            public void onOpen() {
                //这种Toast效果非常的棒，可以直接显示当前的，冲掉之前的
                Utils.showToast(MainActivity.this,"打开了");
            }

            @Override
            public void onDraging(float percent) {
                Utils.showToast(MainActivity.this,"拖拽中");
            }

            @Override
            public void onClose() {
                Utils.showToast(MainActivity.this,"关闭了");
            }
        });
```
Toast的工具方法为：
```java
public static Toast mToast;

	public static void showToast(Context mContext, String msg) {
		if (mToast == null) {
			mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
		}
		mToast.setText(msg);
		mToast.show();
	}
```
很容易看出，这是单例的Toast，这种Toast的效果非常的棒。
现在的效果如下：
![QQ侧滑菜单状态监听](http://1.infotravel.sinaapp.com/pic/38.gif)
现在我们就要设置虚拟数据了，首先是布局：
```xml
<me.chenfuduo.myviewdraghelperusage.drag.DragLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:id="@+id/dl"
    android:background="@drawable/bg"
    tools:context=".MainActivity">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:paddingBottom="50dp"
        android:paddingLeft="10dp"
        android:paddingRight="50dp"
        android:paddingTop="50dp" >

        <ImageView
            android:layout_width="50dp"
            android:layout_height="50dp"
            android:src="@drawable/head" />

        <ListView
            android:id="@+id/lv_left"
            android:layout_width="match_parent"
            android:layout_height="match_parent" >
        </ListView>
    </LinearLayout>

    <LinearLayout
        android:id="@+id/mll"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#ffffff"
        android:orientation="vertical" >

        <RelativeLayout
            android:layout_width="match_parent"
            android:layout_height="50dp"
            android:background="#18B6EF"
            android:gravity="center_vertical" >

            <ImageView
                android:id="@+id/iv_header"
                android:layout_width="30dp"
                android:layout_height="30dp"
                android:layout_marginLeft="15dp"
                android:src="@drawable/head" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_centerHorizontal="true"
                android:text="Header" />
        </RelativeLayout>

        <ListView
            android:id="@+id/lv_main"
            android:layout_width="match_parent"
            android:layout_height="match_parent" >
        </ListView>
    </LinearLayout>


</me.chenfuduo.myviewdraghelperusage.drag.DragLayout>
```
填充数据：
```java
  mLeftList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Cheeses.sCheeseStrings){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView mText = ((TextView)view);
                mText.setTextColor(Color.WHITE);
                return view;
            }
        });

        mMainList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Cheeses.NAMES));
```
现在的效果如下:
![QQ侧滑菜单](http://1.infotravel.sinaapp.com/pic/39.gif)
我们再在三种状态下设置不同的事件：
```java
mDragLayout.setDragStatusListener(new DragLayout.onDragStatusChangedListener() {
            @Override
            public void onOpen() {
                //这种Toast效果非常的棒，可以直接显示当前的，冲掉之前的
                Utils.showToast(MainActivity.this,"打开了");
                //随机设置一个条目
                Random random = new Random();
                int nextNum = random.nextInt(20);
                mLeftList.smoothScrollToPosition(nextNum);
            }

            @Override
            public void onDraging(float percent) {
                Utils.showToast(MainActivity.this,"拖拽中：" + percent);
                mHeaderImage.setAlpha(1-percent);
            }

            @Override
            public void onClose() {
                Utils.showToast(MainActivity.this,"关闭了");
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mHeaderImage, "translationX", 15.0f);
                objectAnimator.setInterpolator(new CycleInterpolator(4));
                objectAnimator.setDuration(500);
                objectAnimator.start();
            }
        });
```
现在的效果是：
![QQ侧滑菜单](http://1.infotravel.sinaapp.com/pic/40.gif)
那么现在在Open和Draging的状态下，主面板的ListView是可以滑动的，我们需要禁用到滑动的事件。新建一个类：`MyLinearLayout`继承自LinearLayout，根据状态做出不同的判断。
```java
package me.chenfuduo.myviewdraghelperusage.drag;


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class MyLinearLayout extends LinearLayout {

	private DragLayout mDragLayout;

	public MyLinearLayout(Context context) {
		super(context);
	}

	public MyLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setDraglayout(DragLayout mDragLayout){
		this.mDragLayout = mDragLayout;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// 如果当前是关闭状态, 按之前方法判断
		if(mDragLayout.getStatus() == DragLayout.Status.Close){
			return super.onInterceptTouchEvent(ev);
		}else {
			return true;
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// 如果当前是关闭状态, 按之前方法处理
		if(mDragLayout.getStatus() == DragLayout.Status.Close){
			return super.onTouchEvent(event);
		}else {
			// 手指抬起, 执行关闭操作
			if(event.getAction() == MotionEvent.ACTION_UP){
				mDragLayout.close();
			}
			
			return true;
		}
	}

}
```
那么在xml文件中引用这个布局，最后在MainActivity中设置引用。
```java
 MyLinearLayout mLinearLayout = (MyLinearLayout) findViewById(R.id.mll);

        //查找DragLayout，设置监听
        DragLayout mDragLayout = (DragLayout) findViewById(R.id.dl);


        // 设置引用
        mLinearLayout.setDraglayout(mDragLayout);
```
到此，我们的QQ侧滑菜单也就完成了。

源码：
[源码](https://github.com/leerduo/MyDragLayout)


