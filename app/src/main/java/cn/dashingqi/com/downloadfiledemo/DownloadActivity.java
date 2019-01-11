package cn.dashingqi.com.downloadfiledemo;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.mbms.DownloadProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

public class DownloadActivity extends AppCompatActivity {

    private Button btnStartDownload;
    private Button btnStopDownload;
    private Context context;

    private static class UIHandler extends Handler {

        private final WeakReference<DownloadActivity> mActivity;

        UIHandler(DownloadActivity activity) {
            mActivity = new WeakReference<DownloadActivity>(activity);
        }
    }

    private class ButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_download:
                    //开始下载
                    //TODO 提供下载地址
                    String path = "下载地址";
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        File saveDir = new File(context.getCacheDir(), "/cache/exe");
                    } else {
                        Toast.makeText(getApplicationContext(), "下载路径不存在", Toast.LENGTH_LONG).show();
                    }

                    break;
                case R.id.btn_stop_download:
                    //停止下载

                    break;

            }
        }

        private void download(String path, File savDir) {

        }

        class DownloadTask implements Runnable {
            private String path;
            private File savDir;
            private FileDownload loader;


            DownloadTask(String path, File savDir) {
                this.path = path;
                this.savDir = savDir;
            }

            void exit() {
                if (loader != null) {
                    loader.exit();
                }
            }

            //进度监听，通过message机制传递进度
            DownloadProgressListener downloadProgressListener =  new DownloadProgressListener(){

            }


            @Override
            public void run() {

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        initView();
    }

    private void initView() {
        btnStartDownload = findViewById(R.id.btn_start_download);
        btnStopDownload = findViewById(R.id.btn_stop_download);
        ButtonClickListener listener = new ButtonClickListener();
        btnStartDownload.setOnClickListener(listener);
        btnStopDownload.setOnClickListener(listener);
    }
}
