package opiskelu.luenfc;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Vector;

import static android.widget.Toast.LENGTH_SHORT;


public class MainActivity extends AppCompatActivity{

    // Muuttujien alustusta

    //private static final String MIME_TEXT_PLAIN = "text/plain";
    private static final String TAG = "NfcDemo";
    final String serverURL = "http://193.167.151.5/";


    private NfcAdapter mNfcAdapter;
    private TextView WiFiStateTextView;
    private String ssid_ssid;
    public String deviceName;
    private Button informationButton, uploadButton, downloadButton, openManualButton;
    private Vector<String> results;
    private MyReceiver mr;
    private fileTransfer fileTransferObject;
    private TextView mTextView;
    private String infoLink;

    private final int REQUEST_WRITE_STORAGE = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //onCreate-funktio suoritetaan aina kun ohjelman suoritus menee pääruutuun

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Muuttujien ja nappien alustuksia

        results = new Vector<>();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mTextView = (TextView) findViewById(R.id.textView_explanation);
        WiFiStateTextView = (TextView) findViewById(R.id.textView_WiFiState);

        Button WiFi = (Button)findViewById(R.id.WiFi);
        informationButton = (Button) findViewById(R.id.more_info);
        uploadButton = (Button) findViewById(R.id.upload);
        downloadButton = (Button) findViewById(R.id.download);
        openManualButton = (Button) findViewById(R.id.manual);
        fileTransferObject = new fileTransfer(serverURL);

        // WiFi.setOnClickListener luo uuden funktion, joka suoritetaan kun WiFi-nappia painetaan

        WiFi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
            }
        });

        // Tarkistetaan, että laitteessa on NFC-ominaisuus, käyttäjälle ilmoitetaan mikäli laitteessa ei ole NFC-ominaisuutta, NFC ei ole päällä tai mikäli NFC on päällä.

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }

        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText(R.string.disabled);
        }

        if (mNfcAdapter.isEnabled()) {

            mTextView.setText(R.string.enabled);
        }

        // Tarkistetaan, että ohjelmalla on lupa kirjoittaa puhelimen muistiin

        boolean hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }

        // checkWifiOnAndConnected() tarkistaa puhelimen WiFi-yhteyden tilan, funktio palauttaa falsen mikäli laite ei ole yhdistetty verkkoon tai WiFi ei ole päällä, true kun WiFi on yhdistetty verkkoon.

        checkWifiConnection();

        //RegisterReceiver tarkkailee, mikäli WiFi-yhteys muuttuu ja muuttaa pääruudun tekstikentän tekstiä eli näyttää käyttäjälle mihin WiFi-verkkoon laite on kytketty
        mr = new MyReceiver();
        registerReceiver(mr, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

}
    private boolean checkWifiConnection() {

        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapteri päällä
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 ){
                WiFiStateTextView.setText(R.string.WiFiEnabledDisconnected);
                return false; // Ei yhteydessä WiFi-verkkoon, WiFi on päällä
            }
            String ssid_message = "You are connected to: " + wifiInfo.getSSID();
            ssid_ssid = wifiInfo.getSSID();
            WiFiStateTextView.setText(ssid_message);
            return true;
        }
        else {
            WiFiStateTextView.setText(R.string.WiFiDisabled);
            return false; // Wi-Fi ei ole päällä
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // Tarkistaa, voiko laitteen sisäiseen muistiin kirjoittaa dataa

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {

                if(!(grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void downloadClicked(View view) {

        //Funktio suoritetaan kun käyttäjä painaa "Download"-nappia, ensin tarkistetaan verkko, kun laite on kytketty oikeaan verkkoon, tarkistetaan löytyykö tiedostoa muistista, jos ei löydy niin se ladataan

        if ( ssid_ssid.equals("\"kk\"") ) {
            fileTransferObject.downloadFile(MainActivity.this, deviceName + ".xlsx");

        } else {
            Toast.makeText(MainActivity.this, "Connect to kk before downloading", LENGTH_SHORT).show();
        }
    }

    public void uploadClicked(View view) {

        //Funktio suoritetaan kun käyttäjä painaa "Upload"-nappia, ensin tarkistetaan verkko, kun laite on kytketty oikeaan verkkoon niin luodaan uusi thread, jossa suoritetaan "Upload"-funktio
        // joka lähettää tiedoston palvelimelle, käyttäjälle ilmoitetaan, mikäli laite on kytketty väärään verkkoon.

        if ( ssid_ssid.equals("\"kk\"") ) {
            new Thread(new Runnable() {
                public void run() {
                    fileTransferObject.uploadFile(MainActivity.this, deviceName + ".xlsx");
                }
            }).start();
        } else {
            Toast.makeText(MainActivity.this, "Connect to kk before uploading", LENGTH_SHORT).show();
        }
    }

    public void infoClicked(View view) {

        //Avataan linkki PrinLabin infosivulle
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(infoLink));
        startActivity(browserIntent);

    }

    public void openManual(View view) {
        // Suoritetaan funktio, jossa ladataan ja avataan käyttöohje

        if ( ssid_ssid.equals("\"kk\"") ) {

            fileTransferObject.downloadFile(MainActivity.this, deviceName + ".pdf");

        } else {
            Toast.makeText(MainActivity.this, "Connect to kk before downloading", LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onStop(){

        Log.e("stop", "lopetetaan");
        super.onStop();
    }

    @Override
    protected void onResume() {

        // Suoritetaan käyttäjän palatessa sovellukseen

        super.onResume();
        setupForegroundDispatch(this, mNfcAdapter);
        //checkWifiOnAndConnected();
    }
    @Override
    protected void onPause() {

        // Suoritetaan käyttäjän "pysäyttäessä" sovelluksen, esim. kun käytetään jotain toista sovellusta

        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mr);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {

        handleIntent(intent);
    }
    private void handleIntent(Intent intent) {

        // Luetaan NFC-tag ja suoritetaan sen sisältö

        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            String type = intent.getScheme();
            //String type = intent.getType();
            // jos kyseisissä nfc tarrassa on URL ensimmäisenä tallenteena.
            if (type.equals("http")) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                // luetaan tarra
                new NdefReaderTask().execute(tag);
            }

            else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        }

        else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();
            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }
    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    private void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {

        // ohjelma on kiinnostunut vain sellaisista nfc tarroista joitten alussa on URL
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        filters[0].addDataScheme("http");
        /*try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }*/
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    private static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }




    private class NdefReaderTask extends AsyncTask<Tag, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Tag... params) {
            boolean isOk = false;
            results.clear();
            Tag tag = params[0];
            Ndef ndef = Ndef.get(tag);

            if (ndef == null) {
                // NDEF is not supported by this Tag.
                Log.e("NFC", "error");
                return false;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
            NdefRecord[] records = ndefMessage.getRecords();

            // luetaan nfc data tallenteet
            for (NdefRecord ndefRecord : records) {

                // luetaan ainoastaan tekti tallenteet.
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT  )) {

                    try {
                        results.add(readText(ndefRecord));
                        isOk = true;
                        Log.i("NFC", results.elementAt(0));
                    }

                    catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }
            return isOk;
        }
        private String readText(NdefRecord record) throws UnsupportedEncodingException {

            // Lukee NFC-tagin

            byte[] payload = record.getPayload();
            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            // Get the Language Code
            int languageCodeLength = payload[0] & 51;
            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"
            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {


                //infoLink = link + results.elementAt(1);
/*
                ConnectivityManager cm = (ConnectivityManager)MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if( activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                    informationButton.setEnabled(true);
                }
*/
                if ( checkWifiConnection() ) {
                    if (ssid_ssid.equals("\"kk\"")) {

                        Log.i("NFC", "sql haku");
                        new SigningActivity().execute(results.elementAt(0));

                    }
                }
            }
        }
    }

    private class SigningActivity extends AsyncTask<String, Void, Void> {

        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... arg0) {

            // Ottaa yhteyttä tietokantaan, hakee tietokannasta NFC-tagin ID:n perusteella laitteen nimen.
            // Nimi tallennetaan muuttujaan "deviceName", ja tämän perusteella ohjelma osaa avata verkkosivun tai avata käyttöohjeen
            // infoLink sisältää PrinLab:n verkko-osoitteen ja laitteen nimi lisätään perään

            try {

                String sqlLink = serverURL + "sqlandroidup.php";
                String data = URLEncoder.encode("id", "UTF-8") + "=" + URLEncoder.encode(arg0[0], "UTF-8");
                HttpHandler sh = new HttpHandler();

                String response = sh.makeServiceCall(sqlLink, data);

                if (response != null) {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        JSONObject device = jObj.getJSONObject("device");
                        //String id = jObj.getString("id");
                        final String name = "Device name: " + device.getString("name");
                        Log.i("JSON", name);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mTextView.setText(name);
                            }
                        });


                        deviceName = device.getString("url");
                        infoLink = "http://www.oamk.fi/hankkeet/prinlab/equipment/index.php?page=" + deviceName;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                informationButton.setEnabled(true);
                                uploadButton.setEnabled(true);
                                downloadButton.setEnabled(true);
                                openManualButton.setEnabled(true);

                            }
                        });


                    } else {

                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            informationButton.setEnabled(false);
                            uploadButton.setEnabled(false);
                            downloadButton.setEnabled(false);
                            openManualButton.setEnabled(false);

                        }
                    });

                }

            }

            catch (JSONException e) {
                // JSON error
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();

            }

            return null;
        }
    }

    // WiFi:n tila muuttunut
    private class MyReceiver extends BroadcastReceiver {

        private MyReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            checkWifiConnection();

            // resetoidaan käyttöliittymä
            downloadButton.setEnabled(false);
            uploadButton.setEnabled(false);
            openManualButton.setEnabled(false);
            informationButton.setEnabled(false);
            mTextView.setText(R.string.device_not_available);
        }
    }
}

