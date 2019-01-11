package cn.dashingqi.com.downloadfiledemo;

import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * <p>文件描述：<p>
 * <p>作者：北京车车网络技术有限公司<p>
 * <p>创建时间：2019/1/11<p>
 * <p>更改时间：2019/1/11<p>
 * <p>版本号：1<p>
 */
public class DownloadThread extends Thread {
    private static final String TAG = "DownloadThread";
    private URL downUrl;
    private File saveFile;
    private int block;
    private int downloadLength;
    private int threadId;
    private boolean finished = false;
    private FileDownload download;

    DownloadThread(FileDownload download, URL downUrl, File saveFile, int block, int downloadLength, int threadId) {
        this.downUrl = downUrl;
        this.saveFile = saveFile;
        this.block = block;
        this.downloadLength = downloadLength;
        this.threadId = threadId;
        this.download = download;
    }

    @Override
    public void run() {
        if (downloadLength < block) {
            try {
                HttpURLConnection connection = (HttpURLConnection) downUrl.openConnection();
                connection.setConnectTimeout(50000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                connection.setRequestProperty("Accept-Language", "zh-CN");
                connection.setRequestProperty("Referer", downUrl.toString());
                connection.setRequestProperty("Charset", "UTF-8");
                int startPos = block * (threadId - 1) + downloadLength;
                int endPos = block * threadId - 1;
                connection.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
                connection.setRequestProperty("Connection", "Keep-Alive");

                InputStream inStream = connection.getInputStream();

                byte[] buffer = new byte[1024];
                int offset = 0;
                print("Thread" + this.threadId + "starts to download from position" + startPos);
                RandomAccessFile threadFile = new RandomAccessFile(saveFile, "rw");
                threadFile.seek(startPos);

                while (download.getExited() && (offset = inStream.read(buffer, 0, 1024)) != -1) {
                    threadFile.write(buffer, 0, offset);
                    downloadLength += offset;
                    download.update(this.threadId, downloadLength);
                    download.append(offset);
                }

                threadFile.close();
                inStream.close();

                if (download.getExited())
                    print("Thread" + this.threadId + "has been paused");
                else
                    print("Thread" + this.threadId + "download finish");

                this.finished = true;

            } catch (Exception e) {
                this.downloadLength = -1;
                print("Thread" + this.threadId + ":" + e);
            }
        }
    }

    private static void print(String msg) {
        Log.i(TAG, msg);
    }

    public boolean isFinished() {
        return finished;
    }

    public long getDownloadedLength() {
        return downloadLength;
    }
}
