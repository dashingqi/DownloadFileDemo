package cn.dashingqi.com.downloadfiledemo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

/**
 * <p>文件描述：<p>
 * <p>作者：北京车车网络技术有限公司<p>
 * <p>创建时间：2019/1/11<p>
 * <p>更改时间：2019/1/11<p>
 * <p>版本号：1<p>
 */
public class DBOpenHelper extends SQLiteOpenHelper {

    private static final String DBNAME = "eric.db";
    private static final int VERSION = 1;

    public DBOpenHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS fileddownlog (id integer primary key autoincrement,downpath varchar(100),threadid INTEGER,downlength INTEGER)");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS filedownlog");
        onCreate(db);

    }
}
