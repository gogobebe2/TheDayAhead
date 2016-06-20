package com.gmail.gogobebe2.thedayahead;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.ConnectException;

public class TimetableFragment extends TheDayAheadFragment implements View.OnClickListener {
    private RelativeLayout relativeLayout;
    private Document kmarDocument = null;
    private final String KMAR_LOGIN_URL = "https://portal.sanctamaria.school.nz/student/index.php/login";
    private final String KMAR_TIMETABLE_URL = "https://portal.sanctamaria.school.nz/student/index.php/timetable";

    public TimetableFragment() { /* Required empty public constructor */}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        relativeLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_timetable, parent, false);

        initKmarLoginConnection();

        Button loginButton = (Button) relativeLayout.findViewById(R.id.login_button);
        loginButton.setOnClickListener(this);

        EditText usernameField = (EditText) relativeLayout.findViewById(R.id.editText_username);
        EditText passwordField = (EditText) relativeLayout.findViewById(R.id.editText_password);
        CheckBox rememberMeCheckBox = (CheckBox) relativeLayout.findViewById(R.id.checkBox_rememberMe);

        SharedPreferences loginPreferences = getLoginPreferencesInstance();

        boolean saveLogin = loginPreferences.getBoolean("saveLogin", false);
        if (saveLogin) {
            usernameField.setText(loginPreferences.getString("username", ""));
            passwordField.setText(loginPreferences.getString("password", ""));
            rememberMeCheckBox.setChecked(true);
        }

        return relativeLayout;
    }

    private SharedPreferences getLoginPreferencesInstance() {
        return getActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    String getTitle() {
        return "Timetable";
    }

    @Override
    public String getLoggingTag() {
        return Utils.createTagName(TimetableFragment.class);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initKmarLoginConnection() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    kmarDocument = Jsoup.connect(KMAR_LOGIN_URL).get();
                    if (kmarDocument == null) throw new NullPointerException();
                } catch (IOException e) {
                    if (e instanceof ConnectException) Log.w(getLoggingTag(),
                            "ConnectException, Kmar Portal down or internet down.");
                    else {
                        Log.w(getLoggingTag(), "Failed to connect to Kmar Portal.");
                        e.printStackTrace();
                    }
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean succesful) {
                if (succesful)
                    Toast.makeText(getContext(), "Successfully connected to the Kmar Portal.",
                            Toast.LENGTH_SHORT).show();
                else Toast.makeText(getContext(),
                        "Failed to connect to the Kmar Portal.", Toast.LENGTH_LONG).show();
            }
        }.execute();

    }

    @SuppressWarnings("deprecation")
    private static void clearCookies(TimetableFragment instance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.d(instance.getLoggingTag(), "Using clearCookies code for API >=" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            Log.d(instance.getLoggingTag(), "Using clearCookies code for API <" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(instance.getContext());
            cookieSyncManager.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncManager.stopSync();
            cookieSyncManager.sync();
        }
    }

    private void updateLoginPreferences(CheckBox rememberMeCheckBox, EditText usernameEditText,
                                      EditText passwordEditText) {
        String usernameString = usernameEditText.getText().toString();
        String passwordString = passwordEditText.getText().toString();

        SharedPreferences.Editor loginPrefEditor = getLoginPreferencesInstance().edit();
        if (rememberMeCheckBox.isChecked()) {
            loginPrefEditor.putBoolean("saveLogin", true);
            loginPrefEditor.putString("username", usernameString);
            loginPrefEditor.putString("password", passwordString);
            loginPrefEditor.apply();
        }
        else {
            loginPrefEditor.clear();
            loginPrefEditor.apply();
        }
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.login_button) {
            if (kmarDocument == null) initKmarLoginConnection();
            else {
                EditText usernameEditText = (EditText) relativeLayout.findViewById(R.id.editText_username);
                EditText passwordEditText = (EditText) relativeLayout.findViewById(R.id.editText_password);

                updateLoginPreferences((CheckBox) relativeLayout.findViewById(R.id.checkBox_rememberMe),
                        usernameEditText, passwordEditText);

                WebView webView = new WebView(getContext());

                webView.setVisibility(View.INVISIBLE);

                webView.clearCache(true);
                webView.clearHistory();
                clearCookies(this);

                class HTMLRetrieverJavaScriptInterface {
                    @JavascriptInterface
                    void showHTML(String html) {
                        // TODO make this method create a TimetableParser instance using this html and
                        // store it in a instance variable of the TimetableFragment class.
                        Toast.makeText(getContext(), html, Toast.LENGTH_LONG).show();
                    }
                }

                webView.addJavascriptInterface(new HTMLRetrieverJavaScriptInterface(), "HtmlRetriever");

                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onLoadResource(WebView webView, String destinationUrl) {
                        CheckBox rememberMeCheckbox = (CheckBox) relativeLayout.findViewById(R.id.checkBox_rememberMe);
                        ProgressBar progressBar = (ProgressBar) relativeLayout.findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.VISIBLE);
                        rememberMeCheckbox.setVisibility(View.INVISIBLE);
                        super.onLoadResource(webView, destinationUrl);
                    }

                    @Override
                    public void onPageFinished(WebView webView, String urlLoaded) {
                        final String LOGIN_JAVASCRIPT = "javascript:document.getElementById(\"loginSubmit\").click()";
                        final String HTML_RETRIEVER_JAVASCRIPT = "javascript:window.HtmlRetriever.showHTML" +
                                "('<html>' + document.getElementsByTagName('html')[0].innerHTML + '</html>');";
                        if (urlLoaded.equals(KMAR_TIMETABLE_URL)) {
                            webView.setVisibility(View.VISIBLE); // TODO remove and reformat timetable.
                            webView.loadUrl(HTML_RETRIEVER_JAVASCRIPT);
                        } else if (!urlLoaded.equals(LOGIN_JAVASCRIPT)) {
                            webView.loadUrl(LOGIN_JAVASCRIPT);
                        }
                    }
                });

                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);

                relativeLayout.addView(webView);

                Element loginUsernameElement = kmarDocument.select("input#loginUsername").first();
                Element loginPasswordElement = kmarDocument.select("input#loginPassword").first();

                loginUsernameElement.attr("value", usernameEditText.getText().toString());
                loginPasswordElement.attr("value", passwordEditText.getText().toString());

                webView.loadData(kmarDocument.html(), "text/html", "UTF-8");

                // I then call the click() function on the loginSubmit button when the page is finished
                // loading in the overridden onPageFinished(WebView webView, String url) method.
            }
        }
    }
}
