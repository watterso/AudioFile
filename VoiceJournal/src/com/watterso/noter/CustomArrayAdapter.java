package com.watterso.noter;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomArrayAdapter extends ArrayAdapter<Entry> {
	Activity mContext;
	ArrayList<Entry> entries;
	int currentShaded = -1;
	public CustomArrayAdapter(Activity context, int textViewResourceId,
			ArrayList<Entry> entries2) {
		super(context, textViewResourceId, entries2);
		mContext=context;
		entries = entries2;
	}
	public View getView(int position, View convertView, ViewGroup parent){
		LayoutInflater inflator =  mContext.getLayoutInflater();
        View rowView = inflator.inflate(R.layout.rowlayout, null, true);
        TextView entryName = (TextView) rowView.findViewById(R.id.entryName);
        String temp = entries.get(position).getName();
        if(temp.length()>20)
        	temp=temp.substring(0, 22)+"...";
        if(entryName!=null)
        	entryName.setText(temp);
        TextView entryTag = (TextView) rowView.findViewById(R.id.entryTag);
        if(entryTag!=null)
        	entryTag.setText("#"+entries.get(position).getTag());
        TextView entryDate = (TextView) rowView.findViewById(R.id.entryDate);
        if(entryDate!=null)
        	entryDate.setText(entries.get(position).getFormattedTime());
        if(currentShaded!=-1 && currentShaded< this.getCount()){
        	Drawable back = rowView.getBackground();
        	if(position == currentShaded){
        		back.mutate().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
        	}
        	else{
        		back.mutate().setColorFilter(null);
        	}
        		
        }
        return rowView;
	}
	public void setShaded(int i){
		currentShaded = i;
	}
	public int getShaded(){
		return currentShaded;
	}
}
