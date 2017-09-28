package com.bikeguard.mobile.bikeguard;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.zendesk.logger.Logger;
import com.zendesk.sdk.model.access.AnonymousIdentity;
import com.zendesk.sdk.model.request.CreateRequest;
import com.zendesk.sdk.network.RequestProvider;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.sdk.requests.RequestActivity;
import com.zendesk.sdk.support.ContactUsButtonVisibility;
import com.zendesk.sdk.support.SupportActivity;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;
import com.zopim.android.sdk.api.ZopimChat;
import com.zopim.android.sdk.model.VisitorInfo;
import com.zopim.android.sdk.prechat.ZopimChatActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String SUPPORT_NUMBER = "tel:+353768888870";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialiseView();

        initializeChatButton();

        initializeCallButton();

        initializeHelpCentreButton();

        initializeShowRequestsButton();

    }

    private void initialiseView() {

        Fabric.with(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Logger.setLoggable(BuildConfig.DEBUG);

        initializeZendesk();

        ZopimChat.init("tBHcpfcP5vOr2XVkANbBjDPWOIcNlUYw");
    }


    private void initializeShowRequestsButton() {
        findViewById(R.id.show_open_requests_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestActivity.startActivity(MainActivity.this, null);
            }
        });
    }

    private void initializeHelpCentreButton() {
        findViewById(R.id.launch_help_center_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new SupportActivity.Builder()
                        .withArticleVoting(false)
                        .withContactUsButtonVisibility(ContactUsButtonVisibility.OFF)
                        .show(MainActivity.this);
            }
        });
    }

    private void initializeCallButton() {
        findViewById(R.id.call_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse(SUPPORT_NUMBER));

                if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CALL_PHONE)) {
                    return;
                }

                createTicketOnCallInitiated();
                startActivity(callIntent);
            }
        });
    }

    private void initializeChatButton() {
        Button mChatButton;
        mChatButton = (Button) findViewById(R.id.chat_button);

        mChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = getGoogleEmail();

                if (email == null) {
                    email = "unknown";
                }

                // add some visitor info
                VisitorInfo visitorInfo = new VisitorInfo.Builder()
                        .name("User name")
                        .email(email)
                        .phoneNumber("Number")
                        .build();

                // set visitor info
                ZopimChat.setVisitorInfo(visitorInfo);

                //initiate the chat activity
                startActivity(new Intent(getApplicationContext(), ZopimChatActivity.class));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeZendesk() {
        // Initialize the Support SDK with your Zendesk Support subdomain, mobile SDK app ID, and client ID.
        // Get these details from your Zendesk Support dashboard: Admin -> Channels -> Mobile SDK
        ZendeskConfig.INSTANCE.init(getApplicationContext(),
                getString(R.string.com_zendesk_sdk_url),
                getString(R.string.com_zendesk_sdk_identifier),
                getString(R.string.com_zendesk_sdk_clientIdentifier));

        //Some logic which will autofill the Google email of the user if known
        String email = getGoogleEmail();

        if (email == null) {
            email = "unknown";
        }

        // Authenticate anonymously as a Zendesk Support user
        ZendeskConfig.INSTANCE.setIdentity(
                new AnonymousIdentity.Builder()
                        .withNameIdentifier("Zen Desk user")
                        .withEmailIdentifier(email)
                        .build()
        );
    }



    private String getGoogleEmail() {
        //this is somewhat hacky as we have no guarantee the user has a google account
        //but it demonstrates some use of the SDKs
        AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<>();

        for (Account account : accounts) {
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            return possibleEmails.get(0);
        }
        return null;
    }

    private void createTicketOnCallInitiated() {
        //this logic creates a support ticket the moment a call is made to us
        //especially useful if the user is using a device which has no telephony

        CreateRequest createRequest = new CreateRequest();

        createRequest.setSubject("User support call");
        createRequest.setDescription("User has initiated a theft conversation with us. High Priority.");

        //add the current time and android version as examples of metadata
        addRequestMetadata(createRequest);

        RequestProvider requestProvider = ZendeskConfig.INSTANCE.provider().requestProvider();

        requestProvider.createRequest(createRequest, new ZendeskCallback<CreateRequest>() {
            @Override
            public void onSuccess(CreateRequest createRequest) {
                Logger.i(TAG, "Request created...");
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                Logger.e(TAG, errorResponse);
            }
        });

    }

    private void addRequestMetadata(CreateRequest createRequest) {

        final String currentTime = Calendar.getInstance().getTime().toString();

        HashMap<String, String> metadata = new HashMap<String, String>() {{
            put("ContactTime", currentTime);

        }};

        try {
            final PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            metadata.put("AndroidVersion", pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        createRequest.setMetadata(metadata);
    }

}
