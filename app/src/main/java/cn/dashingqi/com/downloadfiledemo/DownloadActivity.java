package cn.dashingqi.com.downloadfiledemo;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

public class DownloadActivity extends AppCompatActivity {

    private static final int PROCESSING = 1;
    private static final int FAILURE = -1;

    private Button btnStartDownload;
    private Button btnStopDownload;
    private Context context;
    private ProgressBar mProgressBar;
    Handler handler = new UIHandler(this);

    private static class UIHandler extends Handler {

        private final WeakReference<DownloadActivity> mActivity;
        private TextView resultView;

        UIHandler(DownloadActivity activity) {
            mActivity = new WeakReference<DownloadActivity>(activity);

        }

        @Override
        public void handleMessage(Message msg) {
            DownloadActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case PROCESSING:
                        //更新UI
                        ProgressBar progressBar = activity.findViewById(R.id.mProgressBar);
                        resultView = activity.findViewById(R.id.resultView);
                        progressBar.setProgress(msg.getData().getInt("size"));
                        float num = (float) progressBar.getProgress() / (float) progressBar.getMax();
                        int result = (int) (num * 100);
                        resultView.setText(result);

                        if (progressBar.getProgress() == progressBar.getMax()) {
                            Toast.makeText(activity, "下载成功", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case FAILURE:
                        Toast.makeText(activity, "下载失败", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    }

    private class ButtonClickListener implements View.OnClickListener {

        private File saveDir;
        private DownloadTask task;

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_download:
                    //开始下载
                    //TODO 提供下载地址
                    String path = "http://www.pptbz.com/pptpic/UploadFiles_6909/201211/2012111719294197.jpg";
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        saveDir = new File(context.getCacheDir(), "/cache/exe");
                        download(path, saveDir);
                    } else {
                        Toast.makeText(getApplicationContext(), "下载路径不存在", Toast.LENGTH_LONG).show();
                    }
                    btnStartDownload.setEnabled(false);
                    btnStopDownload.setEnabled(true);
                    break;
                case R.id.btn_stop_download:
                    //停止下载
                    btnStartDownload.setEnabled(true);
                    btnStopDownload.setEnabled(false);
                    exit();
                    break;

            }
        }

        private void exit() {
            if (task != null) {
                task.exit();
            }
        }

        private void download(String path, File savDir) {
            task = new DownloadTask(path, savDir);
            new Thread(task).start();

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
            DownloadProgressListener downloadProgressListener = new DownloadProgressListener() {

                @Override
                public void onDownloadSize(int size) {
                    Message message = new Message();
                    message.what = PROCESSING;
                    message.getData().putInt("size", size);
                    handler.sendMessage(message);
                }
            };


            @Override
            public void run() {
                try {
                    //固定三个线程
                    FileDownload loader = new FileDownload(getApplicationContext(), path, saveDir, 3);
                    mProgressBar.setMax(loader.getFileSize());
                    loader.download(downloadProgressListener);
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendMessage(handler.obtainMessage(FAILURE));

                }

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
        mProgressBar = findViewById(R.id.mProgressBar);
        ButtonClickListener listener = new ButtonClickListener();
        btnStartDownload.setOnClickListener(listener);
        btnStopDownload.setOnClickListener(listener);
    }
}
