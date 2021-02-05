package be.birbbro.java;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import be.birbbro.R;

public class PageFragment extends Fragment {

    private Double timestamp;
    private Bitmap bitmap;

    public static PageFragment getInstance(double timestamp) {
        PageFragment f = new PageFragment();
        Bundle args = new Bundle();
        args.putDouble("image_source", timestamp);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timestamp = getArguments().getDouble("image_source");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_page, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imagerRef = storage.getReference().child("files/" + String.format("%.0f", timestamp) + ".jpg");
        Glide.with(this /* context */)
                .load(imagerRef)
                .into(imageView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
