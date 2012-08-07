package com.watterso.noter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.util.Log;

public class Entry {
	int _id;
	String _name;
	String _tag;
	Date _timeStamp;
	String _fileName;
	public Entry(){
		
	}
	public Entry(int id, String name, String tag, String file, String time){
		this._id = id;
		this._name = name;
		this._tag = tag;
		this._fileName = file;
		String format = "MMM dd yyyy";
		SimpleDateFormat ad =new SimpleDateFormat(format);
		try {
			this._timeStamp = ad.parse(time);
			Log.d("TimeStamp", "Set");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Entry(String name, String tag){
        this._name = name;
        this._tag = tag;
		this._timeStamp = new Date();
		this._fileName = uniqueFile();
    }
	private String uniqueFile(){
		String format = "MMM dd yyyy";
		SimpleDateFormat ad =new SimpleDateFormat(format);
        String temp = ""+ad.format(this._timeStamp).hashCode();
        return this._name+temp.substring(0,3);
	}
	public int getID(){
        return this._id;
    }
    public void setID(int id){
        this._id = id;
    }
    public String getFile(){
        return this._fileName;
    }
    public void setFile(String file){
        this._fileName = file;
    }
    public String getFormattedTime(){
    	String format =  "MMM dd";
    	Calendar now = Calendar.getInstance();
    	Calendar then = new GregorianCalendar();
    	then.setTime(this._timeStamp);
    	if(then.get(Calendar.YEAR)<now.get(Calendar.YEAR))
    		format = "MMM dd yyyy";
		SimpleDateFormat ad =new SimpleDateFormat(format);
        return this._timeStamp!=null ?ad.format(this._timeStamp) : "DATE ERROR";
    }
    public String getTime(){
    	String format = "MMM dd yyyy";
		SimpleDateFormat ad =new SimpleDateFormat(format);
        return ad.format(this._timeStamp);
    }
    public void setTime(String time){
    	String format = "MMM dd yyyy";
		SimpleDateFormat ad =new SimpleDateFormat(format);
		try {
			this._timeStamp = ad.parse(time);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public String getName(){
        return this._name;
    }
    public void setName(String name){
        this._name = name;
    }
    public String getTag(){
        return this._tag;
    }
    public void setTag(String tag){
        this._tag = tag;
    }
    public String toString(){
		
    	return this._name+" #"+this._tag+" "+this.getFormattedTime();
    }

}
