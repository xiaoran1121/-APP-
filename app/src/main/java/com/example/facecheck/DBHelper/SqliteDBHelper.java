package com.example.facecheck.DBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.facecheck.Enity.Userinfo;

public class SqliteDBHelper extends SQLiteOpenHelper {
    private static SqliteDBHelper mHelper = null;
    private static final String DB_NAME="face.db";
    private static final int DB_VERSION=2;
    private static final String TABLE_USER_INFO = "user_info";
    private SQLiteDatabase mRDB = null;
    private SQLiteDatabase mWDB = null;


    private SqliteDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    // 利用单例模式获取数据库帮助器的唯一实例
    public static SqliteDBHelper getInstance(Context context) {
        if (mHelper == null) {
            mHelper = new SqliteDBHelper(context);
        }
        return mHelper;
    }

    // 打开数据库的读连接
    public SQLiteDatabase openReadLink() {
        if (mRDB == null || !mRDB.isOpen()) {
            mRDB = mHelper.getReadableDatabase();
        }
        return mRDB;
    }

    // 打开数据库的写连接
    public SQLiteDatabase openWriteLink() {
        if (mWDB == null || !mWDB.isOpen()) {
            mWDB = mHelper.getWritableDatabase();
        }
        return mWDB;
    }

    // 关闭数据库连接
    public void closeLink() {
        if (mRDB != null && mRDB.isOpen()) {
            mRDB.close();
            mRDB = null;
        }

        if (mWDB != null && mWDB.isOpen()) {
            mWDB.close();
            mWDB = null;
        }
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_USER_INFO +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                " name VARCHAR ," +
                " gender VARCHAR ," +
                " age INTERGT ," +
                " identity_id INTERGT NOT NULL UNIQUE,"+
                " pass INTERGT );";
        sqLiteDatabase.execSQL(sql);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
    //删除
    public void delete(Userinfo info) {
       mWDB.delete(TABLE_USER_INFO, "name=?", new String[]{info.identity_id});
    }

    //插入用户表信息
    public void insert(Userinfo info) {
        ContentValues values = new ContentValues();
        values.put("name", info.name);
        values.put("gender", info.gender);
        values.put("age", info.age);
        values.put("identity_id", info.identity_id);
        values.put("pass", info.pass);
         mWDB.insert(TABLE_USER_INFO, null, values);
    }
    //保存
    public void save(Userinfo info) {
        // 如果存在则先删除，再添加
        try {
            mWDB.beginTransaction();
            delete(info);
            insert(info);
            mWDB.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            mWDB.endTransaction();
        }
    }
    //查询
    public Userinfo queryById(String identity_id) {
        Userinfo info = null;
        String sql = "select * from " + TABLE_USER_INFO;
        // 执行记录查询动作，该语句返回结果集的游标
        Cursor cursor = mRDB.query(TABLE_USER_INFO, null, "identity_id=? ",
                new String[]{identity_id}, null, null, null);
        if ( cursor.moveToNext()) {
            info = new Userinfo();
            info.id = cursor.getInt(0);
            info.name = cursor.getString(1);
            info.gender = cursor.getString(2);
            info.age = cursor.getInt(3);
            info.identity_id = cursor.getString(4);
            info.pass = (cursor.getInt(5) == 0) ? false : true;
        }
        return info;
    }


}
