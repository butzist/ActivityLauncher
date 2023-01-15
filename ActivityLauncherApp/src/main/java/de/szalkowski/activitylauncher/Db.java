package de.szalkowski.activitylauncher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public class Db {
    public static final String QUERIES_TABLE = "queries";
    public static final String VALUE = "value";

    public static void insertItem(Context context, String value) {
        ContentValues content = new ContentValues();
        content.put(VALUE, value);

        SQLiteDatabase database = new DbOpenHelper(context).getWritableDatabase();
        database.insertWithOnConflict(QUERIES_TABLE, null, content, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static List<String> getItems(Context context, String value) {
        List<String> result = new ArrayList<>();

        SQLiteDatabase database = new DbOpenHelper(context).getReadableDatabase();
        Cursor cursor = database.query(QUERIES_TABLE, new String[] { VALUE }, VALUE + " LIKE ?", new String[] { value + "%" }, null, null, null);
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();

        return result;
    }

    public static class DbOpenHelper extends SQLiteOpenHelper {
        private static final int DB_VERSION = 1;
        private static final String DB_NAME = "data.db";

        public DbOpenHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            final String queriesTable = "CREATE TABLE IF NOT EXISTS [" + QUERIES_TABLE + "]" +
                    "([" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " [" + VALUE + "] TEXT UNIQUE NOT NULL)";

            sqLiteDatabase.execSQL(queriesTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + QUERIES_TABLE);
            onCreate(sqLiteDatabase);
        }
    }
}
