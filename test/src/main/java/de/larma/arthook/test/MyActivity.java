package de.larma.arthook.test;

import android.app.Activity;
import android.net.sip.SipAudioCall;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MyActivity extends Activity {

    private static final String TAG = "ahook.MyActivity";

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Log.d(TAG, "setContentView(" + Integer.toHexString(R.layout.activity_my) + ");");
            setContentView(R.layout.activity_my);
            //Activity.class.getDeclaredMethod("setContentView", int.class).invoke(this, R.layout.activity_my);
            //ArtHook.about(System.class.getDeclaredMethod("arraycopy", Object.class, int.class, Object.class, int.class, int.class));
            //ArtHook.backupMethods.get(new MethodInfo(Activity.class, "setContentView", int.class)).invoke(this, R.layout.activity_my);
        } catch (Exception e) {
            Log.d(TAG, "Catching exception");
            Log.d(TAG, "e: ", e);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        Log.d(TAG, "before Activity.setContentView");
        super.setContentView(layoutResID);
        //Log.d(TAG, "after Activity.setContentView");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            try {
                SipAudioCall call = new SipAudioCall(this, null);
                call.startAudio();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
