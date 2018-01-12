/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */
package com.comet.keyboard.models;

/**
 * Heights for keys
 */
public class KeyHeight implements Cloneable {
  // default row height
  private int mRowDefault;
  
  // scale factor for bottom row
  private float mScaleBottom = 1.0f;
  
  public int getRowDefault(){
    return mRowDefault;
  }
  
  public void setRowDefault(int val){
    mRowDefault = val;
  }
     
  public float getScaleBottom(){
    return mScaleBottom;
  }
  
  public void setScaleBottom(float val){
    mScaleBottom = val;
  }
  
  public int countRowBottom(){
    return  (int) (mRowDefault * mScaleBottom);
  }

  @Override
  public KeyHeight clone() throws CloneNotSupportedException {
    return (KeyHeight) super.clone();
  }
  
  @Override
  public boolean equals(Object o) {
    if(o instanceof KeyHeight && mRowDefault == ((KeyHeight) o).mRowDefault && mScaleBottom == ((KeyHeight) o).mScaleBottom){
      return true;
    }
    return super.equals(o);
  }
}