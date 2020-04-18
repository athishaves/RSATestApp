package com.athish_works.rsatestapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

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
import java.util.Arrays;
import java.util.Map;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {

    ListView keys, messages;
    private EditText message;
    EditText encDecMessage;


    String publicKey, privateKey;
    byte[] encodeData;


    private final String KEYS = "Keys";
    private final String PUBLIC_SP = "Public Key";
    private final String PRIVATE_SP = "Private Key";

    SharedPreferences keysSp;


    private void declareVariables() {
        keys = findViewById(R.id.keys);
        messages = findViewById(R.id.messages);
        message = findViewById(R.id.text);

        // TODO : Delete this
        encDecMessage = findViewById(R.id.encDecText);

        encodeData = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        declareVariables();

        keysSp = getSharedPreferences(KEYS, MODE_PRIVATE);

        String pubKey = keysSp.getString(PUBLIC_SP, "");
        String priKey = keysSp.getString(PRIVATE_SP, "");

        if (pubKey.equals("") || priKey.equals("")) {
            firstTime();
        } else {
            publicKey = pubKey;
            Log.i("Keys", "Public Key = " + publicKey);
            privateKey = priKey;
            Log.i("Keys", "Private Key = " + privateKey);
        }

    }

    private void firstTime() {
        Log.i("Keys", "First Time");
        try {
            Map<String, Object> keyMap = RSAAlgorithm.initKey();
            SharedPreferences.Editor editor = keysSp.edit();

            publicKey = RSAAlgorithm.getPublicKey(keyMap);
            Log.i("Keys", "Public Key = " + publicKey);
            editor.putString(PUBLIC_SP, publicKey);

            privateKey = RSAAlgorithm.getPrivateKey(keyMap);
            Log.i("Keys", "Private Key = " + privateKey);
            editor.putString(PRIVATE_SP, privateKey);

            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshButton(View view) {
        String privateKey = getPrivateKey();

        try {
            byte[] decodeData = RSAAlgorithm.encryptByPrivateKey(encodeData, privateKey);
            String data = new String(decodeData);
            encDecMessage.setText(data);
            Log.i("Keys", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendButton(View view) {
        String publicKey = getPublicKey();
        byte[] rsaData = message.getText().toString().getBytes();

        try {
            encodeData = RSAAlgorithm.encryptByPublicKey(rsaData, publicKey);
            String data = new BigInteger(1, encodeData).toString();
            encDecMessage.setText(data);
            Log.i("Keys", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPrivateKey() {
        return privateKey;
    }

    private String getPublicKey() {
        return publicKey;
    }


}
