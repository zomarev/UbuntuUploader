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

package com.ubuntuone.android.files.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;

public abstract class EventsReceiver extends BroadcastReceiver
{
	boolean isConnected = false;
	boolean isWifi = false;
	boolean isRoaming = true;
	boolean isCharging = false;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
			updateValues(context);
			onConnectivityEventReceived(isConnected, isWifi, isRoaming);
		} else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
			int pluggedFlag = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
			int statusFlag = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isPlugged = (pluggedFlag != -1) &&
					(pluggedFlag == BatteryManager.BATTERY_PLUGGED_USB ||
					pluggedFlag == BatteryManager.BATTERY_PLUGGED_AC);
			boolean isCharging = (statusFlag != -1) &&
					(statusFlag == BatteryManager.BATTERY_STATUS_CHARGING ||
					statusFlag == BatteryManager.BATTERY_STATUS_FULL);
			onBatteryEventReceived(isPlugged || isCharging);
		}
	}
	
	public void updateValues(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		isConnected = ni != null && ni.isAvailable() && ni.isConnected();
		
		if (isConnected) {
			isWifi = ni.getType() == ConnectivityManager.TYPE_WIFI ||
					ni.getType() == ConnectivityManager.TYPE_WIMAX;
			isRoaming = ni.isRoaming();
		} else {
			isWifi = false;
			isRoaming = false;
		}
		onConnectivityEventReceived(isConnected, isWifi, isRoaming);
		
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent intent = context.registerReceiver(null, ifilter);
		int pluggedFlag = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		int statusFlag = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isPlugged = (pluggedFlag != -1) &&
				(pluggedFlag == BatteryManager.BATTERY_PLUGGED_USB ||
				pluggedFlag == BatteryManager.BATTERY_PLUGGED_AC);
		boolean isCharging = (statusFlag != -1) &&
				(statusFlag == BatteryManager.BATTERY_STATUS_CHARGING ||
				statusFlag == BatteryManager.BATTERY_STATUS_FULL);
		onBatteryEventReceived(isPlugged || isCharging);
	}
	
	public abstract void onConnectivityEventReceived(boolean isConnected,
			boolean isWifi, boolean isRoaming);
	
	public abstract void onBatteryEventReceived(boolean isCharging);
}
