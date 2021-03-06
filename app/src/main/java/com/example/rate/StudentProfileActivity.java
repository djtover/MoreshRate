package com.example.rate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.Constants;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class StudentProfileActivity extends AppCompatActivity {
    public static final int CAMERA_REQUEST = 1;
    Button btnCamera, btnLogout, btnBack, btnHome;
    FirebaseAuth firebaseAuth;
    CircularImageView civProfilePic;
    TextView tvName, tvRatedCourses;
    ListView lvStudentRatings;
    Photos newPhoto;
    DatabaseReference drPhotos, drStudents, drRatings, drCourses, drMain;
    ArrayAdapter<String> adapter;
    ArrayList<String> coursesRated;
    Query qUserRatings, qStudentName, qCourseID, qUserID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);
        //hides the action bar.
        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e) {
        }
        btnCamera = findViewById(R.id.addPhoto);
        btnHome = findViewById(R.id.studentProfileHome);
        drPhotos = FirebaseDatabase.getInstance().getReference("photos");
        drStudents = FirebaseDatabase.getInstance().getReference("students");
        drRatings = FirebaseDatabase.getInstance().getReference("ratings");
        drCourses = FirebaseDatabase.getInstance().getReference("courses");
        drMain = FirebaseDatabase.getInstance().getReference();
        btnBack = findViewById(R.id.studentProfileBack);
        btnLogout = findViewById(R.id.studentProfileLogout);
        firebaseAuth = firebaseAuth.getInstance();
        civProfilePic = findViewById(R.id.circularImageView);
        tvName = findViewById(R.id.studentName);
        tvRatedCourses = findViewById(R.id.tvRatedCourses);
        lvStudentRatings = findViewById(R.id.listViewStudent);
        coursesRated = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.courses_info, R.id.textView3, coursesRated);
        lvStudentRatings.setAdapter(adapter);

        //finds all the ratings of the current user and adds it to the listView.
        qUserRatings = drRatings.orderByChild("userID").equalTo(firebaseAuth.getCurrentUser().getUid());
        qUserRatings.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String courseID;
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                while (it.hasNext()) {
                    DataSnapshot node = it.next();
                    courseID = node.child("courseID").getValue().toString();

                    //finds the course of the specific rating.
                    qCourseID = drCourses.orderByKey().equalTo(courseID);
                    addRating(qCourseID, adapter, coursesRated);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //gets the name of the current user and displays it.
        qStudentName = drStudents.orderByKey().equalTo(firebaseAuth.getCurrentUser().getUid());
        getUserName(qStudentName, tvName);


        //gets the profile photo of the user if he has one.
        qUserID = drPhotos.orderByChild("userID").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        getProfileImage(qUserID, civProfilePic);

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(StudentProfileActivity.this, HomeActivity.class);
                startActivity(i);
                finishAffinity();
            }
        });

        //this listener opens the phone's camera.
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera(v);
            }
        });

        //this listener make sure that the user wants to logout and if he does it makes the logout.
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                try {
                                    firebaseAuth.getInstance().signOut();
                                    Intent i = new Intent(StudentProfileActivity.this, MainActivity.class);
                                    startActivity(i);
                                    finishAffinity();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("בטוח שברצונך להתנתק?").setPositiveButton("כן", dialogClickListener)
                        .setNegativeButton("לא", dialogClickListener).show();


            }
        });

        //back button.
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }

    /**
     * takes the string of the image from the database, and decodes it so it can be presented as a bitmap.
     */
    public static Bitmap decodeFromFirebaseBase64(String image) throws IOException {

        byte[] decodedByteArray = android.util.Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);

    }

    /**
     * this method opens up the phone's camera.
     * @param v
     */
    public void openCamera(View v) {
        Intent newIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(newIntent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {
                try {
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    civProfilePic.setImageBitmap(bitmap);
                    encodeBitmapAndSaveToFirebase(bitmap);

                } catch (NullPointerException e) {
                    e.printStackTrace();

                }
            }
        }
    }

    /**
     * this method encodes a bitmap image to a string and uploads it to the database.
     *
     * @param bitmap
     */
    private void encodeBitmapAndSaveToFirebase(Bitmap bitmap) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        final String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);


        Query qUserID = drPhotos.orderByChild("userID").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        qUserID.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                //if the user already has a photo.
                if (it.hasNext()) {
                    DataSnapshot node = it.next();
                    HashMap<String, Object> update = new HashMap<>();
                    update.put("imageData", imageEncoded);
                    drPhotos.child(node.getKey()).updateChildren(update);
                } else {
                    newPhoto = new Photos(FirebaseAuth.getInstance().getCurrentUser().getUid(), imageEncoded);
                    drPhotos.push().setValue(newPhoto);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * this method gets the user's profile picture, if he has one in the database.
     * @param q -
     * @param civ
     */
    private static void getProfileImage(Query q, final CircularImageView civ) {
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                //if the user already has a photo.
                if (it.hasNext()) {
                    DataSnapshot node = it.next();
                    String imageData = node.child("imageData").getValue().toString();
                    try {
                        Bitmap bitmap = decodeFromFirebaseBase64(imageData);
                        civ.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * this method gets the user name from the database.
     * @param q
     * @param tv
     */
    private static void getUserName(Query q, final TextView tv) {
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String getStudentName;
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                if (it.hasNext()) {
                    DataSnapshot node = it.next();
                    getStudentName = node.child("studentName").getValue().toString();
                    tv.setText(getStudentName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * this method adds the rating from the database to the arrayList of the listView, and updates the arrayAdapter of the listView.
     * @param q
     * @param aa
     * @param al
     */
    private static void addRating(Query q, final ArrayAdapter<String> aa, final ArrayList<String> al) {
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String courseName;
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                if (it.hasNext()) {
                    DataSnapshot node = it.next();
                    courseName = node.child("courseName").getValue().toString();
                    if (!al.contains(courseName)) {
                        al.add(0, courseName);
                        aa.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


}
