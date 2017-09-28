package com.bikeguard.mobile.bikeguard;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.zendesk.logger.Logger;
import com.zendesk.sdk.model.access.AnonymousIdentity;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;
import com.zendesk.sdk.support.SupportActivity;
import com.zendesk.sdk.requests.RequestActivity;

import com.zopim.android.sdk.api.ZopimChat;
import com.zopim.android.sdk.prechat.ZopimChatActivity;

import java.util.LinkedList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Logger.setLoggable(BuildConfig.DEBUG);
        initializeZendesk();

        ZopimChat.init("tBHcpfcP5vOr2XVkANbBjDPWOIcNlUYw");

       /*
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:+353768888870"));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(callIntent);
        */

        Button mChatButton;
        mChatButton = (Button) findViewById(R.id.chat_button);

        mChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start chat
                startActivity(new Intent(getApplicationContext(), ZopimChatActivity.class));
            }
        });

        findViewById(R.id.call_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SupportActivity to browse Help Center and create/update requests
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:+353768888870"));


                if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                startActivity(callIntent);
            }
        });

        findViewById(R.id.launch_help_center_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SupportActivity to browse Help Center and create/update requests
                new SupportActivity.Builder().show(MainActivity.this);
            }
        });


        // Start RequestActivity to show existing requests
        findViewById(R.id.show_open_requests_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestActivity.startActivity(MainActivity.this, null);
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

        if(email == null){
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

    public String getGoogleEmail() {
        AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type values.
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            return possibleEmails.get(0);


        }
        return null;
    }

}
