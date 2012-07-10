package com.watterso.noter;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

public class RecordActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        ActionBar action = getActionBar();
        action.setTitle("Make a Recording");
        action.setDisplayShowHomeEnabled(false);
        
        //action.hide();
        Intent intent = getIntent();
        ArrayList<String> tags = intent.getStringArrayListExtra(MainActivity.EXTRA_MESSAGE);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, tags);
        AutoCompleteTextView textView = (AutoCompleteTextView)findViewById(R.id.tagger);
        textView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_record, menu);
        return true;
    }

    
}
