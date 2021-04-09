package be.birbbro.java;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import be.birbbro.R;
import be.birbbro.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BirdBro";
    private String storageFolder = "default";
    private static Double[] latestImageTimestamps = new Double[50];
    private static ArrayList<Double> visibleImageTimestamps = new ArrayList<>();
    private ViewPager viewPager;
    private View btnNext, btnPrev;
    private ViewPagerAdapter viewPagerAdapter;
    private LinearLayout thumbnailsContainer;
    private Classifier classifier;
    private FirebaseAuth mAuth;
    private String email = "YOUR_FIREBASE_AUTH_EMAIL";
    private String password = "YOUR_FIREBASE_AUTH_PASSWORD";
    private FirebaseDatabase database = FirebaseDatabase.getInstance("https://YOUR_RTDB_ID.europe-west1.firebasedatabase.app");
    private String modelName = "YOURMODELNAME.pt";
    private FirebaseUser user;
    private HashMap<String, ClassifierResult> predictedImages =new HashMap<String, ClassifierResult>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //find view by id
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        thumbnailsContainer = (LinearLayout) findViewById(R.id.thumbnail_contailer);
        btnNext = findViewById(R.id.next);
        btnPrev = findViewById(R.id.prev);

        btnPrev.setOnClickListener(onClickListener(0));
        btnNext.setOnClickListener(onClickListener(1));

        classifier = new Classifier(Utils.assetFilePath(this, modelName));
        if(user == null){
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                user = mAuth.getCurrentUser();
                                startUpFirebase();
                            } else {
                                Log.w(TAG, getString(R.string.firebase_login_error), task.getException());
                                Toast.makeText(MainActivity.this, getString(R.string.firebase_login_error),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }else{
            startUpFirebase();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshImages();
    }

    void startUpFirebase(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        Log.d(TAG, String.valueOf(R.string.msg_subscribed));
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

                        // Log
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);

                        // Add to firebase realtime database
                        DatabaseReference myRef = database.getReference(storageFolder);
                        myRef.child(token).setValue(token);
                    }
                });
    }

    void refreshImages(){

        Arrays.fill(latestImageTimestamps, Double.valueOf(0));
        visibleImageTimestamps.clear();
        thumbnailsContainer.removeAllViews();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference listRef = storage.getReference().child(storageFolder);
        listRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference item : listResult.getItems()) {
                            final String timestamp = item.getName().substring(0, item.getName().lastIndexOf("."));
                            if(Double.parseDouble(timestamp) > latestImageTimestamps[0]){
                                latestImageTimestamps[0] = Double.parseDouble(timestamp);
                            }
                            Arrays.sort(latestImageTimestamps);
                        }
                        Arrays.sort(latestImageTimestamps, Collections.reverseOrder());
                        for (int i = 0; i < latestImageTimestamps.length; i++) {
                            if(latestImageTimestamps[i] > 0) {
                                final String timestamp = String.format("%.0f", latestImageTimestamps[i]);
                                if(!predictedImages.containsKey(timestamp)){
                                    FirebaseStorage storageInteral = FirebaseStorage.getInstance();
                                    StorageReference imagerRef = storageInteral.getReference().child(storageFolder + "/" + timestamp + ".jpg");
                                    try {
                                        final File localFile = File.createTempFile("Images", "jpg");
                                        final int current_index = i;
                                        OnSuccessListener<FileDownloadTask.TaskSnapshot> onSuccessListener = new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                                Bitmap currentImage = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                                ClassifierResult result = classifier.predict(currentImage);
                                                predictedImages.put(timestamp, result);
                                                if (current_index == latestImageTimestamps.length - 1) {
                                                    inflateUI();
                                                } else if (latestImageTimestamps[current_index + 1] == 0) {
                                                    inflateUI();
                                                }
                                            }
                                        };
                                        imagerRef.getFile(localFile).addOnSuccessListener(onSuccessListener);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }else{
                                    if (i == latestImageTimestamps.length - 1) {
                                        inflateUI();
                                    } else if (latestImageTimestamps[i + 1] == 0) {
                                        inflateUI();
                                    }
                                }

                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, getString(R.string.no_images_error));
                    }
                });
    }

    private void inflateUI(){
        setImagesData(latestImageTimestamps);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), visibleImageTimestamps, predictedImages, getString(R.string.default_class), storageFolder);
        viewPager.setAdapter(viewPagerAdapter);
        inflateThumbnails();

        findViewById(R.id.thubmnails_header).setVisibility(View.VISIBLE);
        findViewById(R.id.pager_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.loading_text).setVisibility(View.INVISIBLE);

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
            StorageReference imagerRef = storage.getReference().child(storageFolder + "/" + String.format("%.0f", visibleImageTimestamps.get(i)) + ".jpg");
            Glide.with(this /* context */)
                    .load(imagerRef)
                    .into(imageView);
            thumbnailsContainer.addView(imageLayout);
            TextView date = (TextView) imageLayout.findViewById(R.id.date_thumb);
            TextView time = (TextView) imageLayout.findViewById(R.id.time_thumb);
            long dv = (long)(visibleImageTimestamps.get(i)*1000);// its need to be in milisecond
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            case R.id.action_delete:
                deleteCurrentImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void deleteCurrentImage() {
        String timestamp = viewPagerAdapter.getTimestamp(viewPager.getCurrentItem());
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference deleteRef = storage.getReference().child(storageFolder + "/" + timestamp + ".jpg");
        // Delete the file
        deleteRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(MainActivity.this, getString(R.string.msg_delete), Toast.LENGTH_SHORT).show();
                refreshImages();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(MainActivity.this, getString(R.string.msg_delete_failure), Toast.LENGTH_SHORT).show();
            }
        });

    }
}
