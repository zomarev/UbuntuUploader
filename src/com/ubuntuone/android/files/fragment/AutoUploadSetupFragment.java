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

package com.ubuntuone.android.files.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ubuntuone.android.files.Preferences;
import com.ubuntuone.android.files.R;
import com.ubuntuone.android.files.util.Log;

public class AutoUploadSetupFragment extends Fragment implements
		OnClickListener, OnCheckedChangeListener {
	private static final String TAG = AutoUploadSetupFragment.class.getSimpleName();
	
	private Controller mController;
	
	private ScrollView mContentScroll;
	private RadioGroup mRadioGroup;
	private TextView mMobileNotice;
	private Button mDoneButton;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(
				R.layout.fragment_autoupload_setup, container, false);
		mController = (Controller) getActivity();
		
		mRadioGroup = (RadioGroup) view.findViewById(R.id.auto_upload_mode);
		
		mContentScroll = (ScrollView) view.findViewById(R.id.content_scroll);
		mMobileNotice = (TextView) view.findViewById(R.id.auto_upload_mobile_notice);
		
		mDoneButton = (Button) view.findViewById(R.id.button_setup_done);
		mDoneButton.setOnClickListener(this);
		
		final RadioGroup autoUploadMode =
				(RadioGroup) view.findViewById(R.id.auto_upload_mode);
		autoUploadMode.setOnCheckedChangeListener(this);
		selectInitialAutoUploadMode(autoUploadMode);
		
		return view;
	}
	
	private void selectInitialAutoUploadMode(RadioGroup autoUploadMode) {
		final boolean autoUploadEnabled = Preferences.getBoolean(
				Preferences.PHOTO_UPLOAD_ENABLED_KEY, false);
		final boolean autoUploadWifiOnly = Preferences.getBoolean(
				Preferences.PHOTO_UPLOAD_ONLY_ON_WIFI, false);
		
		if (!autoUploadEnabled) {
			autoUploadMode.check(R.id.auto_upload_on_none);
		} else {
			if (autoUploadWifiOnly) {
				autoUploadMode.check(R.id.auto_upload_on_wifi);
			} else {
				autoUploadMode.check(R.id.auto_upload_on_both);
			}
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (group.getId() == R.id.auto_upload_mode) {
			switch (checkedId) {
			case R.id.auto_upload_on_both:
				showMobileNotice();
				break;
			case R.id.auto_upload_on_wifi:
				hideMobileNotice();
				break;
			case R.id.auto_upload_on_none:
				hideMobileNotice();
				break;

			default:
				Log.d(TAG, "unhandled onCheckedChanged source: " + group.getId());
				break;
			}
		}
	}

	private void showMobileNotice() {
		animateMobileNotice(View.VISIBLE);
		mContentScroll.smoothScrollTo(0, mContentScroll.getBottom());
	}

	private void hideMobileNotice() {
		animateMobileNotice(View.INVISIBLE);
	}

	private void animateMobileNotice(int visibility) {
		if (mMobileNotice.getVisibility() != visibility) {
			mMobileNotice.setAnimation(AnimationUtils.loadAnimation(
					getActivity(),
					visibility == View.VISIBLE ? R.anim.fade_in : R.anim.fade_out));
			mMobileNotice.setEnabled(visibility == View.VISIBLE);
			mMobileNotice.setVisibility(visibility);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_setup_done:
			int checkedId = mRadioGroup.getCheckedRadioButtonId();
			switch (checkedId) {
			case R.id.auto_upload_on_both:
				Preferences.putBoolean(Preferences.PHOTO_UPLOAD_ENABLED_KEY, true);
				Preferences.putBoolean(Preferences.PHOTO_UPLOAD_ONLY_ON_WIFI, false);
				break;
			case R.id.auto_upload_on_wifi:
				Preferences.putBoolean(Preferences.PHOTO_UPLOAD_ENABLED_KEY, true);
				Preferences.putBoolean(Preferences.PHOTO_UPLOAD_ONLY_ON_WIFI, true);
				break;
			case R.id.auto_upload_on_none:
				Preferences.putBoolean(Preferences.PHOTO_UPLOAD_ENABLED_KEY, false);
				break;

			default:
				Log.e(TAG, "unhandled radio group id " + checkedId);
				break;
			}
			Preferences.putBoolean(Preferences.PHOTO_UPLOAD_CONFIGURED_FLAG_KEY, true);
			
			if (mController != null) mController.onSetupDoneClicked(v);
			break;

		default:
			Log.d(TAG, "unhandled onClick source: " + v.getId());
			break;
		}
	}

	public interface Controller {
		public void onSetupDoneClicked(View v);
	}
}
