package com.acentria.benslist;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.adapters.ActionbarIconSpinnerAdapter;
import com.acentria.benslist.adapters.SpinnerAdapter;
import com.acentria.benslist.controllers.AccountArea;
import com.acentria.benslist.controllers.CharityArea;
import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;


public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity=> ";
    private static HashMap<String, String> formData = new HashMap<String, String>();
    public static EditProfileActivity instance;
    /*private static LinkedHashMap<String, HashMap<String,String>> data = new LinkedHashMap<String, HashMap<String,String>>();*/
    private static HashMap<String, View> fieldViews = new HashMap<String, View>();
    public static LinearLayout account_fields_area;

    public static MenuItem actionbarSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Lang.setDirection(this);
        instance = this;

        setTitle(Lang.get("title_activity_edit_profile"));
        Log.e(TAG, "ActivityName" + Lang.get("title_activity_edit_profile"));
        setContentView(R.layout.activity_edit_profile);

        /* enable back action */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        LinearLayout fields_area = (LinearLayout) findViewById(R.id.fields_area);

        /* build profile fields */
        LinearLayout form_layout = (LinearLayout) getLayoutInflater().inflate(R.layout.edit_profile_fields, null);

        final LinearLayout profile_fields_area = (LinearLayout) form_layout.findViewById(R.id.edit_profile_fields);
        account_fields_area = (LinearLayout) form_layout.findViewById(R.id.edit_account_fields);

        // username
        TextView username = (TextView) profile_fields_area.findViewById(R.id.username);
        username.setText(Account.accountData.get("username"));

        // email
        final LinearLayout email_field_cont = (LinearLayout) profile_fields_area.findViewById(R.id.email_field_cont);
        final EditText email_field = (EditText) profile_fields_area.findViewById(R.id.email_field);

        if (Utils.getCacheConfig("account_edit_email_confirmation").equals("1")) {
            LinearLayout email_cont = (LinearLayout) profile_fields_area.findViewById(R.id.email_view_cont);
            email_field_cont.setVisibility(View.GONE);
            email_cont.setVisibility(View.VISIBLE);

            TextView email_view = (TextView) profile_fields_area.findViewById(R.id.email_view);
            email_view.setText(Account.accountData.get("mail"));

            ImageView edit_email = (ImageView) profile_fields_area.findViewById(R.id.edit_email);
            edit_email.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    LinearLayout email_field_dialog = (LinearLayout) getLayoutInflater()
                            .inflate(R.layout.dialog_edit_text, null);

                    final EditText new_email_field = (EditText) email_field_dialog.getChildAt(0);
                    new_email_field.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

                    // custom on click listener
                    class CustomListener implements View.OnClickListener {
                        private final AlertDialog dialog;

                        public CustomListener(AlertDialog dialog) {
                            this.dialog = dialog;
                        }

                        @Override
                        public void onClick(View v) {
                            String new_email = new_email_field.getText().toString();

                            Matcher matcher = Pattern.compile("(.+@.+\\.[a-z]+)").matcher(new_email);

                            if (!matcher.matches()) {
                                new_email_field.setError(Lang.get("bad_email"));
                                return;
                            } else {
                                new_email_field.setError(null);

                                this.dialog.dismiss();

                                final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("loading"));

                                /* get form data - build request url */
                                HashMap<String, String> params = new HashMap<String, String>();
                                //params.put("current_type", Account.accountData.get("type"));
                                params.put("new_email", new_email);
                                params.put("account_id", Account.accountData.get("id"));
                                params.put("password_hash", Utils.getSPConfig("accountPassword", null));
                                final String url = Utils.buildRequestUrl("updateProfileEmail", params, null);

                                /* do async request */
                                AsyncHttpClient client = new AsyncHttpClient();
                                client.post(url, Utils.toParams(formData), new AsyncHttpResponseHandler() {

                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                                        // called when response HTTP status is "200 OK"
                                        try {
                                            String response = String.valueOf(new String(server_response, "UTF-8"));
                                            /* dismiss progress dialog */
                                            progress.dismiss();

                                            /* parse response */
                                            XMLParser parser = new XMLParser();
                                            Document doc = parser.getDomElement(response, url);

                                            if (doc == null) {
                                                Dialog.simpleWarning(Lang.get("returned_xml_failed"), instance);
                                            } else {
                                                NodeList errorNode = doc.getElementsByTagName("error");

                                                /* handle errors */
                                                if (errorNode.getLength() > 0) {
                                                    Element error = (Element) errorNode.item(0);
                                                    Dialog.simpleWarning(Lang.get(error.getTextContent()), instance);
                                                }
                                                /* finish this activity and show toast */
                                                else {
                                                    NodeList successNode = doc.getElementsByTagName("success");

                                                    if (successNode.getLength() > 0) {
                                                        Dialog.simpleWarning(Lang.get("dialog_email_saved_as_tmp"), instance);
                                                    } else {
                                                        Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), instance);
                                                    }
                                                }
                                            }

                                        } catch (UnsupportedEncodingException e1) {

                                        }
                                    }

                                    @Override
                                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                                    }
                                });
                            }
                        }
                    }

                    // create dialog
                    AlertDialog dialog = Dialog.confirmActionView(Lang.get("dialog_confirm_email_changing"), instance, email_field_dialog, null, null);
                    Button positive_button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    positive_button.setOnClickListener(new CustomListener(dialog));
                }
            });
        } else {
            formData.put(Account.email_field_key, Account.accountData.get("mail"));
            email_field.setText(Account.accountData.get("mail"));

            // add email field to fields data to allow validator validate on form submit | in "allow to change email" mode
            fieldViews.put(Account.email_field_key, email_field_cont);

            // add listener
            email_field.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable text) {
                    formData.put(Account.email_field_key, text.toString());
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });
        }

        // account type
        final HashMap<String, String> account_type_field_info = new HashMap<String, String>();
        account_type_field_info.put("key", "account_type");
        account_type_field_info.put("data", "");

        final ArrayList<HashMap<String, String>> account_types = new ArrayList<HashMap<String, String>>();

        HashMap<String, String> placeholder = new HashMap<String, String>();
        placeholder.put("name", Lang.get("account_type"));
        placeholder.put("key", "");
        account_types.add(placeholder);

        int current_type_position = 0;
        int index = 0;

        for (Entry<String, HashMap<String, String>> entry : Config.cacheAccountTypes.entrySet()) {
            account_types.add(entry.getValue());
            index++;
            if (entry.getValue().get("key").equals(Account.accountData.get("type"))) {
                current_type_position = index;
            }
        }

        Spinner account_type = (Spinner) profile_fields_area.findViewById(R.id.account_type);
        SpinnerAdapter adapter = new SpinnerAdapter(instance, account_types, account_type_field_info, formData, null);

        account_type.setAdapter(adapter);
        account_type.setSelection(current_type_position, false);
        formData.put(account_type_field_info.get("key"), Account.accountData.get("type"));

        fields_area.addView(form_layout);
        Account.requestAccountForm(Account.accountData.get("type"), instance, profile_fields_area, account_fields_area, formData, fieldViews);

        /* account type spinner handler */
        account_type.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                /* do default listener actions */
                final String selected_key = account_types.get(position).get("key");
                formData.remove(account_type_field_info.get("key"));
                formData.put(account_type_field_info.get("key"), selected_key);

                /* show dialog */
                Dialog.confirmAction(Lang.get("dialog_confirm_action"), instance, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //formData.clear();
                        fieldViews.clear();
                        Account.data.clear();

                        formData.put("current_type", Account.accountData.get("type"));
                        Account.accountData.put("type", selected_key);

                        formData.put(account_type_field_info.get("key"), selected_key);

                        ProgressBar progressBar = (ProgressBar) getLayoutInflater()
                                .inflate(R.layout.loading, null);

                        Spinner multilingual = (Spinner) profile_fields_area.findViewWithTag("multilingual_spinner");
                        profile_fields_area.removeView(multilingual);

                        account_fields_area.removeAllViews();
                        account_fields_area.addView(progressBar);

                        Account.requestAccountForm(selected_key, instance, profile_fields_area, account_fields_area, formData, fieldViews);

                        // add email field to fields data to allow validator validate on form submit | in "allow to change email" mode
                        if (!Utils.getCacheConfig("account_edit_email_confirmation").equals("1")) {
                            fieldViews.put(Account.email_field_key, email_field_cont);
                        }
                    }
                }, null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /* cancel button */
        Button reset_button = (Button) findViewById(R.id.form_reset);
        reset_button.setText(Lang.get("dialog_cancel"));
        reset_button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                instance.finish();
            }
        });

        /* submit button */
        Button submit_button = (Button) findViewById(R.id.form_submit);
        submit_button.setText(Lang.get("edit_profile"));
        submit_button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                /* validate form data */
                if (!Forms.validate(formData, Account.data, fieldViews)) {
                    Toast.makeText(Config.context, Lang.get("dialog_fill_required_fields"), Toast.LENGTH_SHORT).show();
                } else {
                    /* show progress dialog */
                    final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("loading"));

                    /* get form data - build request url */
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("current_type", Account.accountData.get("type"));
                    params.put("account_id", Account.accountData.get("id"));
                    params.put("password_hash", Utils.getSPConfig("accountPassword", null));
                    final String url = Utils.buildRequestUrl("updateProfile", params, null);

                    /* do async request */
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.post(url, Utils.toParams(formData), new AsyncHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                            // called when response HTTP status is "200 OK"
                            try {
                                String response = String.valueOf(new String(server_response, "UTF-8"));
                                /* dismiss progress dialog */
                                progress.dismiss();

                                /* parse response */
                                XMLParser parser = new XMLParser();
                                Document doc = parser.getDomElement(response, url);

                                if (doc == null) {
                                    Dialog.simpleWarning(Lang.get("returned_xml_failed"), instance);
                                } else {
                                    NodeList errorNode = doc.getElementsByTagName("error");

                                    /* handle errors */
                                    if (errorNode.getLength() > 0) {
                                        Element error = (Element) errorNode.item(0);
                                        Dialog.simpleWarning(Lang.get(error.getTextContent()), instance);
                                    }
                                    /* finish this activity and show toast */
                                    else {
                                        NodeList successNode = doc.getElementsByTagName("success");

                                        if (successNode.getLength() > 0) {
                                            /* update full name */
                                            String full_name = "";
                                            if (formData.containsKey("First_name") || formData.containsKey("Last_name")) {
                                                full_name = formData.get("First_name") + " " + formData.get("Last_name");
                                            } else {
                                                full_name = Account.accountData.get("username");
                                            }
                                            Account.accountData.put("full_name", full_name.trim());

                                            SwipeMenu.menuData.get(SwipeMenu.loginIndex).put("name", Account.accountData.get("full_name"));
                                            SwipeMenu.adapter.notifyDataSetChanged();

                                            /* update account email address in cache */
                                            String new_email = email_field.getText().toString();
                                            if (!Account.accountData.get("mail").equals(new_email) && !new_email.isEmpty()) {
                                                Account.accountData.put("mail", new_email);
                                            }

                                            TextView type_name = (TextView) AccountArea.profileTab.findViewById(R.id.type_name);
                                            TextView type_namecharity = (TextView) CharityArea.profileTab.findViewById(R.id.type_name);

                                            type_name.setText(Config.cacheAccountTypes.get(Account.accountData.get("type")).get("name"));
                                            type_namecharity.setText(Config.cacheAccountTypes.get(Account.accountData.get("type")).get("name"));

                                            /* show toast and finish activity */
                                            Toast.makeText(Config.context, Lang.get("profile_updated"), Toast.LENGTH_LONG).show();
                                            instance.finish();
                                        } else {
                                            Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), instance);
                                        }
                                    }
                                }

                            } catch (UnsupportedEncodingException e1) {

                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                        }
                    });
                }
            }
        });

        /* set timeout */
        CountDownTimer timer = new CountDownTimer(Config.ACTIONBAR_TIMEOUT, Config.ACTIONBAR_TIMEOUT) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                actionbarSpinner.setVisible(false);
            }
        };
        timer.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case AddListingActivity.RESULT_ACCEPT:
                if (resultCode == RESULT_OK) {
                    Forms.confirmAccept(data.getStringExtra("key"));
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.multilingual, menu);

        Utils.translateMenuItems(menu);

        MenuItem spinnerItem = menu.findItem(R.id.multilingual);
        Spinner spinner = (Spinner) spinnerItem.getActionView();

        actionbarSpinner = spinnerItem;

        ActionbarIconSpinnerAdapter adapter = new ActionbarIconSpinnerAdapter(instance, account_fields_area, Config.cacheLanguagesWebs, spinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(adapter);
        adapter.selectDefault();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }
}