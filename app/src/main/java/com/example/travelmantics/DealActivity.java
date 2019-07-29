package com.example.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;

public class DealActivity extends AppCompatActivity {

    public static final int INSERT_PICTURE = 40;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference mDatabaseReference;

    EditText txtTitle;
    EditText txtPrice;
    EditText txtDescription;

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

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("deal");
        if (deal == null){
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());

        Button imageButton = findViewById(R.id.btnImage);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =  new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
                startActivityForResult(intent.createChooser(intent,"Insert Picture"
                ), INSERT_PICTURE);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.save_deal:
                saveDeal();
                Toast.makeText(this,"Deal saved",Toast.LENGTH_SHORT).show();
                clean();
                backToList();
                return true;

            case R.id.delete_deal:
                deleteDeal();
                Toast.makeText(this,"deal deleted",Toast.LENGTH_SHORT).show();
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

        if (FirebaseUtil.isAdmin == true){
            menu.findItem(R.id.delete_deal).setVisible(true);
            menu.findItem(R.id.save_deal).setVisible(true);
            enableEditTexts(true);
        }else {
            menu.findItem(R.id.delete_deal).setVisible(false);
            menu.findItem(R.id.save_deal).setVisible(false);
            enableEditTexts(false);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSERT_PICTURE && requestCode == RESULT_OK){
            Uri imageUri = data.getData();
            StorageReference reference = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            reference.putFile(imageUri);
            Log.d("Upload","Upload successful");
        }
    }

    private void saveDeal() {
        deal.setTitle(txtTitle.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        deal.setDescription(txtDescription.getText().toString());

        //choose whether this is a new deal or an existing one
        if (deal.getId() == null){
            //insert the new object to our database
            mDatabaseReference.push().setValue(deal);
        }else{
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }

    }
    private void deleteDeal(){
        if (deal == null){
            Toast.makeText(this,"Save deal first before deleting",Toast.LENGTH_SHORT).show();
        }
        mDatabaseReference.child(deal.getId()).removeValue();
    }
    private void backToList(){
        Intent intent = new Intent(this,ListActivity.class);
        startActivity(intent);
    }



    private void clean() {
        txtDescription.setText("");
        txtTitle.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();

    }

    private void enableEditTexts(boolean isEnabled){
        txtTitle.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
    }


}
