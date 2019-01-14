package cn.dashingqi.com.picwallsdemo;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        final int size = getResources().getDimensionPixelSize(R.dimen.size);
        final int spacing = getResources().getDimensionPixelSize(R.dimen.spacing);
        myAdapter = new MyAdapter(this, 0, Images.imageThumbUrls, mGridView);
        mGridView.setAdapter(myAdapter);

        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onGlobalLayout() {

                int mGridViewWidth = mGridView.getWidth();
                int columnsNum = (int) Math.floor(mGridViewWidth / (size + spacing));

                if (columnsNum > 0) {
                    int columnWidth = (mGridView.getWidth() / columnsNum) - spacing;
                    myAdapter.setItemHeight(columnWidth);
                    mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }


            }
        });
    }

    private void initView() {
        mGridView = findViewById(R.id.mGridView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        myAdapter.flushDiskLruCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myAdapter.deleteAllTask();
    }
}
