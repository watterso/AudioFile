package com.watterso.noter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 4;
 
    // Database Name
    private static final String DATABASE_NAME = "entryManager";
 
    // Contacts table name
    private static final String TABLE_ENTRY = "entries";
 
    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_TAG = "tag";
    private static final String KEY_FILE = "file";
    private static final String KEY_TIME = "time";
    
 
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
	@Override
	public void onCreate(SQLiteDatabase arg0) {
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_ENTRY + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_TAG + " TEXT," + KEY_FILE + " TEXT," + KEY_TIME + " TEXT"+")";
        arg0.execSQL(CREATE_CONTACTS_TABLE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// Drop older table if existed
        arg0.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRY);
 
        // Create tables again
        onCreate(arg0);
	}
	public void addEntry(Entry entry) {
		SQLiteDatabase db = this.getWritableDatabase();
		 
	    ContentValues values = new ContentValues();
	    values.put(KEY_NAME, entry.getName()); 
	    values.put(KEY_TAG, entry.getTag()); 
	    values.put(KEY_FILE, entry.getFile());
	    values.put(KEY_TIME, entry.getTime()); 
	 
	    // Inserting Row
	    db.insert(TABLE_ENTRY, null, values);
	    db.close(); // Closing database connection
	}
	public void addEntries(ArrayList<Entry> dbs){
		for(Entry ent: dbs){
			addEntry(ent);
		}
	}
	public Entry getEntry(int id) {
		SQLiteDatabase db = this.getReadableDatabase();
		 
	    Cursor cursor = db.query(TABLE_ENTRY, new String[] { KEY_ID,
	            KEY_NAME, KEY_TAG, KEY_FILE, KEY_TIME }, KEY_ID + "=?",
	            new String[] { String.valueOf(id) }, null, null, null, null);
	    if (cursor != null)
	        cursor.moveToFirst();
	 
	    Entry entry = new Entry(Integer.parseInt(cursor.getString(0)),
	            cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
	    return entry;
	}
	public List<String> getTags(){			//returns a list of the the tags sans duplicates
		List<Entry> temp = getAllEntries();
		List<String> ret = new ArrayList<String>();
		for(Entry ent: temp){
			ret.add(ent.getTag());
		}
		HashSet<String> hash = new HashSet<String>();
		hash.addAll(ret);
		ret.clear();
		ret.addAll(hash);
		return ret;
	}
	public List<Entry> getAllEntries(String tag){			//given a tag with a '#' in front of it return matching entries
		//tag = tag.substring(1);								//Get rid of '#'
		List<Entry> entryList = new ArrayList<Entry>();
	    // Select All Query
	    String selectQuery = "SELECT  * FROM " + TABLE_ENTRY;
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	        do {
	        	if(cursor.getString(2).equals(tag)){
	        		//Log.d("tag match:",cursor.getString(2)+"=="+tag);
	        		Entry entry = new Entry();
	        		entry.setID(Integer.parseInt(cursor.getString(0)));
	        		entry.setName(cursor.getString(1));
	        		entry.setTag(cursor.getString(2));
	        		entry.setFile(cursor.getString(3));
	        		entry.setTime(cursor.getString(4));
	        		entryList.add(entry);
	        	}else{
	        		//Log.d("no tag match:",cursor.getString(2)+"=\\="+tag);
	        	}
	        } while (cursor.moveToNext());
	    }
	    return entryList;
	}
	public List<Entry> getAllEntries() {
		List<Entry> entryList = new ArrayList<Entry>();
	    // Select All Query
	    String selectQuery = "SELECT  * FROM " + TABLE_ENTRY;
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	        do {
	            Entry entry = new Entry();
	            entry.setID(Integer.parseInt(cursor.getString(0)));
	            entry.setName(cursor.getString(1));
	            entry.setTag(cursor.getString(2));
        		entry.setFile(cursor.getString(3));
        		entry.setTime(cursor.getString(4));
	            entryList.add(entry);
	        } while (cursor.moveToNext());
	    }
	    return entryList;
	}
	public int getEntryCount() {
		String countQuery = "SELECT  * FROM " + TABLE_ENTRY;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
 
        // return count
        return cursor.getCount();
	}
	public int updateEntry(Entry entry) {
		SQLiteDatabase db = this.getWritableDatabase();
		 
	    ContentValues values = new ContentValues();
	    values.put(KEY_NAME, entry.getName());
	    values.put(KEY_TAG, entry.getTag());
	    values.put(KEY_FILE, entry.getFile());
	    values.put(KEY_TIME, entry.getTime());
	    // updating row
	    return db.update(TABLE_ENTRY, values, KEY_ID + " = ?",
	            new String[] { String.valueOf(entry.getID()) });
	}
	public void deleteEntry(Entry entry) {
		SQLiteDatabase db = this.getWritableDatabase();
	    db.delete(TABLE_ENTRY, KEY_ID + " = ?",
	            new String[] { String.valueOf(entry.getID()) });
	    db.close();
	}

}
