package com.example.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DealActivity extends AppCompatActivity {

    private static final String LOG_TAG = DealActivity.class.getSimpleName();

    private static final int INSERT_PICTURE = 40;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference mDatabaseReference;

    private EditText txtTitle;
    private EditText txtPrice;
    private EditText txtDescription;
    private ImageView dealImage;
    private Uri imageUri;

    TravelDeal deal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);

        //FirebaseUtil.openFirebaseReference("traveldeals",);
        firebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;

        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        dealImage = findViewById(R.id.image);


        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("deal");
        if (deal == null) {
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());

        Button imageButton = findViewById(R.id.btnImage);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.action_select_picture)), INSERT_PICTURE);

            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.save_deal:
                saveDeal();
                Toast.makeText(this, "Deal saved", Toast.LENGTH_SHORT).show();
                clean();
                backToList();
                return true;

            case R.id.delete_deal:
                deleteDeal();
                Toast.makeText(this, "deal deleted", Toast.LENGTH_SHORT).show();
                backToList();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.save_menu, menu);

        if (FirebaseUtil.isAdmin == true) {
            menu.findItem(R.id.delete_deal).setVisible(true);
            menu.findItem(R.id.save_deal).setVisible(true);
            enableEditTexts(true);
        } else {
            menu.findItem(R.id.delete_deal).setVisible(false);
            menu.findItem(R.id.save_deal).setVisible(false);
            enableEditTexts(false);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != INSERT_PICTURE && resultCode != RESULT_OK ){
            Log.d("Image selection","Error getting image from gallery");
        }else {

            imageUri = data.getData();
            deal.setImageUrl(imageUri.toString());
            showImage(imageUri.toString());
            int takeFlags = data.getFlags();
            takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            final StorageReference reference = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            UploadTask uploadTask = reference.putFile(imageUri);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    String url = reference.getDownloadUrl().toString();
                    return reference.getDownloadUrl();

                }
            }).addOnSuccessListener(this, new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {


                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();

                    } else {
                        // Handle failures
                        // ...
                    }
                }
            });

        }


    }

    private void saveDeal() {
        deal.setTitle(txtTitle.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        deal.setDescription(txtDescription.getText().toString());

        //choose whether this is a new deal or an existing one
        if (deal.getId() == null) {
            //insert the new object to our database
            mDatabaseReference.push().setValue(deal);
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }

    }

    private void deleteDeal() {
        if (deal == null) {
            Toast.makeText(this, "Save deal first before deleting", Toast.LENGTH_SHORT).show();
        }
        mDatabaseReference.child(deal.getId()).removeValue();
    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }


    private void clean() {
        txtDescription.setText("");
        txtTitle.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();

    }

    private void enableEditTexts(boolean isEnabled) {
        txtTitle.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
    }

    private void showImage(String url){
        if (url != null && url.isEmpty() == false){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
                    .resize(width,width*2/3)
                    .centerCrop()
                    .into(dealImage);
        }
    }


}
