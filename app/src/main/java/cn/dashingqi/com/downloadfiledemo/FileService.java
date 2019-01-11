package cn.dashingqi.com.downloadfiledemo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>文件描述：<p>
 * <p>作者：北京车车网络技术有限公司<p>
 * <p>创建时间：2019/1/11<p>
 * <p>更改时间：2019/1/11<p>
 * <p>版本号：1<p>
 */
public class FileService {

    private final DBOpenHelper openHelper;

    public FileService(Context context) {
        openHelper = new DBOpenHelper(context);
    }

    public Map<Integer, Integer> getData(String path) {
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select threadid,downlength from filedownlog where downpath = ?", new String[]{path});
        HashMap<Integer, Integer> data = new HashMap<>();
        while (cursor.moveToNext()) {
            data.put(cursor.getInt(0), cursor.getInt(1));
            data.put(cursor.getInt(cursor.getColumnIndexOrThrow("threadid")), cursor.getInt(cursor.getColumnIndexOrThrow("downlength")));
        }
        cursor.close();
        db.close();
        return data;
    }

    public void save(String path, Map<Integer, Integer> map) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                db.execSQL("insert into filedownlog(downpath,threadid,downlength) values(?,?,?)", new Object[]{path, entry.getKey(), entry.getValue()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public void update_tyc(String path, int threadId, int pos) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("update filedownlog set downlength=? where downpath=? and threadid=?", new Object[]{pos, path, threadId});
        db.close();
    }

    public void update(String path, Map<Integer, Integer> map) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                db.execSQL("update filedownlog set downlength =? where downpath=? and threadid=?", new Object[]{entry.getValue(), path, entry.getKey()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.close();
    }

    public void delete(String path) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("delete from filedownlog where downpath =?", new Object[]{path});
        db.close();
    }
}
