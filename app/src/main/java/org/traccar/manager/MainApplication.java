/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.manager;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;

import org.traccar.manager.model.User;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.LinkedList;
import java.util.List;

import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PREFERENCE_URL = "url";
    private static final String PREFERENCE_EMAIL = "email";
    private static final String PREFERENCE_PASSWORD = "password";

    public interface GetServiceCallback {
        void onServiceReady(WebService service);
    }

    private Handler handler = new Handler();
    private SharedPreferences preferences;
    private WebService service;
    private final List<GetServiceCallback> callbacks = new LinkedList<>();

    public void getServiceAsync(GetServiceCallback callback) {
        if (service != null) {
            callback.onServiceReady(service);
        } else {
            callbacks.add(callback);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        service = null;

        String url = preferences.getString(PREFERENCE_URL, null);
        String email = preferences.getString(PREFERENCE_EMAIL, null);
        String password = preferences.getString(PREFERENCE_PASSWORD, null);

        if (url != null && email != null && password != null) {
            OkHttpClient client = new OkHttpClient();
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            client.setCookieHandler(cookieManager);

            Retrofit retrofit = new Retrofit.Builder()
                    .client(client)
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            final WebService service = retrofit.create(WebService.class);

            service.addSession(email, password).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Response<User> response, Retrofit retrofit) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MainApplication.this.service = service;
                            for (GetServiceCallback callback : callbacks) {
                                callback.onServiceReady(service);
                            }
                            callbacks.clear();
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    Toast.makeText(MainApplication.this, R.string.error_connection, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

}