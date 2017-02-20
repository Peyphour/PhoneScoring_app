package bnancy.fr.autophoneblocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyReceiver extends BroadcastReceiver {

    Context context;

    @Override
    public void onReceive(Context context, Intent intent) {

        // If, the received action is not a type of "Phone_State", ignore it
        if (!intent.getAction().equals("android.intent.action.PHONE_STATE"))
            return;

        this.context = context;

        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        new CallBackend().execute(number);

    }

    public boolean killCall(Context context) {
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");
            Method methodSilenceRinger = telephonyInterfaceClass.getDeclaredMethod("silenceRinger");

            methodSilenceRinger.invoke(telephonyInterface);

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);


        } catch (Exception ex) { // Many things can go wrong with reflection calls
            Log.d("PHONE", "PhoneStateReceiver **" + ex.toString());
            return false;
        }
        return true;
    }

    class CallBackend extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            OkHttpClient okHttpClient = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("http://bnancy.fr:8081/score/" + params[0])
                    .build();

            Response response = null;
            try {
                response = okHttpClient.newCall(request).execute();

                String json = response.body().string();

                Log.i("JSON", json);
                JSONObject jsonObject = new JSONObject(json);
                return jsonObject;
            } catch (IOException | JSONException e) {
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            try {
                if(jsonObject.getString("recommendation").equals("block")) {
                    Log.i("POSTEXECUE", "killcall");
                    killCall(context);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
