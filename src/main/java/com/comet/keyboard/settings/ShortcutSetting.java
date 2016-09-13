/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;


import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.comet.keyboard.R;
import com.comet.keyboard.util.DatabaseHelper;
import com.comet.keyboard.util.DatabaseHelper.DBError;

public class ShortcutSetting extends Activity implements OnClickListener,
		OnItemClickListener {
	private class ShortcutAdapter extends ArrayAdapter<ShortcutData> {
		private ArrayList<ShortcutData> items;
		
		public ShortcutAdapter(Context context, int textViewResourceId,
				ArrayList<ShortcutData> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}
		
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			TextView tvShortcut;
			TextView tvExpand;
			
			// Get item data
			ShortcutData data = items.get(position);

			if (view == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.setting_shortcut_item, null);
			}
			
			tvShortcut = (TextView)view.findViewById(R.id.tvShortcut);
			tvExpand = (TextView)view.findViewById(R.id.tvShortcutExpand);
			
			// Set shortcut info
			tvShortcut.setText(data.mKeystroke);
			tvExpand.setText(data.mExpand);

			view.setTag(data);

			return view;
		}
	}
	
	// Request Command
	private final static int REQ_EDIT_SETTING = 100;
	
	// UI components
	private ListView mShortcutListView;
	private LinearLayout mNewShortcutLayout;
	
	private ShortcutAdapter listAdapater;
	// Shortcut list
	private ArrayList<ShortcutData> mList = new ArrayList<ShortcutData>();
	
	/**
     * Called with the activity is first created.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.settings_title_shortcut);
        
        setContentView(R.layout.setting_shortcut);
        
        // Get UI Components
        mShortcutListView = (ListView)findViewById(R.id.lvShortcutList);
        mNewShortcutLayout = (LinearLayout)findViewById(R.id.llNewShortcut);
        
        // Set properties
        listAdapater = new ShortcutAdapter(this, R.layout.setting_shortcut, mList);
        mShortcutListView.setAdapter(listAdapater);
        
        // Load shortcut items
        updateShortcutListFromDB();
        
        // Set event listener
        mNewShortcutLayout.setOnClickListener(this);
        mShortcutListView.setOnItemClickListener(this);
    }

    /**
     * Update shortcut list from database
     */
    private void updateShortcutListFromDB() {
    	DBError error;
        
        error = DatabaseHelper.safeGetDatabaseHelper(this).getShortcutItems(mList);
        if (error != DBError.DB_ERROR_NONE) {
        	Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show();
        	finish();
        }
        
        mShortcutListView.setAdapter(listAdapater);
        listAdapater.notifyDataSetChanged();
    }
	/**
	 * Add new shortcut item
	 */
	public void onClick(View arg0) {
		Intent intent = new Intent(this, EditShortcut.class);
		intent.putExtra(EditShortcut.EXTRA_ISNEW, Boolean.valueOf(true).toString());
		
		startActivityForResult(intent, REQ_EDIT_SETTING);
	}

	/**
	 * Update selected shortcut item
	 */
	public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
		ShortcutData selData;
		
		// Retrieve selected shortcut item
		selData = (ShortcutData)view.getTag();
		
		Intent intent = new Intent(this, EditShortcut.class);
		intent.putExtra(EditShortcut.EXTRA_ISNEW, Boolean.valueOf(false).toString());
		intent.putExtra(EditShortcut.EXTRA_KEYSTROKE, selData.mKeystroke);
		intent.putExtra(EditShortcut.EXTRA_EXPAND, selData.mExpand);
		
		startActivityForResult(intent, REQ_EDIT_SETTING);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_EDIT_SETTING) {
			updateShortcutListFromDB();
		}
	}
}
