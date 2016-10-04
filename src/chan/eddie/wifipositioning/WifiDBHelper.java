package chan.eddie.wifipositioning;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class WifiDBHelper extends SQLiteOpenHelper {

	final public static int DB_VERSION = 1;
    final public static String DATABASE_NAME = "wifi.db";
    final public static String TABLE_POINT = "WPoint";
    final public static String TABLE_SURVEY = "WSurvey";
    final public static String TABLE_SAMPLE = "WSample";
	
	public WifiDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DB_VERSION);
	}
	
    public WifiDBHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL(
        	"CREATE TABLE "+TABLE_POINT+" (" +
        		"pid INTEGER PRIMARY KEY AUTOINCREMENT, " +
        		"px FLOAT NOT NULL, " +
        		"py FLOAT NO NULL" +
        	")"
        );
        db.execSQL(
            	"CREATE TABLE "+TABLE_SURVEY+" (" +
            		"sid INTEGER PRIMARY KEY AUTOINCREMENT, " +
            		"pid INTEGER NOT NULL, " +
            		"sts TIMESTAMP NOT NULL" +
            	")"
            );
        db.execSQL(
            	"CREATE TABLE "+TABLE_SAMPLE+" (" +
            		"pid INTEGER NOT NULL, " +
            		"sid INTEGER NOT NULL, " +
            		"bssid TEXT NOT NULL, " +
            		"ssid TEXT NOT NULL, " +
            		"rssi FLOAT NOT NULL, " +
            		"freq INT NOT NULL, " +
            		"desc TEXT NOT NULL" +
            	")"
            );
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_POINT);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_SURVEY);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_SAMPLE);
        onCreate(db);
	}

}

