/*
 * Ubuntu One Files - access Ubuntu One cloud storage on Android platform.
 * 
 * Copyright (C) 2011 Canonical Ltd.
 * Author: Michał Karnicki <michal.karnicki@canonical.com>
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

package com.ubuntuone.android.files.util;

import oauth.signpost.signature.PlainTextMessageSigner;

import org.apache.http.client.HttpClient;

import com.ubuntuone.android.files.Preferences;
import com.ubuntuone.api.sso.authorizer.OAuthAuthorizer;
import com.ubuntuone.api.sso.exceptions.TimeDriftException;

public final class Authorizer {
	private static OAuthAuthorizer sAuthorizer;
	
	public static synchronized OAuthAuthorizer getInstance(
			final HttpClient httpClient, boolean reinitialise)
			throws TimeDriftException {
		if (sAuthorizer == null || reinitialise) {
			sAuthorizer = OAuthAuthorizer.getWithTokens(
					Preferences.getSerializedOAuthToken(),
					new PlainTextMessageSigner());
		}
		/*
		// UpDown has been fixed to ignore timestamps which are way off.
		try {
			OAuthAuthorizer.syncTimeWithU1(httpClient);
		} catch (TimeDriftException e) {
			// Do nothing. User will be thrown to the login screen upon HTTP 401.
		}
		*/
		return sAuthorizer;
	}
}
