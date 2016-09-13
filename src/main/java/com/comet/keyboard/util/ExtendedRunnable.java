package com.comet.keyboard.util;


public abstract class ExtendedRunnable implements Runnable {
	public Object item;
	public ExtendedRunnable(Object item) {
		this.item = item;
	}
}