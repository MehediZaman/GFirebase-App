/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
	private DatabaseReference mDatabaseReference;
	private ChildEventListener mChildEventListener;
    private static final String TAG = "MainActivity";

	public static final int RC_SIGN_IN = 1;
	public static final int RC_PHOTO_PICKER = 10;

	public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

	private FirebaseAuth mFirebaseAuth;
	private FirebaseAuth.AuthStateListener mAuthStateListener;
	private FirebaseStorage mFirebaseStorage;
	private StorageReference mStorageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		mFirebaseDatabase = FirebaseDatabase.getInstance();
		mDatabaseReference = mFirebaseDatabase.getReference().child("messages");

		mFirebaseAuth = FirebaseAuth.getInstance();
		mFirebaseStorage = FirebaseStorage.getInstance();
		mStorageRef = mFirebaseStorage.getReference().child("Photos");
        mUsername = ANONYMOUS;

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/jpeg");
				intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
				startActivityForResult(Intent.createChooser(intent, "abcd"), RC_PHOTO_PICKER);
			}
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

				FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
				mDatabaseReference.push().setValue(friendlyMessage);
                // Clear input box
                mMessageEditText.setText("");
            }
        });

		mAuthStateListener = new FirebaseAuth.AuthStateListener() {
			@Override
			public void onAuthStateChanged (@NonNull final FirebaseAuth firebaseAuth) {
				FirebaseUser currentUser = firebaseAuth.getCurrentUser();
				if (currentUser != null){
					onUserSignedIn(currentUser.getDisplayName());
				}else{
					onUserSignedOut();
					startActivityForResult(
							AuthUI.getInstance()
									.createSignInIntentBuilder()
									.setProviders(AuthUI.EMAIL_PROVIDER, AuthUI.GOOGLE_PROVIDER)
									.build(),
							RC_SIGN_IN);
				}
			}
		};
    }



	private void onUserSignedIn (final String displayName) {
		mUsername = displayName;
		onAttachEventListener();
	}

	private void onUserSignedOut () {
		mUsername = "";
		mMessageAdapter.clear();
		onDetachEventListener();
	}



	private void onDetachEventListener () {
		if(mChildEventListener != null){
			mDatabaseReference.removeEventListener(mChildEventListener);
			mChildEventListener = null;
		}
	}



	private void onAttachEventListener () {
		if (mChildEventListener == null){
			mChildEventListener = new ChildEventListener() {
				@Override
				public void onChildAdded (final DataSnapshot dataSnapshot, final String s) {
					FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
					mMessageAdapter.add(friendlyMessage);
				}

				@Override
				public void onChildChanged (final DataSnapshot dataSnapshot, final String s) {

				}

				@Override
				public void onChildRemoved (final DataSnapshot dataSnapshot) {

				}

				@Override
				public void onChildMoved (final DataSnapshot dataSnapshot, final String s) {

				}

				@Override
				public void onCancelled (final DatabaseError databaseError) {

				}
			};
			mDatabaseReference.addChildEventListener(mChildEventListener);
		}
	}



	@Override
	protected void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode){
			case RC_SIGN_IN:
				switch (resultCode){
					case RESULT_OK:
						Toast.makeText(MainActivity.this, "Welcome to the Chat App!", Toast.LENGTH_LONG).show();
					case RESULT_CANCELED:
						Toast.makeText(MainActivity.this, "Sign-in Cancelled!", Toast.LENGTH_SHORT).show();
						finish();
				}
			case RC_PHOTO_PICKER:
				switch (resultCode){
					case RESULT_OK:
						Uri photoUri = data.getData();
						StorageReference sRef = mStorageRef.child(photoUri.getLastPathSegment());
						sRef.putFile(photoUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
							@Override
							public void onSuccess (final UploadTask.TaskSnapshot taskSnapshot) {
								Uri newPhotoUri = taskSnapshot.getDownloadUrl();
								FriendlyMessage message = new FriendlyMessage(null, mUsername, newPhotoUri.toString());
								mDatabaseReference.push().setValue(message);
							}
						});
				}
		}
	}



	@Override
	protected void onResume () {
		super.onResume();
		mFirebaseAuth.addAuthStateListener(mAuthStateListener);
	}



	@Override
	protected void onPause () {
		super.onPause();
		if (mAuthStateListener != null){
			mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
		}
		mMessageAdapter.clear();
		onDetachEventListener();

	}



	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case R.id.sign_out_menu:
				AuthUI.getInstance().signOut(this);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
    }

}
