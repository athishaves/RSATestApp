package com.athish_works.rsatestapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {

    ListView keys, messages;
    private EditText message;

    ArrayList<String> publicKeysList;
    ArrayList<String> namesList;
    ArrayList<String> messagesList;

    int index;


    String publicKey, privateKey;
    byte[] encodeData;

    private final String KEYS = "Keys";
    private final String PUBLIC_SP = "Public Key";
    private final String PRIVATE_SP = "Private Key";

    SharedPreferences keysSp;


    DatabaseReference databaseReference, databaseReferenceMessage;

    String myName;


    private void declareVariables() {
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

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        declareVariables();

        Log.i("Keys", "Here");

        keysSp = getSharedPreferences(KEYS, MODE_PRIVATE);

        String pubKey = keysSp.getString(PUBLIC_SP, "");
        String priKey = keysSp.getString(PRIVATE_SP, "");

        Log.i("Keys", "Here");

        if (pubKey.equals("") || priKey.equals("")) {
            askName();

        } else {
            publicKey = pubKey;
            Log.i("Keys", "Public Key = " + publicKey);
            privateKey = priKey;
            Log.i("Keys", "Private Key = " + privateKey);
        }

        forKeyListView();
        forMessagesListView();

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
                    Log.i("Keys", "Keys DataSnapshot Child " + ds);
                    publicKeysList.add(ds.child("key").getValue().toString());
                    namesList.add(ds.child("name").getValue().toString());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, namesList);
                keys.setAdapter(adapter);
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
                    Log.i("Encrypt", "Encrypt Size = " + encryptedMessage.length());
                    messagesList.add(decryptMessage(new BigInteger(encryptedMessage)));
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, messagesList);
                messages.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void callAToast(String a) {
        Toast.makeText(getApplicationContext(), a, Toast.LENGTH_SHORT).show();
    }

    private void askName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View customLayout = getLayoutInflater().inflate(R.layout.edit_text_alert_dialog, null);

        builder
                .setView(customLayout)
                .setTitle("")
                .setMessage("Enter your name")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callAToast("You should have had entered your name");
                        finish();
                    }
                })
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callAToast("Thanks for the submission");
                        EditText editText = customLayout.findViewById(R.id.alert_edit_text);
                        myName = editText.getText().toString();
                        if (myName.equals("")) {
                            callAToast("Please enter the name");
                            finish();
                        } else {
                            firstTime();
                        }

                    }
                })
        .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
        Log.i("Keys", "Cleared here");
    }

    private void firstTime() {
        Log.i("Keys", "First Time");
        try {
            Map<String, Object> keyMap = RSAAlgorithm.initKey();
            SharedPreferences.Editor editor = keysSp.edit();

            publicKey = RSAAlgorithm.getPublicKey(keyMap);
            editor.putString(PUBLIC_SP, publicKey);

            Log.i("Keys", "Here first time public key");

            privateKey = RSAAlgorithm.getPrivateKey(keyMap);
            editor.putString(PRIVATE_SP, privateKey);

            Log.i("Keys", "Here first time private key");

            editor.apply();

            String id = databaseReference.push().getKey();
            databaseReference.child(id).child("key").setValue(publicKey);
            databaseReference.child(id).child("name").setValue(myName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String decryptMessage(BigInteger gotMessage) {
        String privateKey = getPrivateKey();

        try {
            byte[] decodeData = RSAAlgorithm.encryptByPrivateKey(gotMessage.toByteArray(), privateKey);
            String data = new String(decodeData);
            Log.i("Keys", data);
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
            databaseReferenceMessage.child(id).child("key").setValue(pKey);

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


}
