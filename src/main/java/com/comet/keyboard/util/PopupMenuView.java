/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */


package com.comet.keyboard.util;

import java.util.ArrayList;

import junit.framework.Assert;

import android.content.Context;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class PopupMenuView extends LinearLayout implements OnCreateContextMenuListener, OnKeyListener {
	// current popup menu
	private static PopupMenuView currPopupMenu = null;
	
	private ViewGroup parentView;
	
	public ContextMenu mMenu;
	public ContextMenuInfo mMenuInfo;
	
	@SuppressWarnings("unused")
	private boolean isShowed = false;
	@SuppressWarnings("unused")
	private static boolean isCloseAllMenu = false;
	
	// Menu item list
	private ArrayList<String> mMenuList = new ArrayList<String>(5);
	
	private int mIconResId;
	private int mTitleResId;
	
	// Menu item handler
	private OnPopupMenuItemClickListener mListener;
	
	public PopupMenuView(Context context) {
		super(context);
	}
	
	public PopupMenuView(ViewGroup view, ArrayList<String> list, int titleResId, int iconResId) {
		super(view.getContext());
		
		parentView = view;
		isCloseAllMenu = false;
		
		setMenuList(list);
		setTitle(titleResId);
		setIcon(iconResId);
		
		setOnCreateContextMenuListener(this);
		setOnKeyListener(this);
		
		parentView.addView(this);
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu);

		mMenu = menu;
		mMenuInfo = menuInfo;
		buildMenu();
		
		
		isShowed = true;
	}
	
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			isShowed = false;
			return true;
		}
		
		return false;
	}
	
	public boolean showContextMenu() {
		currPopupMenu = this;
		
		return super.showContextMenu();
	}
	
	public static void closeAllMenu() {
		if (currPopupMenu != null && currPopupMenu.mMenu != null) {
			currPopupMenu.parentView.removeView(currPopupMenu);
			currPopupMenu.setVisibility(View.GONE);

			/*
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) currPopupMenu.mMenuInfo;

			if (info.targetView != null)
				info.targetView.setVisibility(View.GONE);
			
			MenuItem item = currPopupMenu.mMenu.findItem(0);
			item.setVisible(false);
			*/
			
			isCloseAllMenu = true;
		}
	}
	
	/**
	 * Build sub menu items
	 */
	public void buildMenu() {
		int index = 0;
		
		mMenu.clear();
		mMenu.setHeaderTitle(mTitleResId);
		mMenu.setHeaderIcon(mIconResId);
		for (String eachMenu : mMenuList) {
			mMenu.add(0, index, index, eachMenu).setOnMenuItemClickListener(new OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					if (mListener != null) {
						mListener.onPopupMenuItemClicked(item.getItemId(), item.getTitle().toString());
					}
					
					return true;
				}
			});
			index++;
		}
	}
	
	/**
	 * Set menu items
	 */
	public void setMenuList(ArrayList<String> list) {
		Assert.assertTrue(list != null);
		
		mMenuList.clear();
		mMenuList.addAll(list);
	}
	
	public void setTitle(int resId) {
		mTitleResId = resId;
	}
	
	public void setIcon(int resId) {
		mIconResId = resId;
	}
	
	public void setOnPopupMenuItemClickListener(OnPopupMenuItemClickListener listener) {
		mListener = listener;
	}
}
