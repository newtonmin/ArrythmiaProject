package com.siza.arrythmia.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

/**
 * Created by Siza on 5/26/2014.
 */
public class DeviceListActivity extends ActionBarActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id)      //Checking which menu pressed
        {
            case R.id.action_scan:

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
