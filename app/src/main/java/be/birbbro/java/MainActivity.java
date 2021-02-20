/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.birbbro.java;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import be.birbbro.R;
import be.birbbro.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static Double[] latestImageTimestamps = new Double[50];
    private static ArrayList<Double> visibleImageTimestamps = new ArrayList<>();
    private ViewPager viewPager;
    private View btnNext, btnPrev;
    private FragmentStatePagerAdapter viewPagerAdapter;
    private LinearLayout thumbnailsContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
        // [END handle_data_extras]

        Log.d(TAG, String.valueOf(R.string.msg_subscribed));
        // [START subscribe_topics]
        FirebaseMessaging.getInstance().subscribeToTopic(String.valueOf(R.string.default_notification_channel_name))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = getString(R.string.msg_subscribed);
                        if (!task.isSuccessful()) {
                            msg = getString(R.string.msg_subscribe_failed);
                        }
                        Log.d(TAG, msg);
                    }
                });
        // [END subscribe_topics]

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);

                        // Add to firebase realtime database
                        FirebaseDatabase database = FirebaseDatabase.getInstance("https://birb-bro-default-rtdb.europe-west1.firebasedatabase.app");
                        DatabaseReference myRef = database.getReference("fcm");
                        myRef.child(token).setValue(token);
                    }
                });


        //find view by id
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        thumbnailsContainer = (LinearLayout) findViewById(R.id.container);
        btnNext = findViewById(R.id.next);
        btnPrev = findViewById(R.id.prev);

        btnPrev.setOnClickListener(onClickListener(0));
        btnNext.setOnClickListener(onClickListener(1));

        refreshImages();

    }

    void refreshImages(){
        Arrays.fill(latestImageTimestamps, Double.valueOf(0));
        visibleImageTimestamps.clear();
        thumbnailsContainer.removeAllViews();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference listRef = storage.getReference();
        listRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference item : listResult.getItems()) {
                            String timestamp = item.getName().substring(0, item.getName().lastIndexOf("."));
                            if(Double.parseDouble(timestamp) > latestImageTimestamps[0]){
                                latestImageTimestamps[0] = Double.parseDouble(timestamp);
                            }
                            Arrays.sort(latestImageTimestamps);
                        }
                        Arrays.sort(latestImageTimestamps, Collections.reverseOrder());
                        setImagesData(latestImageTimestamps);
                        // init viewpager adapter and attach
                        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), visibleImageTimestamps);
                        viewPager.setAdapter(viewPagerAdapter);
                        inflateThumbnails();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Could not find images on storage");
                    }
                });
    }

    private View.OnClickListener onClickListener(final int i) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (i > 0) {
                    //next page
                    if (viewPager.getCurrentItem() < viewPager.getAdapter().getCount() - 1) {
                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                    }
                } else {
                    //previous page
                    if (viewPager.getCurrentItem() > 0) {
                        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                    }
                }
            }
        };
    }


    private void setImagesData(Double[] latestImageTimestamps) {
        for (int i = 0; i < latestImageTimestamps.length; i++) {
            if (latestImageTimestamps[i] > 0) {
                visibleImageTimestamps.add(latestImageTimestamps[i]);
            }
        }
    }

    private void inflateThumbnails() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        for (int i = 0; i < visibleImageTimestamps.size(); i++) {
            View imageLayout = getLayoutInflater().inflate(R.layout.item_image, null);
            ImageView imageView = (ImageView) imageLayout.findViewById(R.id.img_thumb);
            imageView.setOnClickListener(onChangePageClickListener(i));
            StorageReference imagerRef = storage.getReference().child(String.format("%.0f", visibleImageTimestamps.get(i)) + ".jpg");
            Glide.with(this /* context */)
                    .load(imagerRef)
                    .into(imageView);
            thumbnailsContainer.addView(imageLayout);
            TextView date = (TextView) imageLayout.findViewById(R.id.date_thumb);
            TextView time = (TextView) imageLayout.findViewById(R.id.time_thumb);
            long dv = (long)(visibleImageTimestamps.get(i)*1);// its need to be in milisecond
            Date df = new java.util.Date(dv);
            String date_string = new SimpleDateFormat("yyyy/MM/dd").format(df);
            String time_string = new SimpleDateFormat("HH:mm:ss").format(df);
            date.setText(date_string);
            time.setText(time_string);
        }
    }

    private View.OnClickListener onChangePageClickListener(final int i) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(i);
            }
        };
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshImages();
                Toast.makeText(MainActivity.this, getString(R.string.msg_refresh), Toast.LENGTH_SHORT).show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
