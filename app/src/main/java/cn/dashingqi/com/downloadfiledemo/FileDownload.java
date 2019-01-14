package cn.dashingqi.com.downloadfiledemo;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>文件描述：<p>
 * <p>作者：北京车车网络技术有限公司<p>
 * <p>创建时间：2019/1/11<p>
 * <p>更改时间：2019/1/11<p>
 * <p>版本号：1<p>
 */
public class FileDownload {
    private static final String TAG = "FileDownload";
    private Context context;
    private FileService fileService;
    private boolean exited;
    private int downloadedSize = 0;
    private int fileSize = 0;
    private DownloadThread[] threads;
    private File saveFile;
    private Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>();
    private int block;
    private String downloadUrl;

    public int getThreadSize() {
        return threads.length;
    }

    public void exit() {
        this.exited = true;
    }

    public boolean getExited() {
        return this.exited;
    }

    public int getFileSize() {
        return fileSize;
    }

    protected synchronized void append(int size) {
        downloadedSize += size;
    }

    protected synchronized void update(int threadId, int pos) {
        this.data.put(threadId, pos);
        this.fileService.update_tyc(this.downloadUrl, threadId, pos);
    }

    public FileDownload(Context context, String downloadUrl, File fileSaveDir, int threadNum) {
        try {
            this.context = context;
            this.downloadUrl = downloadUrl;
            fileService = new FileService(this.context);
            URL url = new URL(this.downloadUrl);
            if (!fileSaveDir.exists())
                fileSaveDir.mkdirs();
            this.threads = new DownloadThread[threadNum];

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            connection.setRequestProperty("Accept_Language", "zh_CN");
            connection.setRequestProperty("Referer", downloadUrl);
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.connect();

            printResponseHeader(connection);

            if (connection.getResponseCode() == 200) {
                this.fileSize = connection.getContentLength();
                if (this.fileSize <= 0)
                    throw new RuntimeException("UnKnow file size");

                String fileName = getFileName(connection);
                this.saveFile = new File(fileSaveDir, fileName);

                Map<Integer, Integer> logdata = fileService.getData(downloadUrl);

                if (logdata.size() > 0) {
                    for (Map.Entry<Integer, Integer> entry : logdata.entrySet()) {
                        data.put(entry.getKey(), entry.getValue());
                    }
                }

                if (this.data.size() == this.threads.length) {
                    for (int i = 0; i < this.threads.length; i++) {
                        this.downloadedSize += this.data.get(i + 1);
                    }
                    print("已经下载的长度" + this.downloadedSize + "个字节");
                }
                this.block = (this.fileSize % this.threads.length) == 0 ? this.fileSize / this.threads.length : this.fileSize / this.threads.length + 1;
            } else {
                print("服务器响应错误" + connection.getResponseCode() + connection.getResponseMessage());
            }

        } catch (IOException e) {
            print(e.toString());
            throw new RuntimeException("can't connection this url");
        }
    }

    private String getFileName(HttpURLConnection connection) {
        String filename = this.downloadUrl.substring(this.downloadUrl.lastIndexOf("/") + 1);

        if (filename == null || "".equals(filename.trim())) {
            for (int i = 0; ; i++) {
                String mine = connection.getHeaderField(i);
                if (mine == null) {
                    break;
                }

                if ("content-disposition".equals(connection.getHeaderFieldKey(i).toLowerCase())) {
                    Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase(Locale.getDefault()));
                    if (m.find())
                        return m.group(1);
                }
            }

            filename = UUID.randomUUID() + ".tmp";
        }
        return filename;
    }

    public int download(cn.dashingqi.com.downloadfiledemo.DownloadProgressListener listener) throws Exception {
        try {
            RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rwd");
            if (this.fileSize > 0)
                randOut.setLength(this.fileSize);
            randOut.close();
            URL url = new URL(this.downloadUrl);

            if (this.data.size() != this.threads.length) {
                this.data.clear();
                for (int i = 0; i < this.threads.length; i++) {
                    this.data.put(i + 1, 0);
                }
                this.downloadedSize = 0;
            }

            for (int i = 0; i < this.threads.length; i++) {
                int downloadedLength = this.data.get(i + 1);
                if (downloadedLength < this.block && this.downloadedSize < this.fileSize) {
                    this.threads[i] = new DownloadThread(this, url, this.saveFile, this.block, this.data.get(i + 1), i + 1);
                    this.threads[i].setPriority(7);
                    this.threads[i].start();
                } else {
                    this.threads[i] = null;
                }
            }
            this.fileService.delete(this.downloadUrl);
            this.fileService.save(this.downloadUrl, this.data);
            boolean notFinished = true;
            while (notFinished) {
                Thread.sleep(900);
                notFinished = false;
                for (int i = 0; i < this.threads.length; i++) {
                    notFinished = true;
                    if (this.threads[i].getDownloadedLength() == -1) {
                        this.threads[i] = new DownloadThread(this, url, this.saveFile, this.block, this.data.get(i + 1), i + 1);
                        this.threads[i].setPriority(7);
                        this.threads[i].start();
                    }
                }
            }

            if (listener != null)
                listener.onDownloadSize(this.downloadedSize);

            if (downloadedSize == this.fileSize)
                this.fileService.delete(this.downloadUrl);
        } catch (Exception e) {
            print(e.toString());
            throw new Exception("File downloads error");
        }

        return this.downloadedSize;
    }

    public static void printResponseHeader(HttpURLConnection http) {

        Map<String, String> header = getHttpResponseHeader(http);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            print(key + entry.getValue());
        }
    }

    public static Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
        LinkedHashMap<String, String> header = new LinkedHashMap<>();
        for (int i = 0; ; i++) {
            String mine = http.getHeaderField(i);
            if (mine == null) {
                break;
            }
            header.put(http.getHeaderFieldKey(i), mine);
        }

        return header;
    }

    private static void print(String msg) {
        Log.i(TAG, msg);
    }

}
