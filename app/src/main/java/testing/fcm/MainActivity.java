package testing.fcm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    public static final String MAIN_SITE_URL = "http://www.prowl.esy.es/test",
            STRING_TO_MATCH_FOR_BARCODE_SCAN = "firebarcodescannerforwebsites=1",
            POST_URL = "http://www.prowl.esy.es/test/admin/assignments/register/^[0-9]$/";

    private WebView main_web_view;

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;

    private AlertDialog mDialog;

    private List<Tag> mTags = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseMessaging.getInstance().subscribeToTopic("test");
        FirebaseInstanceId.getInstance().getToken();

        //hide actionbar on top of activity
        ActionBar action_bar = getSupportActionBar();
        action_bar.hide();

        resolveIntent(getIntent());

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        //if (mAdapter == null) {
            //showMessage(R.string.error, R.string.no_nfc);
            //finish();
            //return;
        //}

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        mNdefPushMessage = new NdefMessage(new NdefRecord[]{newTextRecord(
                "Message from NFC Reader :-)", Locale.ENGLISH, true)});

        // load site URL
        openUrl(MAIN_SITE_URL);
    }

    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }

    private NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
            mAdapter.enableForegroundNdefPush(this, mNdefPushMessage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
            mAdapter.disableForegroundNdefPush(this);
        }
    }

    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.nfc_disabled);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
        return;
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                byte[] payload = dumpTagData(tag).getBytes();
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
                mTags.add(tag);
            }
            // Setup the views
//            buildTagViews(msgs);
        }
    }

    private String dumpTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        String codeContent = String.valueOf(sb.append(toDec(id)));

        Toast.makeText(this, codeContent, Toast.LENGTH_SHORT).show();

        String webUrl = main_web_view.getUrl();

        //load the URL and Pass the scanned barcode
        openUrl(webUrl + '/' + codeContent);

        return codeContent;
    }


    private long toDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }



    /**
     * function to init a webview and load URL
     * @param url String URL
     */
    public void openUrl(String url){
        main_web_view = (WebView) findViewById(R.id.mainWebView);
        //enable javascript
        main_web_view.getSettings().setJavaScriptEnabled(true);

        // get the activity context
        final Activity activity = this;

        main_web_view.getSettings().setLoadWithOverviewMode(true);
        main_web_view.getSettings().setUseWideViewPort(true);
        main_web_view.getSettings().setBuiltInZoomControls(true);
        main_web_view.getSettings().setDisplayZoomControls(false);

        //set client to handle errors and intercept link clicks
        main_web_view.setWebViewClient(new WebViewClient(){

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                String msg = "error : "+description+" Request URL : "+failingUrl;
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // we will interrupt the link here
                if (isURLMatching(url)) {
                    scanBarCodeNow();
                    return true;
                }
                return super.shouldOverrideUrlLoading(view,url);
            }

        });

        //load the URL
        main_web_view.loadUrl(url);
    }

    /**
     * Function to check if URL match contains our barcode string
     * @param url string URL to compare
     * @return boolean true or false
     */
    protected boolean isURLMatching(String url) {
        return url.toLowerCase().contains(STRING_TO_MATCH_FOR_BARCODE_SCAN.toLowerCase());
    }

    /**
     * Initiate the barcode scan
     */
    public void scanBarCodeNow() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
        integrator.setPrompt(String.valueOf("Scan Barcode"));
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    /**
     * function handle scan result
     *
     * @param requestCode scanned code
     * @param resultCode  result of scanned code
     * @param intent      intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (scanningResult != null) {
            //we have a result
            String codeContent = scanningResult.getContents();
            String codeFormat = scanningResult.getFormatName();
            String webUrl = main_web_view.getUrl();

            Toast toast = Toast.makeText(getApplicationContext(), codeContent , Toast.LENGTH_SHORT);
            toast.show();

            //load the URL and Pass the scanned barcode
            openUrl(webUrl+ '/' + codeContent);

        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && main_web_view.canGoBack()) {
            main_web_view.goBack();
            return true;
        }
        else {
            return false;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        //return super.onKeyDown(keyCode, event);
    }
}
