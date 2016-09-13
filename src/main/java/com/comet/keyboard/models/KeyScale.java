/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */
package com.comet.keyboard.models;

/**
 * Scale factors for keys
 */
public class KeyScale {
  
  private float mRowBottom;

  private float mRowDefault;

  public KeyScale() {}

  /**
   * Creates scales from key heights
   * 
   * @param kh
   * @param size
   */
  public KeyScale(KeyHeight kh, int size) {
    mRowDefault = (float) kh.getRowDefault() / size;
    mRowBottom = (float) kh.countRowBottom() / size;
  }

  @Override
  public KeyHeight clone() throws CloneNotSupportedException {
    return (KeyHeight) super.clone();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof KeyScale && mRowDefault == ((KeyScale) o).mRowDefault
        && mRowBottom == ((KeyScale) o).mRowBottom) {
      return true;
    }
    return super.equals(o);
  }

  public float getRowBottom() {
    return mRowBottom;
  }

  public float getRowDefault() {
    return mRowDefault;
  }

  public void setRowBottom(float rowBottom) {
    this.mRowBottom = rowBottom;
  }

  public void setRowDefault(float rowDefault) {
    this.mRowDefault = rowDefault;
  }
}