/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

/**
 * Makes time event on UI threads and dispatch it to their handler, 
 * so their handlers will enter the safe accessing mode for ui object.
 * Functions: add/remove
 */

package com.comet.keyboard.util;

import junit.framework.Assert;
import android.os.Handler;
import android.os.Message;

/**
 * Game Event Dispatcher
 * @author CCH
 *
 */
public class TimeEventManager extends Handler {
	// Timer
	private static TimeEventManager eventManager;
	
	public static final int HANDLE_INIT = 0;
	public static final int HANDLE_TIME_EVENT = 1;
	public static final int HANDLE_SURFACE_EVENT = 2;
	
	private final static int MESSAGE_ID = 10000;
	
	private final static int HANDLER_STOP = 0;
	private final static int HANDLER_START = 1;
	
	private int handlerStatus = HANDLER_STOP;
	
	public class TimeEventData {
		Object observer;
		
		TimeEventListener listener;
		int count;
		long delay;
	}
	
	public TimeEventManager() {
	}
	
	public static TimeEventManager safeGetEventManager() {
		if (eventManager == null) {
			eventManager = new TimeEventManager();
			eventManager.start();
		}
		
		return eventManager;
	}
	
	/**
	 * Create new event item
	 * @param delay
	 * @param listener
	 * @param count
	 * @return
	 */
	private TimeEventData createNewEventData(Object observer, long delay, 
			TimeEventListener listener, int count) {
		TimeEventData newEventData = new TimeEventData();
		
		setEventData(newEventData, observer, delay, listener, count);
		
		return newEventData;
	}
	
	private TimeEventData setEventData(TimeEventData eventData, 
			Object observer, long delay, TimeEventListener listener, int count) {
		Assert.assertTrue(eventData != null);
		
		Assert.assertTrue(delay > 0);
		Assert.assertTrue(count >= 0);
		
		eventData.observer = observer;
		eventData.listener = listener;
		eventData.count = count;
		eventData.delay = delay;
		
		return eventData;
	}
	
	private TimeEventData createAndSetEventData(TimeEventData eventData, 
			Object observer, long delay, TimeEventListener listener, int count) {
		if (eventData == null)
			eventData = createNewEventData(observer, delay, listener, count);
		else
			setEventData(eventData, observer, delay, listener, count);
		
		return eventData;
	}
	
	/**
	 * Add heartbeat event message
	 * @param delay
	 */
	@SuppressWarnings("unused")
	private synchronized Object addInitMessage(long delay) {
		return addInitMessage(delay, 0);
	}
	
	private synchronized Object addInitMessage(long delay, int count) {
		Message msg = new Message();
		TimeEventData newEventData;
		
		newEventData = createNewEventData(null, delay, null, count);
		msg.what = MESSAGE_ID;
		
		msg.arg1 = HANDLE_INIT;
		msg.obj = newEventData;
		
		_sendMessageDelayed(msg, delay);
		
		return msg;
	}
	
	private void _sendMessageDelayed(Message msg, long delay) {
		msg.arg2 = (int)delay;
		super.sendMessageDelayed(msg, delay);
	}
	/**
	 * Add refresh time event message
	 * @param delay
	 */
	public synchronized Object addRefreshTimeEventMessage(Object observer, TimeEventListener listener, long delay) {
		return addRefreshTimeEventMessage(observer, listener, delay, 0);
	}
	
	public synchronized Object addRefreshTimeEventMessage(Object observer, TimeEventListener listener, long delay, int count) {
		Message msg = new Message();
		TimeEventData newEventData;
		
		newEventData = createNewEventData(observer, delay, listener, count);
		
		msg.what = MESSAGE_ID;
		
		msg.arg1 = HANDLE_TIME_EVENT;
		msg.obj = newEventData;
		_sendMessageDelayed(msg, delay);
		
		return msg;
	}
	
	@SuppressWarnings("unused")
	private synchronized Object addRefreshTimeEventMessage(Message msg, Object observer, 
			TimeEventListener listener, long delay, int count) {
		Assert.assertTrue(msg != null);
		
		msg.what = MESSAGE_ID;
		
		msg.arg1 = HANDLE_TIME_EVENT;
		msg.obj = (Object)createAndSetEventData((TimeEventData)msg.obj, observer, delay, listener, count);
		_sendMessageDelayed(msg, delay);
		
		return msg;
	}
	
	
	/**
	 * Remove all messages
	 */
	public synchronized void removeAllMessages() {
		removeMessages(TimeEventManager.MESSAGE_ID);
	}
	
	/**
	 * Remove specified message
	 */
	public synchronized void removeMessage(Object msgId) {
		if (msgId == null)
			return;
		
		removeMessages(TimeEventManager.MESSAGE_ID, msgId);
	}
	
	/**
	 * Add event handling
	 */
	public synchronized void start() {
		handlerStatus = HANDLER_START;
		// addInitMessage(0);
	}
	
	/**
	 * Stop event handling.
	 */
	public synchronized void stop() {
		handlerStatus = HANDLER_STOP;
	}
	
	/**
	 * Handle user events
	 */
	public void handleMessage(Message msg) {
		TimeEventData eventData;
		int eventType;
		int delay;
		// boolean msgAdded = false;
		
		if (handlerStatus == HANDLER_STOP)
			return;
		
		try {
			synchronized (this) {
				eventData = (TimeEventData)msg.obj;
				eventType = msg.arg1;
				delay = msg.arg2;
				
				if (eventData.listener != null) {
					Assert.assertTrue(eventData.observer != null);
					
					if (eventType == HANDLE_TIME_EVENT) {
						// Call time event
						eventData.count++;
						
						// synchronized (eventData.observer) {
							eventData.listener.onTimeEvent(eventData.count, delay);
						// }
					}

					// Add time event
					// _sendMessageDelayed(msg, eventData.delay);
					// msgAdded = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			/* Ignored for performancing.
			if (msgAdded == false) {
				addInitMessage(HANDLE_REFRESH_TIME);
			}*/
		}
		
		super.handleMessage(msg);
	}
}