package com.acentria.benslist.fragments;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.acentria.benslist.Account;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.response.City;
import com.acentria.benslist.response.Country;
import com.acentria.benslist.response.State;
import com.acentria.benslist.util.ImagePicker;
import com.acentria.benslist.util.UiHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.theartofdev.edmodo.cropper.CropImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;

public class CreatCharityFrag extends Fragment implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {

    private View view;
    private RadioGroup radiogroup_money, bank_radiogroup;
    private RadioButton radio_money, radio_others, radio_bank, radio_paypal;
    private LinearLayout ll_othres, ll_money, _ll_hide_others, ll_bank_detail, ll_radiogroup;
    private Button btn_submit, btn_addmore;
    private ImageView iv_profile;
    private int selected_radio_money, selected_radio_bank;

    private EditText et_title, et_name_organization, et_description, et_product, et_qauntity, et_email, et_amount, et_address_des, et_telephone, et_bankname, et_acc_no, et_ifsc_code, et_additionalinfo;
    private Spinner spinner_selectcountry, spinner_select_state, spinner_select_city;
    private ArrayList<Country> countries = new ArrayList<>();
    private ArrayList<State> states = new ArrayList<>();
    private ArrayList<City> cities = new ArrayList<>();

    private ArrayAdapter<Country> countryArrayAdapter;
    private ArrayAdapter<State> stateArrayAdapter;
    private ArrayAdapter<City> cityArrayAdapter;

    private Context context;
    private ProgressDialog progressDialog;
    private String user_login_id = Account.accountData.get("id"), c_type_str = "", payment_method = "";

    private String title = "", name_of_organization = "", description = "", amount = "", remaining_amount = "", email = "", tel = "", product = "", qauntity = "",
            address = "", paypal_id = "", bank_name = "", account_no = "", ifsc_code = "", additional_info = "", country_str = "", state_str = "", city_str = "";
    private static final int RequestPermissionCode = 1;
    private static final int SELECT_FILE = 2;

    private File file;
    private String profile = "";
    private byte[] image_profileByte = null;
    private String TAG = "CreatCharityFrag=>";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (Utils.isOnline(getActivity())) {
            callCountryListApi();
        } else {

            Toast.makeText(getActivity(), getResources().getString(R.string.network_connection_error), Toast.LENGTH_LONG).show();
        }

    }


    @Override
    public void onDestroy() {
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_layout_create_charity, container, false);
            EnableRunTimePermission();
            Log.e(TAG, "null view ");
            context = container.getContext();
            initializeUI(view, context);

        } else {
            ((ViewGroup) view.getParent()).removeView(view);
            Log.e(TAG, "reomove view");
        }

        return view;
    }

    private void initializeUI(View view, Context context) {
        iv_profile = view.findViewById(R.id.iv_profile);
        radiogroup_money = view.findViewById(R.id.radiogroup_money);
        bank_radiogroup = view.findViewById(R.id.bank_radiogroup);
        ll_othres = view.findViewById(R.id.ll_othres);
        ll_money = view.findViewById(R.id.ll_money);
        ll_radiogroup = view.findViewById(R.id.ll_radiogroup);
        _ll_hide_others = view.findViewById(R.id._ll_hide_others);
        ll_bank_detail = view.findViewById(R.id.ll_bank_detail);

        radio_money = view.findViewById(R.id.radio_money);
        radio_others = view.findViewById(R.id.radio_others);
        radio_bank = view.findViewById(R.id.radio_bank);
        radio_paypal = view.findViewById(R.id.radio_paypal);

        et_bankname = view.findViewById(R.id.et_bankname);
        et_acc_no = view.findViewById(R.id.et_acc_no);
        et_ifsc_code = view.findViewById(R.id.et_ifsc_code);
        et_additionalinfo = view.findViewById(R.id.et_additionalinfo);
        et_title = view.findViewById(R.id.et_title);
        et_name_organization = view.findViewById(R.id.et_name_organization);
        et_description = view.findViewById(R.id.et_description);
        et_product = view.findViewById(R.id.et_product);
        et_qauntity = view.findViewById(R.id.et_qauntity);
        et_email = view.findViewById(R.id.et_email);
        et_amount = view.findViewById(R.id.et_amount);
        et_address_des = view.findViewById(R.id.et_address_des);
        et_telephone = view.findViewById(R.id.et_telephone);


        btn_submit = (Button) view.findViewById(R.id.btn_submit);
        btn_submit.setOnClickListener(this);
        radiogroup_money.setOnCheckedChangeListener(this);
        bank_radiogroup.setOnCheckedChangeListener(this);
        iv_profile.setOnClickListener(this);
//        Utils.hideKeyboard(ll_bank_detail);
        hideSoftKeyboard();

        spinner_selectcountry = (Spinner) view.findViewById(R.id.spinner_selectcountry);
        spinner_select_state = (Spinner) view.findViewById(R.id.spinner_select_state);
        spinner_select_city = (Spinner) view.findViewById(R.id.spinner_select_city);

//        createLists();
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Loading...");

        /*................spinner Started*/

        spinner_selectcountry.setOnItemSelectedListener(country_listener);
        spinner_select_state.setOnItemSelectedListener(state_listener);
        spinner_select_city.setOnItemSelectedListener(city_listener);

        /*radio button get runtime state*/
        if (radio_money.isChecked()) {
            c_type_str = "M";
            Log.e(TAG, "radio_moneycheck " + c_type_str);
        } else if (radio_others.isChecked()) {
            c_type_str = "O";
            Log.e(TAG, "radio_otherscheck " + c_type_str);
        }

        if (radio_bank.isChecked()) {
            payment_method = "B";
            Log.e(TAG, "radio_bankscheck " + payment_method);
        } else if (radio_paypal.isChecked()) {
            payment_method = "P";
            Log.e(TAG, "radio_paypalscheck " + payment_method);
        }

    }


    /*Spinners Adtapter onItems Click object creaeted of country,state and city*/
    private AdapterView.OnItemSelectedListener country_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
            if (position > 0) {
                final Country country = (Country) spinner_selectcountry.getItemAtPosition(position);
                Log.e("SpinnerCountry", "onItemSelected: country: " + /*country.getCountryID() +*/ " " + country.getCountryName());
                country_str = country.getCountryName();
                if (Utils.isOnline(getActivity())) {
                    call_StateApi(country.getCountryName());
                } else {
                    Toast.makeText(getActivity(), "Make sure your device connect to internet", Toast.LENGTH_LONG).show();
                }

            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            Log.e(TAG, "country_listener " + "onNothingSelected");
        }
    };
    private AdapterView.OnItemSelectedListener state_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
            if (position > 0) {
                final State state = (State) spinner_select_state.getItemAtPosition(position);
                Log.e("SpinnerCountry", "onItemSelected: state: " + state.getCountryName() + " " + state.getStateName());
                state_str = state.getStateName();
                if (Utils.isOnline(getActivity())) {
                    call_cityApi(state.getStateName(), state.getCountryName());
                } else {
                    Toast.makeText(getActivity(), "make sure your device connect to internet", Toast.LENGTH_LONG).show();

                }


            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            Log.e(TAG, "state_listener " + "onNothingSelected");
        }
    };
    private AdapterView.OnItemSelectedListener city_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
            if (position > 0) {
                final City city = (City) spinner_select_city.getItemAtPosition(position);
                Log.e("SpinnerCity", "onItemSelected: state: " + /*city.getCityID() +*/ " " + city.getCityName());
                city_str = city.getCityName();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            Log.e(TAG, "city_listener " + "onNothingSelected");
        }
    };


    /**/
    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int position) {
        switch (radioGroup.getId()) {
            case R.id.radiogroup_money:
                selected_radio_money = radiogroup_money.getCheckedRadioButtonId();
                if (selected_radio_money != -1) {
                    final RadioButton radioButton_monty = view.findViewById(selected_radio_money);
                    if (radioButton_monty.getText().toString().equalsIgnoreCase("Money")) {
                        ll_money.setVisibility(View.VISIBLE);
                        ll_othres.setVisibility(View.GONE);
                        _ll_hide_others.setVisibility(View.VISIBLE);
                        radioButton_monty.setChecked(true);
                        radio_others.setChecked(false);

                        Log.e(TAG, "select money " + radioButton_monty.getText().toString());
                        c_type_str = "M";
                    } else {

                        ll_othres.setVisibility(View.VISIBLE);
                        _ll_hide_others.setVisibility(View.GONE);
                        ll_money.setVisibility(View.GONE);
                        radioButton_monty.setChecked(false);
                        radio_others.setChecked(true);
                        Log.e(TAG, "select other " + radioButton_monty.getText().toString());
                        c_type_str = "O";
                    }

                } else {

                    Log.e(TAG, "select other " + "Nothing selected from Money Radio Group.");
                }


                break;
            case R.id.bank_radiogroup:
                selected_radio_bank = bank_radiogroup.getCheckedRadioButtonId();
                if (selected_radio_bank != -1) {
                    final RadioButton radioButton_bank = view.findViewById(selected_radio_bank);
                    if (radioButton_bank.getText().toString().equalsIgnoreCase("Bank")) {
                        Log.e(TAG, "select bank " + radioButton_bank.getText().toString());

                        payment_method = "B";
                        ll_bank_detail.setVisibility(View.VISIBLE);
                        et_bankname.setHint("Bank Name");
                        et_acc_no.setVisibility(View.VISIBLE);
                        et_ifsc_code.setVisibility(View.VISIBLE);
                        et_additionalinfo.setVisibility(View.VISIBLE);
                        radioButton_bank.setChecked(true);
                        radio_paypal.setChecked(false);

                    } else {
                        Log.e(TAG, "select Paypal " + radioButton_bank.getText().toString());
                        payment_method = "P";
                        et_bankname.setHint("Paypal ID");
                        et_acc_no.setVisibility(View.GONE);
                        et_ifsc_code.setVisibility(View.GONE);
                        et_additionalinfo.setVisibility(View.GONE);
                        radioButton_bank.setChecked(false);
                        radio_paypal.setChecked(true);

                    }

                } else {
                    Log.e(TAG, "select money " + "Nothing selected from Bank Radio Group.");
                }

                break;
        }


    }


    /*onclickView*/
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_money:
                break;
            case R.id.ll_othres:
                break;
            case R.id.btn_submit:
                validationforcharity();
                break;
            case R.id.iv_profile:
                insetImageDailog();
                break;
        }
    }


    /*call Country Api and set date on country spinner */
    private void callCountryListApi() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url("https://www.benslist.com/Api/get_location.inc.php").build();
        progressDialog.show();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure:");
                Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json_response = response.body().string();
                    Log.e(TAG, "onResponse: \n" + json_response);

                    if (!json_response.equalsIgnoreCase("[]")) {
                        try {
                            JSONArray itemArray = new JSONArray(json_response);


                            Type type = new TypeToken<ArrayList<Country>>() {
                            }.getType();
                            countries = (new Gson()).fromJson(itemArray.toString(), type);
                            for (int i = 0; i < countries.size(); i++) {
                                if (countries.get(i).getCountryName() != null && countries.get(i).getCountryName().equalsIgnoreCase("")) {
                                    countries.get(i).setCountryName("Please Select Country");
                                }
                            }
                            Log.e(TAG, "FilerCountryArrayBefor pass in spiner adapter " + countries.get(0).getCountryName());
                            countryArrayAdapter = new ArrayAdapter<Country>(getActivity(), R.layout.simple_spinner_dropdown_item, countries);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    spinner_selectcountry.setAdapter(countryArrayAdapter);
                                }
                            });


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        progressDialog.dismiss();
                        Log.e(TAG, "Country response error and response [] blank get ");
                    }
                } else {
                    progressDialog.dismiss();
                    Log.e(TAG, "Country response unscuess fully ");
                }


            }
        });
    }

    /*call state Api and set data on  state spinner*/
    private void call_StateApi(final String countryName) {


        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("countryname", countryName)
                .build();

        Request request = new Request.Builder()
                /* .url(BASE_URL + route)*/
                .url("https://www.benslist.com/Api/" + "get_location.inc.php")
                .post(requestBody)
                .build();
        progressDialog.show();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "State OnFailure " + "onFailure:");
//                Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json_response = response.body().string();
                    Log.e(TAG, "onResponse: \n" + json_response);

                    if (!json_response.equalsIgnoreCase("[]")) {
                        try {

                            JSONArray itemArray = new JSONArray(json_response);
                            Type type = new TypeToken<ArrayList<State>>() {
                            }.getType();
                            states = (new Gson()).fromJson(itemArray.toString(), type);

                            for (int cAs = 0; cAs < states.size(); cAs++) {
                                states.get(cAs).setCountryName(countryName);
                                Log.e(TAG, "Implement country in State pojo " + states.get(cAs).getCountryName());

                            }

                            state_str = states.get(0).getStateName();
                            Log.e(TAG, "FilerStateArray Befor pass in spiner adapter " + states.get(0).getCountryName() + "\nState name" + state_str);
                            stateArrayAdapter = new ArrayAdapter<State>(getActivity(), R.layout.simple_spinner_dropdown_item, states);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    spinner_select_state.setAdapter(stateArrayAdapter);
                                    /*call state api for for first set spinner state wise set city befor selected onItemsselect lisners*/
                                    call_cityApi(states.get(0).getStateName(), states.get(0).getCountryName());

                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        progressDialog.dismiss();
                        Log.e(TAG, "State return [] arrary");
                    }

                } else {
                    progressDialog.dismiss();
                    Log.e(TAG, "State Unsuccess  and return [] arrary");
                }

            }
        });


    }

    /*call city Api and set date on city spinner */
    private void call_cityApi(String stateName, String countryName) {


        OkHttpClient okHttpClientforcity = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("countryname", countryName)
                .addFormDataPart("statename", stateName)
                .build();

        Request requestforcity = new Request.Builder()
                /* .url(BASE_URL + route)*/
                .url("https://www.benslist.com/Api/get_location.inc.php")
                .post(requestBody)
                .build();
        progressDialog.show();
        okHttpClientforcity.newCall(requestforcity).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                progressDialog.dismiss();
                Log.e(TAG, "city onFailure ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        Log.e(TAG, "city successfully ");

                        String json_response = response.body().string();
                        Log.e(TAG, "onResponse: \n" + json_response);
                        if (!json_response.equalsIgnoreCase("[]")) {
                            JSONArray cityitemArray = new JSONArray(json_response);
                            Type type = new TypeToken<ArrayList<City>>() {
                            }.getType();
                            cities = (new Gson()).fromJson(cityitemArray.toString(), type);
//                            for (int i = 0; i < cities.size(); i++) {
//                                cities.get(i).getCityName();
//                                Log.e(TAG, "cityName " + cities.get(i).getCityName());
////                            cities.add(0,"Please Select City");
//                            }
                            city_str = cities.get(0).getCityName();
                            cityArrayAdapter = new ArrayAdapter<City>(getActivity(), R.layout.simple_spinner_dropdown_item, cities);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    spinner_select_city.setAdapter(cityArrayAdapter);

                                }
                            });


                        } else {
                            progressDialog.dismiss();
                            Log.e(TAG, "City Unsuccess  and return [] arrary");
                        }


                    } else {
                        progressDialog.dismiss();
                        Log.e(TAG, "City Unsuccess ");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


    }

    private void validationforcharity() {
        if (!isvalidationbefor_createCharity()) {
            return;
        } /*spinner value*/ else if (country_str.isEmpty()) {
            Toast.makeText(getActivity(), "please select country.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "country empty fields is request " + country_str);
        } else if (country_str.equalsIgnoreCase("Please Select Country")) {
            Toast.makeText(getActivity(), "please select country.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "country fields is request " + country_str);
        } else if (state_str.isEmpty()) {
            Toast.makeText(getActivity(), "please select state.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "state fields is request " + state_str);
        } else if (city_str.isEmpty()) {
            Toast.makeText(getActivity(), "please select city.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "city fields is request " + city_str);
        } else {
            if (Utils.isOnline(getActivity())) {
                callCreateCharityApi();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.network_connection_error), Toast.LENGTH_LONG).show();
            }
        }

    }

    private boolean isvalidationbefor_createCharity() {
        boolean valid = true;
        title = et_title.getText().toString().trim();
        name_of_organization = et_name_organization.getText().toString().trim();
        description = et_description.getText().toString().trim();
        /*c_type*/
        amount = et_amount.getText().toString().trim();
        remaining_amount = amount;
        email = et_email.getText().toString().trim();
        tel = et_telephone.getText().toString().trim();
        product = et_product.getText().toString().trim();
        qauntity = et_qauntity.getText().toString().trim();
        address = et_address_des.getText().toString().trim();

        paypal_id = et_bankname.getText().toString().trim();/*manage on single edit text*/
        bank_name = et_bankname.getText().toString().trim();
        account_no = et_acc_no.getText().toString().trim();
        ifsc_code = et_ifsc_code.getText().toString().trim();
        additional_info = et_additionalinfo.getText().toString().trim(); /*manage on single edit text*/
        file = new File("url");
        /*file path not null */

        /*for title*/
        if (title.isEmpty()) {
//            Toast.makeText(getActivity(), getResources().getString(R.string.title_error), Toast.LENGTH_LONG).show();
            et_title.setError(getResources().getString(R.string.title_error));
            valid = false;
            Log.e(TAG, "title fields is request " + title);
        } else {
            et_title.setError(null);
        }

        if (name_of_organization.isEmpty()) {
//            Toast.makeText(getActivity(), "The \"Name of organization\" field is required; please fill in it.", Toast.LENGTH_LONG).show();
            et_name_organization.setError(getResources().getString(R.string.nameof_org_error));
            valid = false;
            Log.e(TAG, "name_of_organization fields is request " + name_of_organization);
        } else {
            et_name_organization.setError(null);
        }


        if (description.isEmpty()) {
//            Toast.makeText(getActivity(), getResources().getString(R.string.description_error), Toast.LENGTH_LONG).show();
            et_description.setError(getResources().getString(R.string.description_error));
            valid = false;
            Log.e(TAG, "Description fields is request " + description);
        } else {
            et_description.setError(null);
        }

        /*if money radio group is select than got these condiition  eles got on others condion */
        if (ll_money.getVisibility() == View.VISIBLE) {
            if (amount.isEmpty()) {
//                Toast.makeText(getActivity(), getResources().getString(R.string.ammout_filed_error), Toast.LENGTH_LONG).show();
                et_amount.setError(getResources().getString(R.string.ammout_filed_error));
                valid = false;
                Log.e(TAG, "Ammount fields is request " + amount);
            } else {
                et_amount.setError(null);
            }
            if (email.isEmpty()) {
                et_email.setError(getResources().getString(R.string.email_empty));
                valid = false;
//                Toast.makeText(getActivity(), getResources().getString(R.string.email_empty), Toast.LENGTH_LONG).show();
                Log.e(TAG, "email fields is request " + email);
            } else if (!Utils.checkEmail(email)) {
                et_email.setError(getResources().getString(R.string.valid_email_error));
                valid = false;
//                 Toast.makeText(getActivity(), getResources().getString(R.string.valid_email_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "email not registred");
            } else {
                et_email.setError(null);
            }
            if (tel.isEmpty()) {
                et_telephone.setError(getResources().getString(R.string.teli_empty));
                valid = false;
//                Toast.makeText(getActivity(), getResources().getString(R.string.teli_empty), Toast.LENGTH_LONG).show();
                Log.e(TAG, "teliphone not requried" + tel);
//            Please enter your phone number.
            } else if (tel.length() < 7) {
                et_telephone.setError(getResources().getString(R.string.tel_minilength_error));
                valid = false;
//                Toast.makeText(getActivity(), getResources().getString(R.string.tel_minilength_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "teliphone langth error" + tel);
            } else {
                et_telephone.setError(null);
                Log.e(TAG, "tel match");
            }
        }
        /*for other visible */
        if (ll_othres.getVisibility() == View.VISIBLE) {
            ll_money.setVisibility(View.GONE);
            ll_othres.setVisibility(View.VISIBLE);
            Log.e(TAG, "Loop in c_type condion Others " + c_type_str);
            /*other conntent visible */
            if (product.isEmpty()) {
//                Toast.makeText(getActivity(), getResources().getString(R.string.product_filed_error), Toast.LENGTH_LONG).show();
                et_product.setError(getResources().getString(R.string.product_filed_error));
                valid = false;
                Log.e(TAG, "product fields is request " + product);
            } else {
                et_product.setError(null);
            }

            if (qauntity.isEmpty()) {
//                Toast.makeText(getActivity(), R.string.qauntity_fileld_error, Toast.LENGTH_LONG).show();
                et_qauntity.setError(getResources().getString(R.string.qauntity_fileld_error));
                valid = false;
                Log.e(TAG, "Quantity fields is request " + qauntity);
            } else {
                et_qauntity.setError(null);
                Log.e(TAG, "Quantity valid " + qauntity);
            }
        }


        if (address.isEmpty()) {
            et_address_des.setError(getResources().getString(R.string.add_desc_fields_error));
            valid = false;
//            Toast.makeText(getActivity(), getResources().getString(R.string.add_desc_fields_error), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Address  fields is request " + address);
        } else {
            et_address_des.setError(null);
        }

        /*radio group of select payment method */
        if (payment_method.equalsIgnoreCase("B")) {
            if (bank_name.isEmpty()) {
                et_bankname.setError(getResources().getString(R.string.bank_field_error));
                valid = false;
//                Toast.makeText(getActivity(),getResources().getString( R.string.bank_field_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "bank name fields is request " + bank_name);
            } else {
                et_bankname.setError(null);
            }
            if (account_no.isEmpty()) {
                et_acc_no.setError(getResources().getString(R.string.account_fields_error));
                valid = false;
//                 Toast.makeText(getActivity(), getResources().getString(R.string.account_fields_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "account name fields is request " + account_no);
            } else {
                et_acc_no.setError(null);
            }
            if (ifsc_code.isEmpty()) {
                et_ifsc_code.setError(getResources().getString(R.string.ifsc_filesds_error));
                valid = false;
//                  Toast.makeText(getActivity(), getResources().getString(R.string.ifsc_filesds_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "IFSC code fields is request " + account_no);
            } else {
                et_ifsc_code.setError(null);
            }
            if (additional_info.isEmpty()) {
                et_additionalinfo.setError(getResources().getString(R.string.additonal_info_error));
                valid = false;
//                Toast.makeText(getActivity(), getResources().getString(R.string.additonal_info_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Additional Info fields is request " + account_no);
            } else {
                et_additionalinfo.setError(null);
            }
            Log.e(TAG, "Loop in payment_method condion Others " + payment_method);
        }
        if (payment_method.equalsIgnoreCase("P")) {
            if (paypal_id.isEmpty()) {
                et_bankname.setError(getResources().getString(R.string.paypal_fileds_error));
                valid = false;
//                Toast.makeText(getActivity(),getResources().getString( R.string.paypal_fileds_error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Pypal id fields is request " + account_no);
            } else {
                et_bankname.setError(null);
            }
            Log.e(TAG, "Loop in payment_method condion Others " + payment_method);
        }

        return valid;
    }

    /*Create Charity submit api */
    private void callCreateCharityApi() {
        String profilenamepath = "";

        Log.e(TAG, "callCreateCharityApi");

        if (product.equalsIgnoreCase("")) {
            product = "0";
        }
        if (qauntity.equalsIgnoreCase("")) {
            qauntity = "0";
        }


        OkHttpClient okHttpClientforCharity = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        builder.addFormDataPart("account_id", user_login_id)
                .addFormDataPart("title", title)
                .addFormDataPart("name_of_organization", name_of_organization)
                .addFormDataPart("description", description)
                .addFormDataPart("c_type", c_type_str)
                .addFormDataPart("amount", amount)
                .addFormDataPart("remaining_amount", remaining_amount)
                .addFormDataPart("email", email)
                .addFormDataPart("tel", tel)
                .addFormDataPart("[product]", product)
                .addFormDataPart("[quantity]", qauntity)
                .addFormDataPart("country", country_str)
                .addFormDataPart("state", state_str)
                .addFormDataPart("city", city_str)
                .addFormDataPart("address", address)
                .addFormDataPart("payment_method", payment_method)
                .addFormDataPart("paypal_email", paypal_id)
                .addFormDataPart("bank_name", bank_name)
                .addFormDataPart("account_no", account_no)
                .addFormDataPart("ifsc_code", ifsc_code)
                .addFormDataPart("additional_info", additional_info);
        final MediaType MEDIA_TYPE_PNG = null;
        if (image_profileByte != null) {
            profilenamepath = "profile_image" + file.getName();
            Log.e(TAG, "imag builder path=> " + profilenamepath + "\nbyte=>" + image_profileByte);
            builder.addFormDataPart("avtar", profilenamepath, RequestBody.create(MEDIA_TYPE_PNG, image_profileByte));
        }

        RequestBody requestBody = builder.build();

        Log.e(TAG, "MultipartBody Create =>" + "account_id" + user_login_id + "\ntitle" + title + "\nname_of_organization " + name_of_organization + "\ndescription" + description
                + "\nc_type" + c_type_str + "\namount " + amount + "\nremaining_amount " + remaining_amount + "\nemail " + email + "\ntel" + tel + "\nproduct" + product +
                "\nquantity " + qauntity + "\ncountry " + country_str + "\nstate " + state_str + "\ncity " + city_str + "\naddress " + address + "\npayment_method " + payment_method +
                "\npaypal_email " + paypal_id + "\nbank_name " + bank_name + "\naccount_no " + account_no + "\nifsc_code " + ifsc_code + "\nadditional_info " + additional_info +
                "\navtar " + file.getName() + "soucesname " + profilenamepath);

        Request requestcharity = new Request.Builder()
                /* .url(BASE_URL + route)*/
                .url("https://www.benslist.com/Api/charity_create.inc.php")
                .post(requestBody)
                .build();
        progressDialog.show();

        okHttpClientforCharity.newCall(requestcharity).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, " Charity multipart OnFailure " + "onFailure:");
//                Toast.makeText(getActivity(), getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    final String success;
                    String mresponse = response.body().string();
                    Log.e(TAG, "Multipart Response : " + mresponse);
                    if (!mresponse.equalsIgnoreCase("{}")) {
                        try {
                            JSONObject jsonObject = new JSONObject(mresponse);
                            success = jsonObject.getString("result");

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    Utils.isAlertBox(getActivity(), "Ben's List", "Charity Create Successfully");
                                    clearWeights();
                                    Log.e(TAG, "response sucess result=> " + success);

                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "charity multipart=>  response error");
                    }
                } else {
                    Log.e(TAG, "Multipat Unsuccess respone");
                }


            }
        });

    }

    private void clearWeights() {
        et_title.setText("");
        et_name_organization.setText("");
        et_description.setText("");
        et_product.setText("");
        et_qauntity.setText("");
        et_email.setText("");
        et_telephone.setText("");
        et_address_des.setText("");
        et_bankname.setText("");
        et_acc_no.setText("");
        et_ifsc_code.setText("");
        et_additionalinfo.setText("");
        et_product.setText("");

    }


    /*Runtime permission check*/
    private void EnableRunTimePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && getActivity().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        } else {
            Log.e(TAG, "Api Level 23 to lower  verson device");
        }
    }

    /*insert image dialog*/
    private void insetImageDailog() {
        Intent chooseImageIntent = ImagePicker.getPickImageIntent(getActivity());
        startActivityForResult(chooseImageIntent, SELECT_FILE);
    }

    /*get result of  to select image and captures images resources */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Log.e(TAG, "RequestCode=> " + requestCode);
            if (requestCode == SELECT_FILE) {
                setProfilePic(resultCode, data);
            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri();
                    try {
                        Bitmap bm = BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(resultUri));
                        Log.e(TAG, "bitmap profile=> " + bm);
                        Log.e(TAG, "resultUri " + resultUri);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

                        image_profileByte = null;
                        image_profileByte = byteArrayOutputStream.toByteArray();
                        iv_profile.setImageBitmap(bm);
                        file = null;
                        file = new File(resultUri.getPath());
                        profile = file.getName();
                        Glide.with(getActivity()).load(resultUri.getPath()).placeholder(R.mipmap.seller_no_photo).diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate().into(iv_profile);
                        Log.e(TAG, "bitmaAfter Compress Image=> " + bm);
                        Log.e(TAG, "byte " + image_profileByte);
                        Log.e(TAG, "IsProfile " + resultUri);
                        Log.e(TAG, "ProfilePath " + profile);


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                    Log.e(TAG, "Exception uri" + error);
                }
            } else {
                Log.e(TAG, "requestCode" + requestCode + " resultCode=>" + requestCode + "Data=> " + data.getData());
            }
        }

    }

    private void setProfilePic(int resultCode, Intent data) {
        Bitmap bm = ImagePicker.getImageFromResult(context, resultCode, data);
        CropImage.activity(UiHelper.getImageUri(context, bm)).start(context, this);
        Log.e(TAG, "setProfilePic=> " + resultCode + "\tdata=> " + data + "");
    }

    /**
     * Hides the soft keyboard
     */
    public void hideSoftKeyboard() {
        if (getActivity().getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }


}
