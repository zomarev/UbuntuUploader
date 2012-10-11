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

package com.ubuntuone.android.files.activity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.widget.ListAdapter;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.ubuntuone.android.files.Analytics;
import com.ubuntuone.android.files.Constants;
import com.ubuntuone.android.files.Preferences;
import com.ubuntuone.android.files.R;
import com.ubuntuone.android.files.UbuntuOneFiles;
import com.ubuntuone.android.files.provider.MetaUtilities;
import com.ubuntuone.android.files.provider.TransfersContract.TransferState;
import com.ubuntuone.android.files.provider.TransfersContract.Uploads;
import com.ubuntuone.android.files.service.AutoUploadService;
import com.ubuntuone.android.files.service.MetaServiceHelper;
import com.ubuntuone.android.files.service.UpDownService;
import com.ubuntuone.android.files.util.BrowserUtilities;
import com.ubuntuone.android.files.util.ChangeLogUtils;
import com.ubuntuone.android.files.util.ConfigUtilities;
import com.ubuntuone.android.files.util.FileUtilities;
import com.ubuntuone.android.files.util.Log;
import com.ubuntuone.android.files.util.TransferUtils;
import com.ubuntuone.android.files.util.U1CroppedImageDownloader;
import com.ubuntuone.android.files.util.U1RegularImageDownloader;
import com.ubuntuone.android.files.util.UIUtil;

/**
 * Activity to manipulate application preferences. {@link Preference} keys can
 * be found as public fields of {@link Preferences} class. The {@link Pref}
 * interface contains keys of preferences we need a handle for, but don't use
 * them to store actual values, just reference the according {@link View}s.
 */
@SuppressWarnings("deprecation") // TODO Update GA tracker calls.
public final class PreferencesActivity extends PreferenceActivity implements
		OnPreferenceClickListener
{
	private static final String TAG = PreferencesActivity.class.getSimpleName();
	
	public static final int RESULT_UNLINKED = 1;
	
	public static String PURCHASE_STORAGE_SCREEN = "upgrade_storage"; 
	public static String AUTOUPLOAD_SCREEN = "auto_upload";
	public static String SHOW_RETRY_FAILED = "retry_failed";
	
	/**
	 * {@link Preference} keys to retrieve views, we're not using them to
	 * manipulate {@link Preference} values.
	 */
	private static interface Pref {
		public static final String INVITE_FRIEND = "invite_friend";
		public static final String PURCHASE_STORAGE = "purchase_storage";
		public static final String UPGRADE_PLAN = "upgrade_plan";
		
		public static final String MEDIA_UPLOAD = "media_upload";
		public static final String PHOTOS_AUTO_UPLOAD_SOURCES = "upload_photos_src";
		public static final String UPLOAD_PHOTOS_NOW = "upload_photos_now";
		public static final String CANCEL_ALL_UPLOADS = "cancel_all_uploads";
		
		public static final String ADVANCED_OPTIONS = "advanced_options";
		public static final String CLEAR_PREVIEW_CACHE = "clear_preview_cache";
		public static final String CLEAR_THUMB_CACHE = "clear_thumb_cache";
		public static final String REMOVE_DEVICE = "remove_device";
		
		public static final String RETRY_FAILED = "retry_failed";
		public static final String CANCEL_FAILED = "cancel_failed";
		
		public static final String MANAGE_ACCOUNT = "manage_online";
		public static final String SUPPORT_OPTIONS = "support_options";
		
		public static final String FEEDBACK = "feedback";
		public static final String REPORT_PROBLEM = "report_problem";
		public static final String COLLECT_LOGS = "collect_logs";
		public static final String REVIEW_LOGS = "review_logs";
		public static final String SEND_LOGS = "send_logs";
		
		public static final String ABOUT = "about";
		public static final String LICENSE = "license";
		public static final String CHANGELOG ="changelog";
		public static final String AUTHORS = "authors";
		public static final String WEBPAGE = "webpage";
		public static final String GREENDROID = "greendroid";
	}
	
	/** URLs to couple of useful pages. */
	private static interface Url {
		public static final String UPGRADE =
				"https://one.ubuntu.com/plans/";
		public static final String MANAGE =
				"https://one.ubuntu.com/account/";
		public static final String SUPPORT =
				"https://one.ubuntu.com/support/";
		public static final String FEEDBACK =
				"https://answers.edge.launchpad.net/ubuntuone-android-files/+faqs";
		public static final String LICENSE =
				"http://www.gnu.org/licenses/agpl.html";
		public static final String AUTHORS =
				"https://launchpad.net/~ubuntuone-android-hackers";
		public static final String WEBPAGE =
				"https://launchpad.net/ubuntuone-android-files";
		public static final String GREENDROID =
				"https://github.com/cyrilmottier/GreenDroid";
	}
	
	private GoogleAnalyticsTracker mTracker;
	
	private Context mContext;
	
	private Preference mPhotosAutoUploadDir;
	private Preference mRetryFailed;

	private Preference mCancelFailed;
	private CheckBoxPreference mCollectLogs;
	private Preference mReviewLogs;
	private Preference mSendLogs;
	
	private BroadcastReceiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		
		mTracker = GoogleAnalyticsTracker.getInstance();
		mTracker.start(Analytics.U1F_ACCOUNT, this);
		mTracker.trackPageView(TAG);

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		// Account category. 
		
		final Preference username = findPreference(Preferences.USERNAME_KEY);
		
		final String usernameSummary =
				Preferences.getString(Preferences.USERNAME_KEY, "(?)");
		username.setSummary(usernameSummary);
		
		setupOnClickPreference(Pref.INVITE_FRIEND, this);
		
		final Preference purchaseStorage = findPreference(Pref.PURCHASE_STORAGE);
		purchaseStorage.setOnPreferenceClickListener(mPurchaseStorageListener);
		
		final Preference upgradePlan = findPreference(Pref.UPGRADE_PLAN);
		upgradePlan.setOnPreferenceClickListener(mLinkListener);
		
		// Options category.
		
		final PreferenceScreen mediaUploadScreen = 
				(PreferenceScreen) findPreference(Pref.MEDIA_UPLOAD);
		mediaUploadScreen.setOnPreferenceClickListener(whiteHackClick);
		
		// Photo Auto Upload
		
		final CheckBoxPreference photosAutoUploadToggle =
				(CheckBoxPreference) findPreference(Preferences.PHOTO_UPLOAD_ENABLED_KEY);
		photosAutoUploadToggle.setOnPreferenceClickListener(mPhotosAutoUploadToggleListener);
		
		final Preference photosAutoUploadSources = findPreference(Pref.PHOTOS_AUTO_UPLOAD_SOURCES);
		photosAutoUploadSources.setOnPreferenceClickListener(mPhotosAutoUploadSourcesListener);
		
		mPhotosAutoUploadDir =
				(Preference) findPreference(Preferences.PHOTO_UPLOAD_DIR_KEY);
		mPhotosAutoUploadDir.setOnPreferenceClickListener(mPhotosAutoUploadDirListener);
		final String photosAutoUploadDirectory = Preferences.getPhotoUploadDirectory();
		mPhotosAutoUploadDir.setDefaultValue(photosAutoUploadDirectory);
		mPhotosAutoUploadDir.setSummary(photosAutoUploadDirectory);
		
		final Preference uploadPhotosNow = findPreference(Pref.UPLOAD_PHOTOS_NOW);
		uploadPhotosNow.setOnPreferenceClickListener(mUploadPhotosNowListener);
		
		final Preference cancelAllUploads = findPreference(Pref.CANCEL_ALL_UPLOADS);
		cancelAllUploads.setOnPreferenceClickListener(mCancelAllUploadsListener);
		
		// General
		
		final PreferenceScreen advancedOptionsScreen = 
			(PreferenceScreen) findPreference(Pref.ADVANCED_OPTIONS);
		advancedOptionsScreen.setOnPreferenceClickListener(whiteHackClick);
		
		final Preference autoRetry = findPreference(Preferences.AUTO_RETRY_FAILED);
		autoRetry.setOnPreferenceChangeListener(mAutoRetryListener);
		
		final Preference clearPreviewCache = findPreference(Pref.CLEAR_PREVIEW_CACHE);
		clearPreviewCache.setOnPreferenceClickListener(mClearPreviewCacheListener);
		
		final Preference clearThumbCache = findPreference(Pref.CLEAR_THUMB_CACHE);
		clearThumbCache.setOnPreferenceClickListener(mClearThumbCacheListener);
		
		final Preference removeDevice = findPreference(Pref.REMOVE_DEVICE);
		removeDevice.setOnPreferenceClickListener(mRemoveDeviceListener);
		
		mRetryFailed = findPreference(Pref.RETRY_FAILED);
		mRetryFailed.setOnPreferenceClickListener(mRetryFailedListener);
		
		mCancelFailed = findPreference(Pref.CANCEL_FAILED);
		mCancelFailed.setOnPreferenceClickListener(mCancelFailedListener);
		
		// Manage category.
		
		final Preference manageAccount = findPreference(Pref.MANAGE_ACCOUNT);
		manageAccount.setOnPreferenceClickListener(mLinkListener);
		
		final Preference supportOptions = findPreference(Pref.SUPPORT_OPTIONS);
		supportOptions.setOnPreferenceClickListener(mLinkListener);
		
		final Preference feedback = findPreference(Pref.FEEDBACK);
		feedback.setOnPreferenceClickListener(mFeedbackListener);
		
		final PreferenceScreen reportScreen =
				(PreferenceScreen) findPreference(Pref.REPORT_PROBLEM);
		reportScreen.setOnPreferenceClickListener(whiteHackClick);
		
		mCollectLogs = (CheckBoxPreference) findPreference(Pref.COLLECT_LOGS);
		mCollectLogs.setOnPreferenceClickListener(mCollectLogsListener);
		
		mReviewLogs = findPreference(Pref.REVIEW_LOGS);
		mReviewLogs.setOnPreferenceClickListener(mReviewLogsListener);
		
		mSendLogs = findPreference(Pref.SEND_LOGS);
		mSendLogs.setOnPreferenceClickListener(mSendLogsListener);
		
		final PreferenceScreen about =
				(PreferenceScreen) findPreference(Pref.ABOUT);
		about.setSummary(UbuntuOneFiles.getVersion());
		about.setOnPreferenceClickListener(whiteHackClick);
		
		final Preference license = findPreference(Pref.LICENSE);
		license.setOnPreferenceClickListener(mLinkListener);
		
		final Preference changelog = findPreference(Pref.CHANGELOG);
		changelog.setOnPreferenceClickListener(mChangelogListener);
		
		final Preference author = findPreference(Pref.AUTHORS);
		author.setOnPreferenceClickListener(mLinkListener);
		
		final Preference webpage = findPreference(Pref.WEBPAGE);
		webpage.setOnPreferenceClickListener(mLinkListener);
		
		final Preference greendroid = findPreference(Pref.GREENDROID);
		greendroid.setOnPreferenceClickListener(mLinkListener);
		
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (UpDownService.BROADCAST_UPLOAD_INFO.equals(action)) {
					final String info = intent.getStringExtra(Intent.EXTRA_TEXT);
					if (info != null) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								adjustTransferPreferencesState();
								Preference p = findPreference(Pref.UPLOAD_PHOTOS_NOW);
								if (p != null) {
									p.setSummary(info);
								}
							}
						});
					}
				}
			}
		};
		registerAutoUploadInfoReceiver(receiver);
		
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(AUTOUPLOAD_SCREEN)) {
				openPreference(Pref.MEDIA_UPLOAD);
			} else if (intent.hasExtra(PURCHASE_STORAGE_SCREEN)) {
				openPreference(Pref.PURCHASE_STORAGE);
			} else if (intent.hasExtra(SHOW_RETRY_FAILED)) {
				// Simply shows top screen. Hilight retry failed pref?
			}
		}
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		if (key.equals(Pref.INVITE_FRIEND)) {
			onInviteFriendClicked(preference);
			return true;
		}
		return false;
	}
	
	private void setupOnClickPreference(String key, String summary,
			OnPreferenceClickListener listener) {
		Preference preference = findPreference(key);
		if (summary != null) {
			preference.setSummary(summary);
		}
		if (listener == null) {
			throw new IllegalArgumentException(
					"OnPreferenceClickListener can not be null " +
					"for preference " + key);
		}
		preference.setOnPreferenceClickListener(listener);
	}
	
	private void setupOnClickPreference(String key, OnPreferenceClickListener listener) {
		setupOnClickPreference(key, null, listener);
	}
	
	private void onInviteFriendClicked(Preference preference) {
		long userId = Preferences.getLong(Preferences.USER_ID_KEY, -1);
		if (userId == -1) {
			UIUtil.showToast(mContext,
					R.string.preferences_activity_please_wait_updating_user_info,
					true);
			MetaServiceHelper.getUserInfo(this, null);
			return;
		}
		
		final String referral_fmt = "https://one.ubuntu.com/referrals/referee/%s/";
		String url = String.format(referral_fmt, userId);
		UIUtil.shareLink(this, url, true);
	}

	private void registerAutoUploadInfoReceiver(BroadcastReceiver receiver) {
		IntentFilter filter = new IntentFilter(UpDownService.BROADCAST_UPLOAD_INFO);
		registerReceiver(receiver, filter);
	}
	
	private void unregisterAutoUploadInfoReceiver(BroadcastReceiver receiver) {
		unregisterReceiver(receiver);
	}
	
	@Override
	protected void onPostResume() {
		super.onPostResume();
		
		final Preference plan = findPreference(Preferences.PLAN_KEY);
		setUpCurrentPlanSummary(plan);
	}
	
	private void setUpCurrentPlanSummary(Preference plan) {
		final String currentPlan =
			Preferences.getString(Preferences.PLAN_KEY, "(?)");
		final long usedBytes =
			Preferences.getLong(Preferences.USED_BYTES_KEY, 0L);
		final long maxBytes =
				Preferences.getLong(Preferences.MAX_BYTES_KEY, 1L);
		final double usedPercentage = 100 * usedBytes / (double) maxBytes;
		
		final String spaceDetails = getString(
				R.string.preferences_activity_bytes_used_summary_fmt,
				FileUtilities.getHumanReadableSize(usedBytes),
				FileUtilities.getHumanReadableSize(maxBytes),
				usedPercentage);
		final String planSummary = currentPlan + spaceDetails;
		plan.setSummary(planSummary);
	}

	protected void onResume() {
		super.onResume();
		adjustTransferPreferencesState();
	}

	@Override
	public void onDestroy() {
		unregisterAutoUploadInfoReceiver(receiver);
		
		mTracker.dispatch();
		mTracker.stop();
		super.onDestroy();
	}
	
	// Images Auto Upload

	private OnPreferenceClickListener mPhotosAutoUploadToggleListener =
			new OnPreferenceClickListener() {
		
		public boolean onPreferenceClick(Preference preference) {
			final boolean upload = Preferences.getBoolean(
					Preferences.PHOTO_UPLOAD_ENABLED_KEY,
					false);
			mTracker.trackEvent("Settings", "AutoUpload",
					"photos", upload ? 1 : 0);
			
			
			if (upload) {
				// User has enabled the auto-upload. We should be uploading
				// only the pictures taken since *this* moment.
				Preferences.updateLastPhotoUploadTimestamp();
				startUploadService();
			} else {
				TransferUtils.clearAutoUploads(getContentResolver());
				stopUploadService();
			}
			return true;
		}
	};
	
	private OnPreferenceClickListener mPhotosAutoUploadSourcesListener =
			new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			final Intent intent = new Intent(PreferencesActivity.this,
					AutoUploadCustomizeActivity.class);
			startActivity(intent);
			return true;
		}
	};
	
	private OnPreferenceClickListener mPhotosAutoUploadDirListener =
			new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
			final Intent intent = new Intent(
					FilesActivity.ACTION_PICK_AUTO_UPLOAD_DIRECTORY);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return false;
		}
	};
	
	private OnPreferenceClickListener mUploadPhotosNowListener =
			new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			final OnClickListener onClickListener = new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case Dialog.BUTTON_POSITIVE:
						Log.i(TAG, "Uploading all images now.");
						final Intent intent = new Intent(
								AutoUploadService.ACTION_RESCAN_IMAGES);
						startService(intent);
						break;
					case Dialog.BUTTON_NEGATIVE:
						dialog.dismiss();
						break;
					default:
						break;
					}
				}
			};
			
			final AlertDialog dialog =
					new AlertDialog.Builder(PreferencesActivity.this)
					.setTitle(R.string.preferences_activity_upload_all_photos_title)
					.setMessage(R.string.preferences_activity_upload_all_photos_message)
					.setPositiveButton(R.string.yes, onClickListener)
					.setNegativeButton(R.string.no, onClickListener)
					.create();
			dialog.show();
			return true;
		}
	};
	
	private OnPreferenceClickListener mCancelAllUploadsListener =
			new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Intent intent = new Intent(UpDownService.ACTION_CANCEL_UPLOAD);
			LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(
					PreferencesActivity.this);
			lbm.sendBroadcast(intent);
			return true;
		}
	};
	
	private void startUploadService() {
		if (!AutoUploadService.startFrom(this)) {
			Log.w(TAG, "Failed to start Auto Upload service");
			UIUtil.showToast(mContext, R.string.toast_failed_to_start_bg_service);
		}
	}
	
	private void stopUploadService() {
		AutoUploadService.stopFrom(this);
	}
	
	private OnPreferenceChangeListener mAutoRetryListener =
			new OnPreferenceChangeListener() {
		
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			boolean retry = (Boolean) newValue;
			mTracker.trackEvent("Settings", "Advanced",
					"auto_retry", retry ? 1 : 0);
			
			if (retry) {
				if (TransferUtils.getFailedUploadsCount(getContentResolver()) > 0) {
					startService(new Intent(UpDownService.ACTION_RETRY));
				}
			}
			return true;
		}
		
	};
	
	private OnPreferenceClickListener mRetryFailedListener =
			new OnPreferenceClickListener() {
		
		public boolean onPreferenceClick(Preference preference) {
			if (adjustTransferPreferencesState()) {
				mTracker.trackEvent("Settings", "Options", "retry_failed", 1);
				UIUtil.showToast(mContext, R.string.toast_retrying_transfers_now);
				startService(new Intent(UpDownService.ACTION_RETRY));
			} else {
				UIUtil.showToast(mContext, R.string.toast_retrying_transfers_failed);
			}
			return true;
		}
		
	};
	
	private OnPreferenceClickListener mCancelFailedListener =
			new OnPreferenceClickListener() {
	
		public boolean onPreferenceClick(Preference preference) {
			mTracker.trackEvent("Settings", "Options", "cancel_failed", 1);
			TransferUtils.dequeue(getContentResolver(), Uploads.CONTENT_URI,
					TransferState.FAILED);
			MetaUtilities.cancelFailedTransfers();
			adjustTransferPreferencesState();
			return true;
		}
		
	};
	
	/**
	 * Adjust the android:enabled state of RETRY_FAILED and CANCEL_FAILED
	 * preferences based on number of pending failed uploads.
	 * 
	 * @return true if there are pending uploads, false otherwise
	 */
	private boolean adjustTransferPreferencesState() {
		int failedUploads = TransferUtils.getFailedUploadsCount(
				getContentResolver());
		if (failedUploads > 0) {
			mRetryFailed.setEnabled(true);
			mRetryFailed.setSummary(
					R.string.preferences_activity_retry_failed_uploads_summary_pending);
			mCancelFailed.setEnabled(true);
			return true;
		} else {
			mRetryFailed.setEnabled(false);
			mRetryFailed.setSummary(
					R.string.preferences_activity_retry_failed_uploads_summary_none);
			mCancelFailed.setEnabled(false);
			return false;
		}
	}
	
	private OnPreferenceClickListener mClearPreviewCacheListener =
			new OnPreferenceClickListener() {
		
		@Override
		public boolean onPreferenceClick(Preference preference) {
			new ClearThumbsCacheTask().execute(U1RegularImageDownloader.SIZE_MEDIUM);
			return true;
		}
	};
	
	private OnPreferenceClickListener mClearThumbCacheListener =
			new OnPreferenceClickListener() {
		
		@Override
		public boolean onPreferenceClick(Preference preference) {
			new ClearThumbsCacheTask().execute(U1CroppedImageDownloader.SIZE_SMALL);
			return true;
		}
	};
	
	private class ClearThumbsCacheTask extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			final int thumbSize = params[0];
			FileUtilities.clearThumbsCacheDirectory(thumbSize);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			UIUtil.showToast(PreferencesActivity.this,
					R.string.toast_cache_cleared);
		}
	};
	
	private OnPreferenceClickListener mRemoveDeviceListener =
			new OnPreferenceClickListener() {
		
		private DialogInterface.OnClickListener onClick =
				new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case Dialog.BUTTON_POSITIVE:
					// Remove access token.
					final String authToken = Preferences.getSerializedOAuthToken();
					if (authToken != null) {
						final AccountManager am =
								AccountManager.get(PreferencesActivity.this);
						Log.i(TAG, "Invalidating auth token.");
						am.invalidateAuthToken(
								Constants.ACCOUNT_TYPE, authToken);
					}
					Preferences.updateSerializedOAuthToken(null);
					
					mTracker.trackEvent("Settings", "Options", "unlink", 1);
					mTracker.dispatch();
					
					PreferencesActivity.this.setResult(RESULT_UNLINKED);
					finish();
					break;
				case Dialog.BUTTON_NEGATIVE:
					dialog.dismiss();
					break;
				default:
					Log.e(TAG, "no such button");
					break;
				}
			}
			
		};
		
		public boolean onPreferenceClick(Preference preference) {
			final AlertDialog dialog =
					new AlertDialog.Builder(mContext)
				.setTitle(R.string.dialog_remove_device_title)
				.setMessage(R.string.dialog_remove_device_message)
				.setPositiveButton(R.string.remove, onClick)
				.setNegativeButton(R.string.cancel, onClick)
				.create();
			dialog.setOwnerActivity(PreferencesActivity.this);
			dialog.show();
			return true;
		}
		
	};
	
	private OnPreferenceClickListener mPurchaseStorageListener =
			new OnPreferenceClickListener() {
		
		public boolean onPreferenceClick(Preference preference) {
			StoreActivity.startFrom(mContext);
			return true;
		}
	};
	
	private OnPreferenceClickListener mLinkListener =
			new OnPreferenceClickListener() {

		public boolean onPreferenceClick(Preference preference) {
			final String key = preference.getKey();
			if (key.equals(Pref.UPGRADE_PLAN)) {
				mTracker.trackEvent("Settings", "Links", "upgrade", 1);
				BrowserUtilities.open(mContext, Url.UPGRADE);
			} else if (key.equals(Pref.MANAGE_ACCOUNT)) {
				BrowserUtilities.open(mContext, Url.MANAGE);
			} else if (key.equals(Pref.SUPPORT_OPTIONS)) {
				mTracker.trackEvent("Settings", "Links", "support", 1);
				BrowserUtilities.open(mContext, Url.SUPPORT);
			} else if (key.equals(Pref.FEEDBACK)) {
				BrowserUtilities.open(mContext, Url.FEEDBACK);
			} else if (key.equals(Pref.LICENSE)) {
				BrowserUtilities.open(PreferencesActivity.this, Url.LICENSE);
			} else if (key.equals(Pref.AUTHORS)) {
				BrowserUtilities.open(mContext, Url.AUTHORS);
			} else if (key.equals(Pref.WEBPAGE)) {
				BrowserUtilities.open(mContext, Url.WEBPAGE);
			} else if (key.equals(Pref.GREENDROID)) {
				BrowserUtilities.open(mContext, Url.GREENDROID);
			}
			return true;
		}
		
	};
	
	private OnPreferenceClickListener mFeedbackListener =
			new OnPreferenceClickListener() {
		
		private final String EMAIL_TARGET = "ubuntuone-support@canonical.com";
		private final String EMAIL_SUBJECT = "Ubuntu One Files Feedback";
		
		public boolean onPreferenceClick(Preference preference) {
			final String details = getDetails();
			final String body = "Your feedback:\n";
			
			final Intent email = new Intent(Intent.ACTION_SEND);
			email.setType("message/rfc822");
			email.putExtra(Intent.EXTRA_EMAIL,
					new String[] { EMAIL_TARGET });
			email.putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT);
			email.putExtra(Intent.EXTRA_TEXT, details + body);
			try {
				startActivity(email);
			} catch (ActivityNotFoundException e) {
				UIUtil.showToast(mContext, "No e-mail app?");
			}
			return false;
		}
		
	};
	
	private OnPreferenceClickListener mCollectLogsListener =
			new OnPreferenceClickListener() {
		
		public boolean onPreferenceClick(Preference preference) {
			if (mCollectLogs.isChecked()) {
				if (ConfigUtilities.isExternalStorageMounted()) {
					final boolean isLogging = Log.enableCollectingLogs();
					if (isLogging) {
						return false; // Let CheckBox be checked.
					} else {
						UIUtil.showToast(mContext, "Could not start logging.", true);
						return true;
					}
				} else {
					mCollectLogs.setChecked(false);
					UIUtil.showToast(mContext, R.string.need_to_mount_storage, true);
					return true;
				}
			} else {
				Log.disableCollectingLogs();
				return false;
			}
		}
		
	};
	
	private OnPreferenceClickListener mReviewLogsListener =
			new OnPreferenceClickListener() {
		
		public boolean onPreferenceClick(Preference preference) {
			if (ConfigUtilities.isExternalStorageMounted()) {
				reviewLogs();
			} else {
				UIUtil.showToast(mContext, R.string.need_to_mount_storage, true);
			}
			return false;
		}
		
	};
	
	private OnPreferenceClickListener mSendLogsListener =
			new OnPreferenceClickListener() {
		
		public boolean onPreferenceClick(Preference preference) {
			sendLogs();
			return false;
		}
		
	};
	
	private OnPreferenceClickListener mChangelogListener =
			new OnPreferenceClickListener() {
		
		public boolean onPreferenceClick(Preference preference) {
			ChangeLogUtils.showChangelog(PreferencesActivity.this);
			return true;
		}
		
	};
	
	@SuppressWarnings("unused")
	private void collectLogs() throws Exception {
		final File logFile = getLogFile();
		if (logFile.exists()) {
			logFile.delete();
		}
		boolean created = logFile.createNewFile();
		if (!created) {
			throw new Exception(getString(R.string.cant_create_log));
		}
		if (!logFile.canWrite()) {
			throw new Exception(getString(R.string.cant_write_log));
		}
		
		java.lang.Process logcat = null;
		BufferedReader reader = null;
		BufferedWriter writer = null;
		
		logcat = Runtime.getRuntime().exec(new String[] {
				"logcat", "-d", "AndroidRuntime:E System.err:V UbuntuOneFiles:D *:S" });
		reader = new BufferedReader(new InputStreamReader(
				logcat.getInputStream()));
		writer = new BufferedWriter(new FileWriter(logFile));
		
		Log.d(TAG, "collecting logs now");
		String line;
		final String newLine = System.getProperty("line.separator");
		while ((line = reader.readLine()) != null) {
			writer.write(line);
			writer.write(newLine);
		}
		writer.flush();
		writer.close();
	}
	
	private void reviewLogs() {
		final File logFile = getLogFile();
		final Uri uri = Uri.fromFile(logFile);
		Log.i(TAG, "reviewing " + uri.toString());
		final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.setDataAndType(uri, "text/plain");
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			UIUtil.showToast(mContext, R.string.no_text_viewer, true);
		}
	}
	
	private final String getDetails() {
		final String version = UbuntuOneFiles.getVersion();
		final String details = Build.MODEL
				+ " running " + Build.VERSION.RELEASE
				+ ", Ubuntu One Files " + version + "\n\n";
		return details;
	}
	
	private static final String LOG_EMAIL_TARGET = "ubuntuone-support@canonical.com";
	private static final String LOG_EMAIL_SUBJECT = "Ubuntu One Files Logs";
	private static final String LOG_FILENAME = "logs.txt";
	
	private void sendLogs() {
		if (ConfigUtilities.isExternalStorageMounted()) {
			final String details = getDetails();
			final File logFile = getLogFile();
			final Uri uri = Uri.fromFile(logFile);
			
			if (logFile.exists()) {
				final Intent email = new Intent(Intent.ACTION_SEND);
				email.setType("message/rfc822");
				email.putExtra(Intent.EXTRA_EMAIL,
						new String[] { LOG_EMAIL_TARGET });
				email.putExtra(Intent.EXTRA_SUBJECT, LOG_EMAIL_SUBJECT);
				email.putExtra(Intent.EXTRA_TEXT, details);
				email.putExtra(Intent.EXTRA_STREAM, uri);
				startActivity(email);
				mCollectLogs.setChecked(false);
			}
		}
	}
	
	public static final File getLogFile() {
		final File extStorageDir =
				Environment.getExternalStorageDirectory();
		final String pkg =
				UbuntuOneFiles.class.getPackage().getName();
		final String dir =
				String.format("%s/Android/data/%s/files/log",
						extStorageDir, pkg);
		final File logDir = new File(dir);
		logDir.mkdirs();
		final File logFile = new File(logDir, LOG_FILENAME);
		return logFile;
	}

	public static void showFrom(Context context) {
		final Intent intent = new Intent(context, PreferencesActivity.class);
		context.startActivity(intent);
	}
	
	/** Hack for Theme.Light in sub {@link PreferenceScreen}. */
	private OnPreferenceClickListener whiteHackClick =
			new OnPreferenceClickListener() {
		
		public boolean onPreferenceClick(Preference preference) {
			final PreferenceScreen screen = (PreferenceScreen) preference;
			final Window window = screen.getDialog().getWindow();
			window.setBackgroundDrawableResource(android.R.color.white);
			return true;
		}
		
	};
	
	// See http://stackoverflow.com/questions/4805896/how-to-open-or-
	// simulate-a-click-on-a-android-preference-which-was-created-with/4869034#4869034
	
	private void openPreference(String key) {
		PreferenceScreen screen = getPreferenceScreen();
		Log.d(TAG, "screen not null");
		if (screen != null) {
			ListAdapter adapter = getPreferenceScreen().getRootAdapter();
			for (int i = 0; i < adapter.getCount(); i++) {
				Preference p = (Preference) adapter.getItem(i);
				Log.d(TAG, "key is: " + p.getKey());
				if (p.getKey().equals(key)) {
					screen.onItemClick(null, null, i, 0);
					break;
				}
			}
		}
	}
}
