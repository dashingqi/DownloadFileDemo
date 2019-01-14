package cn.dashingqi.com.picwallsdemo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

/**
 * <p>文件描述：<p>
 * <p>作者：北京车车网络技术有限公司<p>
 * <p>创建时间：2019/1/14<p>
 * <p>更改时间：2019/1/14<p>
 * <p>版本号：1<p>
 */
public class MyAdapter extends ArrayAdapter<String> {

    private GridView mGridView;
    private final LruCache<String, Bitmap> mLruCahce;
    private int itemHeight;
    private HashSet<WorkTask> collectionTask;
    private  DiskLruCache mDiskLruCache;
    private BufferedOutputStream out;
    private BufferedInputStream in;
    private HttpURLConnection urlConnection;

    public MyAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull String[] objects, GridView mGridView) {
        super(context, resource, textViewResourceId, objects);
        this.mGridView = mGridView;
        collectionTask = new HashSet<>();

        int maxMemory = (int) Runtime.getRuntime().maxMemory();

        int memoryCache =
                maxMemory / 8;

        mLruCahce = new LruCache<String, Bitmap>(memoryCache) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        File diskLruCachePath = getDiskLruCachePath(getContext(), "bitmap");
        if (!diskLruCachePath.exists())
            diskLruCachePath.mkdirs();
        try {
            mDiskLruCache = DiskLruCache.open(diskLruCachePath, getAppVersion(getContext()), 1, 1024 * 1024 * 8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String imageUrl = getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_grid_view, parent, false);
        } else {
            view = convertView;
        }

        ImageView mImageView = view.findViewById(R.id.mImageView);
        if (mImageView.getLayoutParams().height != itemHeight) {
            mImageView.getLayoutParams().height = itemHeight;
        }
        mImageView.setTag(imageUrl);
        mImageView.setImageResource(R.mipmap.ic_launcher);
        //加载图片去
        loadBitmaps(mImageView, imageUrl);
        return view;
    }

    private void loadBitmaps(ImageView mImageView, String imageUrl) {
        Bitmap bitmap = getBitmapFromLruCache(imageUrl);

        if (bitmap != null) {
            if (mImageView != null && imageUrl != null) {
                mImageView.setImageBitmap(bitmap);
            }
        } else {
            //开启异步 去磁盘加载，如果磁盘没有就去线上去下载数据
            WorkTask workTask = new WorkTask();
            collectionTask.add(workTask);
            workTask.execute(imageUrl);
        }

    }


    private File getDiskLruCachePath(Context context, String name) {
        String path;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || Environment.getExternalStorageDirectory() != null) {

            path = Environment.getDownloadCacheDirectory().getPath();

        } else {
            path = context.getCacheDir().getPath();
        }

        return new File(path + File.separator + name);

    }

    class WorkTask extends AsyncTask<String, Void, Bitmap> {

        private FileDescriptor fileDescriptor;
        private Bitmap bitmap;
        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... args) {
            imageUrl = args[0];
            //首先根据MD5的url值去查询磁盘缓存
            String valueFromMd5 = getValueFromMd5(imageUrl);
            try {
                DiskLruCache.Snapshot snapshot = mDiskLruCache.get(valueFromMd5);
                if (snapshot == null) {
                    //就去下载嘛
                    DiskLruCache.Editor edit = mDiskLruCache.edit(valueFromMd5);
                    OutputStream outputStream = edit.newOutputStream(0);
                    if (edit != null) {
                        if (downLoadValueFromNetWork(imageUrl, outputStream)) {

                            edit.commit();
                        } else {
                            edit.abort();
                        }

                        snapshot = mDiskLruCache.get(valueFromMd5);
                    }

                    if (snapshot != null) {
                        FileInputStream inputStream = (FileInputStream) snapshot.getInputStream(0);
                        fileDescriptor = inputStream.getFD();
                    }

                    if (fileDescriptor != null) {
                        bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            super.onPostExecute(bitmap);
            ImageView mImageView = mGridView.findViewWithTag(imageUrl);
            if (bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            }
            collectionTask.remove(this);
        }
    }

    private boolean downLoadValueFromNetWork(String imageUrl, OutputStream outputStream) {
        try {

            URL url = new URL(imageUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            int code = urlConnection.getResponseCode();
            if (code == 200) {
                out = new BufferedOutputStream(outputStream);
                in = new BufferedInputStream(urlConnection.getInputStream());
                int b;
                while ((b = in.read()) != -1) {
                    out.write(b);
                }

                return true;

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private int getAppVersion(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return 1;
    }

    public void setItemHeight(int height) {
        if (itemHeight == height)
            return;
        itemHeight = height;
        notifyDataSetChanged();
    }

    public Bitmap getBitmapFromLruCache(String url) {
        return mLruCahce.get(url);
    }

    private String getValueFromMd5(String imageUrl) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(imageUrl.getBytes());
            String md5ImageUrl = hexFromByteToString(messageDigest.digest());
            return md5ImageUrl;

        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(imageUrl.hashCode());
        }
    }

    private String hexFromByteToString(byte[] datas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < datas.length; i++) {
            String hex = Integer.toHexString(0xFF & datas[i]);
            sb.append(hex);
        }

        return sb.toString();
    }

    public void flushDiskLruCache() {
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteAllTask() {
        if (collectionTask != null) {
            for (WorkTask workTask : collectionTask) {
                workTask.cancel(false);
            }
        }
    }


}
