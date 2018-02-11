package net.rotunneling.rotunnelingvpn;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

//import com.meiqia.core.MQManager;
//import com.wxy.contact.util.MQConfig;
//import com.wxy.contact.util.MQIntentBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.OpenVpnApi;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.core.VpnStatus;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private boolean isFirst;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
//        findViewById(R.id.fab).setOnClickListener(this);


        sharedPreferences = getSharedPreferences("rotunneling_vpn", MODE_PRIVATE);
        isFirst = sharedPreferences.getBoolean("is_first", true);
        VpnStatus.initLogCache(getApplicationContext().getCacheDir());

        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        EditText et_username = (EditText) findViewById(R.id.username);
        et_username.setText(mSettings.getString("username", "").toString());
        EditText et_password = (EditText) findViewById(R.id.password);
        et_password.setText(mSettings.getString("password", "").toString());

        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        listCertificates();

    }


    private void listCertificates() {
        String[] list;
        try {
            list = getAssets().list("certs");
            if (list.length > 0) {

                ArrayAdapter adapter = new ArrayAdapter<String>(this,
                        R.layout.activity_listview, list);

                ListView listView = (ListView) findViewById(R.id.servers);
                listView.setAdapter(adapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        // When clicked, show a toast with the TextView text
                        TextView c = view.findViewById(R.id.label);
                        String playerChanged = c.getText().toString();
                        startVpn(playerChanged);
                    }
                });
            }
        } catch (IOException e) {

        }

    }


    private void startVpn(String certificate) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        EditText et_username = (EditText) findViewById(R.id.username);
        String username = et_username.getEditableText().toString();

        EditText et_password = (EditText) findViewById(R.id.password);
        String password = et_password.getEditableText().toString();

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.apply();

        EditText s_proxy = (EditText) findViewById(R.id.proxy);
        String proxy = s_proxy.getText().toString();
        String[] proxy_parts = proxy.split(":");


        String config = "";
        try {
            InputStream conf = getAssets().open("certs/" + certificate);// TODO replace your own authentication file in /assets/client.bin
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                config += line + "\n";
            }

            //add the proxy server and port if filled
            if (!proxy.matches("")) {
                config += "http-proxy-retry\nhttp-proxy " + proxy_parts[0] + " " + proxy_parts[1];
            }

        } catch (IOException ignore) {
            Toast.makeText(this, "An error has occured", Toast.LENGTH_LONG).show();
        }
        try {
            OpenVpnApi.startVpn(this, config, username, password);
        } catch (RemoteException e) {
            Toast.makeText(this, "Certificate error", Toast.LENGTH_LONG).show();
        }
    }

    private MyBottomSheetDialog bottomSheetDialog;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.servers:// 点击连接VPN
                if (isFirst) {
                    Snackbar.make(v, R.string.click_again, Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            v.setVisibility(View.GONE);
                        }
                    }).show();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("is_first", false);
                    isFirst = false;
                    editor.apply();
                }
                break;
            default:
                break;
        }
    }
}
