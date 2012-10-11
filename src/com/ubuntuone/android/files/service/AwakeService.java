/*
 * Ubuntu One Files - access Ubuntu One cloud storage on Android platform.
 * 
 * Copyright 2011-2012 Canonical Ltd.
 *   
 * This file is part of Ubuntu One Files.
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses 
 */

package com.ubuntuone.android.files.service;

import android.app.Service;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.ubuntuone.android.files.util.Log;

/**
 * A {@link Service}, which acquires wake and Wi-Fi locks for the period
 * of it's life cycle (between onCreate and onDestroy).
 */
public abstract class AwakeService extends Service
{
	protected String TAG = "AwakeService";
	
	private WakeLock mWakeLock;
	private WifiLock mWifiLock;

	@Override
	public void onCreate() {
		acquireWakeLock();
		acquireWifiLock();
		Log.d(TAG, "Acquired wake locks.");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		releaseWifiLock();
		releaseWakeLock();
		Log.d(TAG, "Released wake locks.");
		super.onDestroy();
	}
	
	private void acquireWakeLock() {
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
			mWakeLock.setReferenceCounted(false);
		}
		mWakeLock.acquire();
	}
	
	private void releaseWakeLock() {
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
	
	private void acquireWifiLock() {
		if (mWifiLock == null) {
			WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
			mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
			mWifiLock.setReferenceCounted(false);
		}
		mWifiLock.acquire();
	}
	
	private void releaseWifiLock() {
		if (mWifiLock != null && mWifiLock.isHeld()) {
			mWifiLock.release();
		}
	}
}
