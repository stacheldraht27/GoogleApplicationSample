package com.azura.googleapplicationsample;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusShare;
import com.google.android.gms.plus.model.people.Person;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity implements View.OnClickListener, ConnectionCallbacks, OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private static final String WEB_CLIENT_ID = "WEB_CLIENT_ID";
    private static final String SERVER_BASE_URL = "SERVER_BASE_URL";
    private static final String SELECT_SCOPES_URL = SERVER_BASE_URL + "/selectscopes";
    private static final String EXCHANGE_TOKEN_URL = SERVER_BASE_URL + "/exchangetoken";

    private boolean mRequestServerAuthCode = false;
    private boolean mServerHasToken = true;
    private ProgressDialog mConnectionProgressDialog;
    private GoogleApiClient mGoogleApiClient;
    private ConnectionResult mConnectionResult;

    Button ShareButton,GetData,ok_btn,LogOutBtn,SignInBtn;
    Dialog get_data_dialog;
    TextView name,url,id;
    ImageView user_image;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{


        super.onCreate(savedInstanceState);

        mGoogleApiClient = buildGoogleApiClient();
        setContentView(R.layout.activity_main);

        mConnectionProgressDialog = new ProgressDialog(this);
        mConnectionProgressDialog.setMessage("Sign in...");

        SignInBtn = (Button) findViewById(R.id.sign_in_button);
        ShareButton = (Button) findViewById(R.id.post_button);
        GetData = (Button)findViewById(R.id.get_data_button);
        LogOutBtn = (Button) findViewById(R.id.log_out_button);

        SignInBtn.setOnClickListener(this);
        GetData.setOnClickListener(this);
        ShareButton.setOnClickListener(this);
        LogOutBtn.setOnClickListener(this);
        }catch (Exception e){
            Log.e("ERROR NYA ADALAH : ",e.toString());
        }
    }

    private GoogleApiClient buildGoogleApiClient() {
        try{
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN);
        if (mRequestServerAuthCode) {
            checkServerAuthConfiguration();
            builder = builder.requestServerAuthCode(WEB_CLIENT_ID, (GoogleApiClient.ServerAuthCodeCallbacks) this);

        }
        return builder.build();
        }catch(Exception e){
            Log.e("ERROR GOOGLE API",e.toString());
        }
        return null;
    }

    private void checkServerAuthConfiguration() {
        // Check that the server URL is configured before allowing this box to
        // be unchecked
        if ("WEB_CLIENT_ID".equals(WEB_CLIENT_ID) ||
                "SERVER_BASE_URL".equals(SERVER_BASE_URL)) {
            Log.w(TAG, "WEB_CLIENT_ID or SERVER_BASE_URL configured incorrectly.");
            Dialog dialog = new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.configuration_error))
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();

            dialog.show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        String accountName = currentPerson.getDisplayName();
        Toast.makeText(this, accountName + " is connected.", Toast.LENGTH_LONG).show();
        SignInBtn.setEnabled(false);
        ShareButton.setEnabled(true);
        GetData.setEnabled(true);
        LogOutBtn.setEnabled(true);
    }

    public void onDisconect(){
        SignInBtn.setEnabled(true);
        ShareButton.setEnabled(false);
        GetData.setEnabled(false);
        LogOutBtn.setEnabled(false);
        Toast.makeText(this, " now disconnected.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button:
                if (!mGoogleApiClient.isConnected()) {
                    if (mConnectionResult == null) {
                        mConnectionProgressDialog.show();
                    } else {
                        try {
                            mConnectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
                        } catch (SendIntentException e) {
                            // Try connecting again.
                            mConnectionResult = null;
                            mGoogleApiClient.connect();
                        }
                    }
                }
                break;
            case R.id.get_data_button:
                String disp_name, disp_url, disp_id;
                if (mGoogleApiClient.isConnected()) {
                    get_data_dialog = new Dialog(MainActivity.this);
                    get_data_dialog.setContentView(R.layout.fragment_get_data);
                    get_data_dialog.setTitle("User Details");
                    name = (TextView) get_data_dialog.findViewById(R.id.get_name);
                    url = (TextView) get_data_dialog.findViewById(R.id.get_url);
                    id = (TextView) get_data_dialog.findViewById(R.id.get_id);
                    user_image = (ImageView) get_data_dialog.findViewById(R.id.imgUser);
                    ok_btn = (Button) get_data_dialog.findViewById(R.id.ok_button);
                    ok_btn.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            get_data_dialog.cancel();
                        }
                    });
                    Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                    disp_name = currentPerson.getDisplayName();
                    disp_url = currentPerson.getUrl();
                    disp_id = currentPerson.getId();
                    name.setText(disp_name);
                    url.setText(disp_url);
                    id.setText(disp_id);
                    user_image.setImageDrawable(Drawable.createFromPath(currentPerson.getImage().getUrl()));
                    get_data_dialog.show();
                } else {
                    Toast.makeText(getApplicationContext(), "Please Sign In", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.post_button:
                // Launch the Google+ share dialog with attribution to your app.
                Intent shareIntent = new PlusShare.Builder(MainActivity.this)
                        .setType("text/plain")
                        .setText("Hi!, We are 7Langit")
                        .setContentUrl(Uri.parse("http://www.7langit.com"))
                        .getIntent();
                startActivityForResult(shareIntent, 0);
                break;
            case R.id.log_out_button:
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                    onDisconect();
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if(mConnectionProgressDialog.isShowing()){
            // The user clicked the sign-in button already. Start to resolve
            // connection errors. Wait until onConnected() to dismiss the
            // connection dialog.
            if (result.hasResolution()) {
                try {
                    result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
                } catch (SendIntentException e) {
                    mGoogleApiClient.connect();
                }
            }
            // Save the result and resolve the connection failure upon a user click.
            mConnectionResult = result;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        super.onActivityResult(requestCode, responseCode, data);
        if (requestCode == REQUEST_CODE_RESOLVE_ERR && responseCode == RESULT_OK) {
            mConnectionResult = null;
            mGoogleApiClient.connect();
        }
    }

//    @Override
//    public CheckResult onCheckServerAuthorization(String idToken, Set<Scope> scopeSet) {
//        Log.i(TAG, "Checking if server is authorized.");
//        Log.i(TAG, "Mocking server has refresh token: " + String.valueOf(mServerHasToken));
//
//        if (!mServerHasToken) {
//            // Server does not have a valid refresh token, so request a new
//            // auth code which can be exchanged for one.  This will cause the user to see the
//            // consent dialog and be prompted to grant offline access. This callback occurs on a
//            // background thread so it is OK to do synchronous network access.
//
//            // Ask the server which scopes it would like to have for offline access.  This
//            // can be distinct from the scopes granted to the client.  By getting these values
//            // from the server, you can change your server's permissions without needing to
//            // recompile the client application.
//            HttpClient httpClient = new DefaultHttpClient();
//            HttpGet httpGet = new HttpGet(SELECT_SCOPES_URL);
//            HashSet<Scope> serverScopeSet = new HashSet<Scope>();
//
//            try {
//                HttpResponse httpResponse = httpClient.execute(httpGet);
//                int responseCode = httpResponse.getStatusLine().getStatusCode();
//                String responseBody = EntityUtils.toString(httpResponse.getEntity());
//
//                if (responseCode == 200) {
//                    String[] scopeStrings = responseBody.split(" ");
//                    for (String scope : scopeStrings) {
//                        Log.i(TAG, "Server Scope: " + scope);
//                        serverScopeSet.add(new Scope(scope));
//                    }
//                } else {
//                    Log.e(TAG, "Error in getting server scopes: " + responseCode);
//                }
//
//            } catch (ClientProtocolException e) {
//                Log.e(TAG, "Error in getting server scopes.", e);
//            } catch (IOException e) {
//                Log.e(TAG, "Error in getting server scopes.", e);
//            }
//
//            // This tells GoogleApiClient that the server needs a new serverAuthCode with
//            // access to the scopes in serverScopeSet.  Note that we are not asking the server
//            // if it already has such a token because this is a sample application.  In reality,
//            // you should only do this on the first user sign-in or if the server loses or deletes
//            // the refresh token.
//            return CheckResult.newAuthRequiredResult(serverScopeSet);
//        } else {
//            // Server already has a valid refresh token with the correct scopes, no need to
//            // ask the user for offline access again.
//            return CheckResult.newAuthNotRequiredResult();
//        }
//    }
//
//    @Override
//    public boolean onUploadServerAuthCode(String idToken, String serverAuthCode) {
//        // Upload the serverAuthCode to the server, which will attempt to exchange it for
//        // a refresh token.  This callback occurs on a background thread, so it is OK
//        // to perform synchronous network access.  Returning 'false' will fail the
//        // GoogleApiClient.connect() call so if you would like the client to ignore
//        // server failures, always return true.
//        HttpClient httpClient = new DefaultHttpClient();
//        HttpPost httpPost = new HttpPost(EXCHANGE_TOKEN_URL);
//
//        try {
//            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
//            nameValuePairs.add(new BasicNameValuePair("serverAuthCode", serverAuthCode));
//            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//
//            HttpResponse response = httpClient.execute(httpPost);
//            int statusCode = response.getStatusLine().getStatusCode();
//            final String responseBody = EntityUtils.toString(response.getEntity());
//            Log.i(TAG, "Code: " + statusCode);
//            Log.i(TAG, "Resp: " + responseBody);
//
//            // Show Toast on UI Thread
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(MainActivity.this, responseBody, Toast.LENGTH_LONG).show();
//                }
//            });
//            return (statusCode == 200);
//        } catch (ClientProtocolException e) {
//            Log.e(TAG, "Error in auth code exchange.", e);
//            return false;
//        } catch (IOException e) {
//            Log.e(TAG, "Error in auth code exchange.", e);
//            return false;
//        }
//    }
}
