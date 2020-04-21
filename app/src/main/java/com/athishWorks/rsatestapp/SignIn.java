package com.athishWorks.rsatestapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SignIn extends AppCompatActivity {

    EditText emailET, passwordET;

    FirebaseAuth mAuth;

    private final String KEYS = "Keys";
    private final String PUBLIC_SP = "Public Key";
    private final String PRIVATE_SP = "Private Key";

    private void declarations() {

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser()!=null) {
            startActivity(new Intent(SignIn.this, MainActivity.class));
            callAToast("You are already logged in");
            finish();
        }

        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        declarations();
    }

    public void openSignUp(View view) {
        startActivity(new Intent(SignIn.this, SignUp.class));
    }

    public void signIn(View view) {

        if (mAuth.getCurrentUser()!=null) {
            mAuth.signOut();
        }

        final String gmail = emailET.getText().toString();
        final String password = passwordET.getText().toString();
        if (gmail.equals("") || password.equals("")) {
            callAToast("Fill the details");

        } else {
            mAuth.signInWithEmailAndPassword(gmail, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                int i = gmail.lastIndexOf('@');
                                String username = gmail.substring(0, i);

                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Keys/"+username);
                                reference.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        SharedPreferences keysSp = getSharedPreferences(KEYS, MODE_PRIVATE);
                                        SharedPreferences.Editor editor = keysSp.edit();

                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                            Log.i("Keysar", "DataSnapshots " + ds);
                                            switch (ds.getKey()) {
                                                case "pubKey":
                                                    editor.putString(PUBLIC_SP, ds.getValue().toString());
                                                    Log.i("Keysar", "Public key " + ds.getValue());
                                                    break;
                                                case "priKey":
                                                    editor.putString(PRIVATE_SP, ds.getValue().toString());
                                                    Log.i("Keysar", "Private key " + ds.getValue());
                                                    break;
                                            }
                                        };

                                        if (editor.commit()) {
                                            callAToast("User login successful");
                                            callAToast("Welcome " + mAuth.getCurrentUser().getDisplayName());
                                            startActivity(new Intent(SignIn.this, MainActivity.class));
                                            finish();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                            } else {
                                callAToast(task.getException().getMessage());
                            }
                        }
                    });
        }
    }

    private void callAToast(String a) {
        Toast.makeText(this, a, Toast.LENGTH_SHORT).show();
    }

}