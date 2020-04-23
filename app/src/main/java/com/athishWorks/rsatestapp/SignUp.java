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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SignUp extends AppCompatActivity {

    EditText nameET, emailET, passwordET;

    FirebaseAuth mAuth;

    private String AES = "AES";

    private final String KEYS = "Keys";
    private final String PUBLIC_SP = "Public Key";
    private final String PRIVATE_SP = "Private Key";


    public native String helloWorld();

    static {
        System.loadLibrary("ndktest");
    }


    private void declarations() {

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser()!=null) {
            startActivity(new Intent(SignUp.this, MainActivity.class));
            callAToast("You are already logged in");
            finish();
        }

        nameET = findViewById(R.id.username);
        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        declarations();
    }

    public void openSignIn(View view) {
        startActivity(new Intent(SignUp.this, SignIn.class));
    }

    public void signUp(View view) {

        if (mAuth.getCurrentUser()!=null) {
            mAuth.signOut();
        }

        final String name = nameET.getText().toString();
        final String gmail = emailET.getText().toString();
        final String password = passwordET.getText().toString();

        if (name.equals("") || gmail.equals("") || password.equals("")) {
            callAToast("Fill the details");

        } else {
            mAuth.createUserWithEmailAndPassword(gmail, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                Map<String, Object> keyMap;
                                try {

                                    SharedPreferences keysSp = getSharedPreferences(KEYS, MODE_PRIVATE);

                                    SharedPreferences.Editor editor = keysSp.edit();

                                    keyMap = RSAAlgorithm.initKey();

                                    String publicKey = RSAAlgorithm.getPublicKey(keyMap);
                                    editor.putString(PUBLIC_SP, publicKey);
                                    Log.i("Keys", "Private key " + publicKey);

                                    String privateKey = RSAAlgorithm.getPrivateKey(keyMap);

                                    Log.i("DSA", "Private key " + privateKey);
                                    privateKey = encrypt(privateKey, "helloWorld");
                                    Log.i("DSA", "Encoded Private key " + privateKey);
                                    Log.i("DSA", "Decoded Private key " + decrypt(privateKey, "helloWorld"));

                                    editor.putString(PRIVATE_SP, privateKey);

                                    if (editor.commit()) {
                                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Keys");
                                        int i = gmail.lastIndexOf('@');
                                        String username = gmail.substring(0, i);
                                        reference.child(username).child("name").setValue(name);
                                        reference.child(username).child("pubKey").setValue(publicKey);
                                        reference.child(username).child("priKey").setValue(privateKey);

                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(name)
                                                .build();
                                        user.updateProfile(profile)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        callAToast("User registered successfully");
                                                        callAToast("Welcome " + name);
                                                        startActivity(new Intent(SignUp.this, MainActivity.class));
                                                        finish();
                                                    }
                                                });
                                    }

                                } catch (Exception e) {
                                    callAToast("Private Public keys aren't updated");
                                    e.printStackTrace();
                                }

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





    private String decrypt(String data, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decVal = Base64.decode(data, Base64.DEFAULT);
        byte[] decValue = c.doFinal(decVal);
        return new String(decValue);
    }

    private String encrypt(String data, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(data.getBytes());
        return Base64.encodeToString(encVal, Base64.DEFAULT);
    }

    private SecretKeySpec generateKey(String password) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        digest.update(bytes, 0, bytes.length);
        byte[] key = digest.digest();
        return new SecretKeySpec(key, AES);
    }

}
