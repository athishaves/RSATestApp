package com.athishWorks.rsatestapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    ListView keys, messages;
    private EditText message;

    ArrayList<String> publicKeysList;
    ArrayList<String> namesList;
    ArrayList<String> messagesList;

    ArrayAdapter<String> keyAdapter, messageAdapter;

    int index;


    String publicKey, privateKey;
    byte[] encodeData;

    private final String KEYS = "Keys";
    private final String PUBLIC_SP = "Public Key";
    private final String PRIVATE_SP = "Private Key";

    SharedPreferences keysSp;


    DatabaseReference databaseReference, databaseReferenceMessage;

    private String AES = "AES";

    public native String helloWorld();

    static {
        System.loadLibrary("ndktest");
    }

    private void declareVariables() {

        Log.i("Success", "Main Activity " + helloWorld() + " " + helloWorld().getBytes().length);

        keys = findViewById(R.id.keys);
        messages = findViewById(R.id.messages);
        message = findViewById(R.id.text);

        encodeData = null;

        databaseReference = FirebaseDatabase.getInstance().getReference("Keys");
        databaseReferenceMessage = FirebaseDatabase.getInstance().getReference("Messages");

        publicKeysList = new ArrayList<>();
        namesList = new ArrayList<>();
        messagesList = new ArrayList<>();
        index = -1;

        keyAdapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_1, namesList);
        keys.setAdapter(keyAdapter);

        messageAdapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_1, messagesList);
        messages.setAdapter(messageAdapter);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        declareVariables();

        Log.i("Keys", "Here");

        keysSp = getSharedPreferences(KEYS, MODE_PRIVATE);

        String pubKey = keysSp.getString(PUBLIC_SP, "");
        String priKey = keysSp.getString(PRIVATE_SP, "");

        Log.i("Keys", "Here");

        publicKey = pubKey;
        Log.i("DSA", "Public Key = " + publicKey);
        privateKey = priKey;
        Log.i("DSA", "Encoded Private Key = " + privateKey);
        try {
            Log.i("DSA", "Decoded Private Key = " + decrypt(privateKey, helloWorld()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        forKeyListView();
        forMessagesListView();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        switch (item.getItemId()) {
            case R.id.log_out_menu:
                if (mAuth.getCurrentUser()!=null) {
                    mAuth.signOut();
                }
                SharedPreferences keysSp = getSharedPreferences(KEYS, MODE_PRIVATE);
                SharedPreferences.Editor editor = keysSp.edit();
                editor.clear();
                if (editor.commit()) {
                    startActivity(new Intent(MainActivity.this, SignUp.class));
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void forKeyListView() {

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (publicKeysList.size()!=0) {
                    publicKeysList.clear();
                }
                if (namesList.size()!=0) {
                    namesList.clear();
                }

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    try {
                        publicKeysList.add(ds.child("pubKey").getValue().toString());
                        namesList.add(ds.child("name").getValue().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                keyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        keys.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                index = position;
            }
        });
    }

    private void forMessagesListView() {

        databaseReferenceMessage.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (messagesList.size()!=0) {
                    messagesList.clear();
                }

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String encryptedMessage = ds.child("message").getValue().toString();
                    messagesList.add(decryptMessage(new BigInteger(encryptedMessage)));
                }
                messageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void callAToast(String a) {
        Toast.makeText(getApplicationContext(), a, Toast.LENGTH_SHORT).show();
    }

    public String decryptMessage(BigInteger gotMessage) {
        String privateKey = getPrivateKey();

        try {
            String password = decrypt(privateKey, helloWorld());
            byte[] decodeData = RSAAlgorithm.encryptByPrivateKey(gotMessage.toByteArray(), password);
            String data = new String(decodeData);
            Log.i("Keys", "Length = " + data.length());
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Keys", "Error decrypting cauze " + e.getMessage());
        }
        return "null";
    }

    public void sendButton(View view) {
        if (index==-1) {
            callAToast("Please select a recipient");
            return;
        }
        String pKey = publicKeysList.get(index);
        Log.i("Keys", "Public key of recipient " + pKey);
        byte[] rsaData = message.getText().toString().getBytes();

        try {
            encodeData = RSAAlgorithm.encryptByPublicKey(rsaData, pKey);
            String data = new BigInteger(1, encodeData).toString();
            Log.i("Keys", "Enc message" + data);

            String id = databaseReferenceMessage.push().getKey();
            databaseReferenceMessage.child(id).child("message").setValue(data);

            Log.i("Keys", "Firebase message updation successful");

        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Keys", e.getMessage());
            callAToast("Error");
        }
    }

    private String getPrivateKey() {
        return privateKey;
    }




    private String decrypt(String data, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decVal = Base64.decode(data, Base64.DEFAULT);
        byte[] decValue = c.doFinal(decVal);
        return new String(decValue);
    }

    private SecretKeySpec generateKey(String password) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        digest.update(bytes, 0, bytes.length);
        byte[] key = digest.digest();
        return new SecretKeySpec(key, AES);
    }

}
