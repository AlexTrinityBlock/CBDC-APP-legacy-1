package com.example.cbdc_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpEntity;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.CloseableHttpResponse;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpPost;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpUriRequest;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.CloseableHttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.DefaultHttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClientBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class MainActivity extends AppCompatActivity {

    public static String RSA_CONFIGURATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static String RSA_PROVIDER = "BC";
    public static String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    public static String PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";


    public String BankPublicKeyURL = "http://10.0.2.2:8080/public-key/user/withdraw";
    public String BankWithdrawUrl = "http://10.0.2.2:8080/get-currency";

    public String SendCurrencyToStoreURL = "http://10.0.2.2:7070/get-currency";
    public String StoreBinaryStringUrl = "http://10.0.2.2:7070/start-transaction/get-binary-string";
    public String StorePublicKeyStringUrl = "http://10.0.2.2:7070/store/public-key/";

    public String BankPublicKeyBase64 = "";
    public String BankPublicKeyJson = "";
    public byte[] bankPublicKeyBytes;

    public String StorePublicKeyBase64 = "";
    public String StorePublicKeyJson = "";
    public byte[] StorePublicKeyBytes;
    public String StoreBinaryString = "";

    public String CipherUserInfoB64 = "";
    public byte[] UserPublicKey;
    public byte[] UserPrivateKey;
    public String UserPublicKeyB64String;
    public String UserPrivateKeyB64String;
    public String UserPublicKeyExchangeString;
    public String UserPublicKeyExchangeStringBase64;

    public String StoreSessionKey;

    public String CurrencyCipherString;
    public String XORResultJsonString;
    public String OneEncryptCurrency;
    public ArrayList<String> currencyList = new ArrayList<String>();
    public ArrayList<String> bankSignatureList = new ArrayList<String>();
    public String UserUUID = "30a1bf87-b0e1-4921-a0b8-8c602af1f391";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void setUserRSAKeyPair() {
        //Step1 Gen RSA key
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            UserPublicKey = publicKey.getEncoded();
            UserPrivateKey = privateKey.getEncoded();
            UserPublicKeyB64String = new String(Base64.encode(UserPublicKey, Base64.DEFAULT), StandardCharsets.UTF_8);
            UserPrivateKeyB64String = new String(Base64.encode(UserPrivateKey, Base64.DEFAULT), StandardCharsets.UTF_8);
            UserPublicKeyExchangeString = PUBLIC_KEY_HEADER + "\n" + UserPublicKeyB64String + PUBLIC_KEY_FOOTER;
            UserPublicKeyExchangeStringBase64 = new String(Base64.encode(UserPublicKeyExchangeString.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT), StandardCharsets.UTF_8);
            Log.d("User Public key", UserPublicKeyB64String);
            Log.d("User Public key PEM", UserPublicKeyExchangeString);
            Log.d("User Private key", UserPrivateKeyB64String);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String XORTwoString(String str1, String str2) {
        byte[] bytes1 = str1.getBytes(StandardCharsets.UTF_8);
        byte[] bytes2 = str2.getBytes(StandardCharsets.UTF_8);
        byte[] bytes3 = new byte[36];
        ;
        for (int i = 0; i < 36; i++) {
            bytes3[i] = (byte) (bytes1[i] ^ bytes2[i]);
        }
        return new String(Base64.encode(bytes3, Base64.NO_WRAP), StandardCharsets.UTF_8);
    }


    public static String randomStringForHiddenInfo() {
        // create a string of all characters
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // create random string builder
        StringBuilder sb = new StringBuilder();

        // create an object of Random class
        Random random = new Random();

        // specify length of random string
        int length = 36;

        for (int i = 0; i < length; i++) {

            // generate random index number
            int index = random.nextInt(alphabet.length());

            // get character specified by index
            // from the string
            char randomChar = alphabet.charAt(index);

            // append the character to string builder
            sb.append(randomChar);
        }
        return sb.toString();
    }

    public static String RSAEncrypt(byte[] UTF8StringBytes, byte[] publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        //Prepare Key
        String publicKeyString = new String(publicKey, StandardCharsets.UTF_8);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        publicKeyString = publicKeyString.replace(PUBLIC_KEY_HEADER, "");
        publicKeyString = publicKeyString.replace(PUBLIC_KEY_FOOTER, "");
        Log.d("Public key", publicKeyString);
        PublicKey publicKeyObject = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeyString, Base64.NO_WRAP)));
        //Try to encrypt
        Cipher c = Cipher.getInstance(RSA_CONFIGURATION, RSA_PROVIDER);
        c.init(Cipher.ENCRYPT_MODE, publicKeyObject, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        byte[] encodedBytes = Base64.encode(c.doFinal(UTF8StringBytes), Base64.DEFAULT);
        String cipherText = new String(encodedBytes, "UTF-8");
        Log.d("Cipher Text", cipherText);
        return cipherText;
    }

    public void onWithdrawBtnClick(View view) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        String result = "";

        RequestQueue queue = Volley.newRequestQueue(this);


        //(End)Step1 Gen RSA key
        this.setUserRSAKeyPair();
        /* Step2 Get bank public key and encrypt user info */
        String publicKeyUrl = BankPublicKeyURL;
        StringRequest stringRequest1 = new StringRequest(Request.Method.GET, publicKeyUrl,
                new Response.Listener<String>() {

                    //RSA encryption
                    //https://stackoverflow.com/questions/50394730/pycrypto-rsa-pkcs1-oaep-sha256-interoperability-with-java
                    //https://vshivam.github.io/2015/06/09/compatible-rsa-encryption/
                    public String RSAEncrypt(byte[] UTF8StringBytes, byte[] publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
                        //Prepare Key
                        String publicKeyString = new String(publicKey, StandardCharsets.UTF_8);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        publicKeyString = publicKeyString.replace(PUBLIC_KEY_HEADER, "");
                        publicKeyString = publicKeyString.replace(PUBLIC_KEY_FOOTER, "");
                        Log.d("Public key", publicKeyString);
                        PublicKey publicKeyObject = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeyString, Base64.NO_WRAP)));
                        //Try to encrypt
                        Cipher c = Cipher.getInstance(RSA_CONFIGURATION, RSA_PROVIDER);
                        c.init(Cipher.ENCRYPT_MODE, publicKeyObject, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
                        byte[] encodedBytes = Base64.encode(c.doFinal(UTF8StringBytes), Base64.DEFAULT);
                        String cipherText = new String(encodedBytes, "UTF-8");
                        Log.d("Cipher Text", cipherText);
                        return cipherText;
                    }

                    @Override
                    public void onResponse(String response) {
                        //////////////////////////////////
                        //Send request
                        Log.d("sending", "sending");
                        BankPublicKeyJson = response;
                        try {
                            JSONObject jsonObject = new JSONObject(BankPublicKeyJson);
                            BankPublicKeyBase64 = jsonObject.getString("PublicKey");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.d("Finish", "Finish");

                        //Decode Public key
                        bankPublicKeyBytes = Base64.decode(BankPublicKeyBase64, Base64.DEFAULT);
                        String userInput = "{ \"user_name\":\"Alice\", \"user_password\":\"abc\", \"withdrawal_number\":1}";

                        //Encrypt User Info
                        try {
                            byte[] userInputBytesArray = userInput.getBytes("UTF-8");
                            CipherUserInfoB64 = this.RSAEncrypt(userInputBytesArray, bankPublicKeyBytes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //////////////////////////////////
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ImALabelForLog", "fail");
                Log.d("ImALabelForLog", error.toString());
            }
        });
        /*(End)Step2 Get bank public key and encrypt user info */


        /* Step3 Send encrypted user info to bank and get currency*/
        StringRequest stringRequest2 = new StringRequest(Request.Method.POST, BankWithdrawUrl, new com.android.volley.Response.Listener<String>() {

            //!!!!!!
            public String RSADecrypt(byte[] B64Bytes, byte[] privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
                Log.d("Try to Decrypt", "Try to Decrypt");
                Cipher c = Cipher.getInstance(RSA_CONFIGURATION, RSA_PROVIDER);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PrivateKey key = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKey));
                c.init(Cipher.DECRYPT_MODE, key, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
                byte[] decodedBytes = c.doFinal(B64Bytes);
                Log.d("Currency", new String(decodedBytes, "UTF-8"));
                return new String(decodedBytes, "UTF-8");
            }

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject respObj = new JSONObject(response);
                    Log.d("Cipher Currency and All responses", respObj.getString("cipher_currency"));
                    JSONArray resArr = respObj.getJSONArray("cipher_currency");
                    respObj = resArr.getJSONObject(0);
                    CurrencyCipherString = respObj.getString("Currency");
                    byte[] CurrencyCipherB64ByteArray = CurrencyCipherString.getBytes(StandardCharsets.UTF_8);
                    byte[] CurrencyCipherDecodeByte = Base64.decode(CurrencyCipherB64ByteArray, Base64.NO_WRAP);
                    Log.d("Cipher Currency", CurrencyCipherString);
                    String currency = this.RSADecrypt(CurrencyCipherDecodeByte, UserPrivateKey);
                    currencyList.add(currency);
                    bankSignatureList.add(respObj.getString("BankSignature"));
                    int numberOfCurrency = currencyList.size();
                    TextView currencyText = findViewById(R.id.currencyText);
                    currencyText.setText("餘額:" + String.valueOf(numberOfCurrency));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();

                params.put("cipher_user_input", CipherUserInfoB64);
                params.put("user_rsa_public_key", UserPublicKeyExchangeStringBase64);

                return params;
            }
        };
        /*(End)Step3 Send encrypted user info to bank and get currency*/
        queue.add(stringRequest1);
        queue.add(stringRequest2);
    }


    public void onBuyBtnClick(View view) throws IOException {
        //Get binary String
        //https://stackoverflow.com/questions/678630/how-do-i-make-an-http-request-using-cookies-on-android
        //https://stackoverflow.com/questions/15336477/deprecated-java-httpclient-how-hard-can-it-be
        //https://developer.android.com/reference/java/net/HttpURLConnection
        //https://blog.csdn.net/u013872857/article/details/71524502 //還沒讀
        //https://stackoverflow.com/questions/38591983/how-to-get-cookies-from-volley-response  //採用
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest1 = new StringRequest(Request.Method.GET, StoreBinaryStringUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        StoreBinaryString = response;
                        Log.i("StoreBinaryString", response);
                        //XOR two String
                        //Generate 10 random string
                        JSONArray XORedStringArray = new JSONArray();
                        Log.d("Test", MainActivity.randomStringForHiddenInfo());
                        for (int i = 0; i < 10; i++) {
                            String randomString = MainActivity.randomStringForHiddenInfo();
                            String XORedString = MainActivity.XORTwoString(UserUUID, MainActivity.randomStringForHiddenInfo());

                            if (StoreBinaryString.charAt(i) == '0') {
                                XORedStringArray.put(randomString);
                            } else {
                                XORedStringArray.put(MainActivity.XORTwoString(randomString, XORedString));
                            }
                        }
                        XORResultJsonString = XORedStringArray.toString();
                        Log.d("Json XOR", XORResultJsonString);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("error", error.getMessage());
                    }
                }) {
//            @Override
//            protected Response<String> parseNetworkResponse(NetworkResponse response) {
//                Map<String, String> responseHeaders = response.headers;
//                String rawCookies = responseHeaders.get("Set-Cookie");
//                StoreSessionKey = rawCookies;
//                StoreSessionKey = StoreSessionKey.replace("; HttpOnly; Path=/", "");
//                StoreSessionKey = StoreSessionKey.replace("\n", "");
//                Log.i("cookies", StoreSessionKey);
//
//                return super.parseNetworkResponse(response);
//            }

        };
        //(End)Get binary String

        //Get Store Public key
        StringRequest stringRequest2 = new StringRequest(Request.Method.GET, StorePublicKeyStringUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        JSONObject respObj = null;
                        try {
                            //Decode Store Public key
                            respObj = new JSONObject(response);
                            StorePublicKeyBase64 = respObj.getString("PublicKey");
                            StorePublicKeyBytes = Base64.decode(StorePublicKeyBase64, Base64.NO_WRAP);
                            StorePublicKeyBase64 = new String(StorePublicKeyBytes, "UTF-8");
                            StorePublicKeyBase64 = StorePublicKeyBase64.replace(PUBLIC_KEY_HEADER, "");
                            StorePublicKeyBase64 = StorePublicKeyBase64.replace(PUBLIC_KEY_FOOTER, "");
                            Log.d("Response", StorePublicKeyBase64);
//                            StorePublicKeyBytes = Base64.decode(StorePublicKeyBase64, Base64.NO_WRAP);

                            //Encrypt Currency
                            if (currencyList.size() > 0) {
                                JSONArray CipherCurrencyAndBankSignatureArray = new JSONArray();
                                String elementCurrency = currencyList.get(0);
                                String elementSignature = bankSignatureList.get(0);
                                Log.d("Currency", elementCurrency);
                                OneEncryptCurrency = MainActivity.RSAEncrypt(elementCurrency.getBytes(StandardCharsets.UTF_8),StorePublicKeyBytes);
                                JSONObject elementObject=new JSONObject();
                                elementObject.put("CipherCurrency",OneEncryptCurrency);
                                elementObject.put("BankSignature",elementSignature);
                                CipherCurrencyAndBankSignatureArray.put(elementObject);
                                OneEncryptCurrency =CipherCurrencyAndBankSignatureArray.toString();
                                Log.d("Json",OneEncryptCurrency);
                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("error", error.getMessage());
                    }
                }) {

        };
        //(End)Get Store Public key


        //Send Currency ,BankSignature and HiddenUserInfoList  To Store
        StringRequest stringRequest3 = new StringRequest(Request.Method.POST, SendCurrencyToStoreURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        try {
                            Log.d("Final Res",response);
                            JSONObject responseObj=new JSONObject(response);
                            if(responseObj.getString("Status").equals("Sucsess")){
                                String elementCurrency = currencyList.get(0);
                                String elementSignature = bankSignatureList.get(0);
                                currencyList.remove(elementCurrency);
                                bankSignatureList.remove(elementSignature);
                                TextView currencyText = findViewById(R.id.currencyText);
                                currencyText.setText("餘額:" + String.valueOf(currencyList.size()));

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ERROR", "error => " + error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<String, String>();
                params.put("CurrencyAndBankSignature", OneEncryptCurrency);
                params.put("HiddenUserInfoList", XORResultJsonString);
                return params;
            }
        };

        queue.add(stringRequest1);
        queue.add(stringRequest2);
        queue.add(stringRequest3);
    }
}

