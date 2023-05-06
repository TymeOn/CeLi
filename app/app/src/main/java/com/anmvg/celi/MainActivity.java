package com.anmvg.celi;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.anmvg.celi.databinding.ActivityMainBinding;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;

import com.anmvg.celi.SOAP.*;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Connection;
import com.zeroc.Ice.InitializationData;
import com.zeroc.Ice.InvocationFuture;
import com.zeroc.Ice.LocalException;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    // ICE VARIABLES

    public static final int MSG_READY = 1;
    public static final int MSG_EXCEPTION = 2;
    public static final int MSG_RESPONSE = 3;
    private Handler _uiHandler;
    private Communicator _communicator;
    private MusicPlayerPrx _proxy = null;
    private InvocationFuture<?> _result;
    private final DeliveryMode _mode = DeliveryMode.TWOWAY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.anmvg.celi.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // ICE INITIALIZATION

        _uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message m) {
                if(m.what == MSG_READY) {
                    MessageReady ready = (MessageReady)m.obj;
                    _communicator = ready.communicator;
                }
                else if(m.what == MSG_EXCEPTION || m.what == MSG_RESPONSE) {
                    _result = null;
                }
            }
        };

        // SSL initialization can take some time. To avoid blocking the
        // calling thread, we perform the initialization in a separate thread.
        new Thread(() -> {
            try
            {
                InitializationData initData = new InitializationData();

                initData.dispatcher = (Runnable runnable, Connection connection) -> _uiHandler.post(runnable);

                initData.properties = Util.createProperties();
                initData.properties.setProperty("Ice.Trace.Network", "3");

                initData.properties.setProperty("IceSSL.Trace.Security", "3");
                initData.properties.setProperty("IceSSL.KeystoreType", "BKS");
                initData.properties.setProperty("IceSSL.TruststoreType", "BKS");
                initData.properties.setProperty("IceSSL.Password", "password");
                initData.properties.setProperty("Ice.Plugin.IceSSL", "com.zeroc.IceSSL.PluginFactory");
                initData.properties.setProperty("Ice.Default.Package", "com.anmvg.celi");

                //
                // We need to postpone plug-in initialization so that we can configure IceSSL
                // with a resource stream for the certificate information.
                //
                initData.properties.setProperty("Ice.InitPlugins", "0");

                Communicator c = Util.initialize(initData);

                //
                // Now we complete the plug-in initialization.
                //
                com.zeroc.IceSSL.Plugin plugin = (com.zeroc.IceSSL.Plugin)c.getPluginManager().getPlugin("IceSSL");
                //
                // Be sure to pass the same input stream to the SSL plug-in for
                // both the keystore and the truststore. This makes startup a
                // little faster since the plug-in will not initialize
                // two keystores.
                //
                java.io.InputStream certs = getResources().openRawResource(R.raw.client);
                plugin.setKeystoreStream(certs);
                plugin.setTruststoreStream(certs);
                c.getPluginManager().initializePlugins();

                _uiHandler.sendMessage(Message.obtain(_uiHandler, MSG_READY, new MessageReady(c, null)));
            }
            catch(LocalException e)
            {
                e.printStackTrace();
                _uiHandler.sendMessage(Message.obtain(_uiHandler, MSG_READY, new MessageReady(null, e)));
            }
        }).start();

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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(_communicator != null)
        {
            _communicator.destroy();
        }
    }


    // ICE FUNCTIONS

    static class MessageReady {
        MessageReady(Communicator c, LocalException e) {
            communicator = c;
            ex = e;
        }

        Communicator communicator;
        LocalException ex;
    }

    void playMusic(String filename) {
        try {
            updateProxy();
            if(_proxy == null || _result != null) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                _proxy.playMusicAsync(filename).whenComplete((result, ex) -> {
                    if(ex != null) {
                        _uiHandler.sendMessage(_uiHandler.obtainMessage(MSG_EXCEPTION, ex));
                    }
                });
            }
        }
        catch(LocalException ex) {
            _uiHandler.sendMessage(_uiHandler.obtainMessage(MSG_EXCEPTION, ex));
        }
    }

    void stopMusic() {
        try {
            updateProxy();
            if(_proxy == null || _result != null) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                _proxy.stopMusicAsync().whenComplete((result, ex) -> {
                    if(ex != null) {
                        _uiHandler.sendMessage(_uiHandler.obtainMessage(MSG_EXCEPTION, ex));
                    }
                });
            }
        }
        catch(LocalException ex) {
            _uiHandler.sendMessage(_uiHandler.obtainMessage(MSG_EXCEPTION, ex));
        }
    }


    private void updateProxy() {
        if(_proxy != null) {
            return;
        }

        String s = "musicplayer:tcp -h " + BuildConfig.ICE_APP_HOST + " -p 10000:ssl -h " + BuildConfig.ICE_APP_HOST + " -p 10001:udp -h " + BuildConfig.ICE_APP_HOST  + " -p 10000";

        ObjectPrx prx = _communicator.stringToProxy(s);
        prx = _mode.apply(prx);
        _proxy = MusicPlayerPrx.uncheckedCast(prx);
    }

}
