package me.chenfuduo.myviewdraghelperusage;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.CycleInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Random;

import me.chenfuduo.myviewdraghelperusage.drag.DragLayout;
import me.chenfuduo.myviewdraghelperusage.drag.MyLinearLayout;
import me.chenfuduo.myviewdraghelperusage.util.Cheeses;
import me.chenfuduo.myviewdraghelperusage.util.Utils;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        final ListView mLeftList = (ListView) findViewById(R.id.lv_left);
        final ListView mMainList = (ListView) findViewById(R.id.lv_main);
        final ImageView mHeaderImage = (ImageView) findViewById(R.id.iv_header);
        MyLinearLayout mLinearLayout = (MyLinearLayout) findViewById(R.id.mll);

        //查找DragLayout，设置监听
        DragLayout mDragLayout = (DragLayout) findViewById(R.id.dl);


        // 设置引用
        mLinearLayout.setDraglayout(mDragLayout);

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

    }

}
