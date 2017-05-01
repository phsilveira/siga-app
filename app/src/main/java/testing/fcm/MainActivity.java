package testing.fcm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {


    public static final String MAIN_SITE_URL = "http://www.prowl.esy.es/test",
            STRING_TO_MATCH_FOR_BARCODE_SCAN = "firebarcodescannerforwebsites=1",
            STRING_TO_MATCH_FOR_NFC_SCAN = "firenfccodescannerforwebsites=1",
            POST_URL = "http://www.prowl.esy.es/test/admin/assignments/register/^[0-9]$/";
    private WebView main_web_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseMessaging.getInstance().subscribeToTopic("test");
        FirebaseInstanceId.getInstance().getToken();

        //hide actionbar on top of activity
        ActionBar action_bar = getSupportActionBar();
        action_bar.hide();

        // load site URL
        openUrl(MAIN_SITE_URL);
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
                if (isURLMatchingBarCode(url)) {
                    scanBarCodeNow();
                    return true;
                }
                if (isURLMatchingNfcCode(url)) {
                    scanNfcCodeNow();
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
    protected boolean isURLMatchingBarCode(String url) {
        return url.toLowerCase().contains(STRING_TO_MATCH_FOR_BARCODE_SCAN.toLowerCase());
    }

    /**
     * Function to check if URL match contains our NFC code string
     * @param url string URL to compare
     * @return boolean true or false
     */
    protected boolean isURLMatchingNfcCode(String url) {
        return url.toLowerCase().contains(STRING_TO_MATCH_FOR_NFC_SCAN.toLowerCase());
    }

    /**
     * Initiate the barcode scan
     */
    public void scanBarCodeNow() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
        integrator.setPrompt(String.valueOf("Scan Barcode"));
//        integrator.setResultDisplayDuration(0);
//        integrator.setWide();  // Wide scanning rectangle, may work better for 1D barcodes
        integrator.setCameraId(0);  // Use a specific camera of the device

        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    /**
     * Initiate the nfc scan
     */
    public void scanNfcCodeNow() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
        integrator.setPrompt(String.valueOf("Scan Barcode"));
//        integrator.setResultDisplayDuration(0);
//        integrator.setWide();  // Wide scanning rectangle, may work better for 1D barcodes
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
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }
}
