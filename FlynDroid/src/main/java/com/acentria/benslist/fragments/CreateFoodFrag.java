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
import android.support.annotation.Nullable;
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
import android.widget.Spinner;
import android.widget.Toast;

import com.acentria.benslist.Account;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.response.Country;
import com.acentria.benslist.response.State;
import com.acentria.benslist.response.City;
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

public class CreateFoodFrag extends Fragment implements View.OnClickListener {

    private Context context;
    private String TAG = "CreateFoodFrag=> ";
    private EditText et_title, et_name_organization, et_description, et_product, et_qauntity, et_email, et_telephone, et_address_des;
    private Button btn_submit, btn_addmore;
    private ImageView iv_profile;
    private Spinner spinner_selectcountry, spinner_select_state, spinner_select_city;
    private ArrayList<Country> countries = new ArrayList<>();
    private ArrayList<State> states = new ArrayList<>();
    private ArrayList<City> cities = new ArrayList<>();
    private ArrayAdapter<Country> countryArrayAdapter;
    private ArrayAdapter<State> stateArrayAdapter;
    private ArrayAdapter<City> cityArrayAdapter;
    private ProgressDialog progressDialog;
    private String title = "", name_of_organization = "", description = "", email = "", tel = "", product = "", qauntity = "", address = "", additional_info = "", country_str = "", state_str = "", city_str = "";
    private static final int RequestPermissionCode = 1, SELECT_FILE = 2;
    private File file;
    private String profile = "";
    private byte[] image_profileByte = null;
    private String user_login_id = Account.accountData.get("id");

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(TAG, "onActivityCreated");
        if (Utils.isOnline(getActivity())) {
            callCountryListApi();
        } else {

            Toast.makeText(getActivity(), getResources().getString(R.string.network_connection_error), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_layout_create_food, container, false);
        context = container.getContext();
        EnableRunTimePermission();
        initializeUI(view, context);
        Log.e(TAG, "view ");
        return view;
    }

    private void initializeUI(View view, Context context) {
        /*Edit text typecast to initilize*/
        et_title = view.findViewById(R.id.et_title);
        et_name_organization = view.findViewById(R.id.et_name_organization);
        et_description = view.findViewById(R.id.et_description);
        et_product = view.findViewById(R.id.et_product);
        et_qauntity = view.findViewById(R.id.et_qauntity);
        et_email = view.findViewById(R.id.et_email);
        et_telephone = view.findViewById(R.id.et_telephone);
        et_address_des = view.findViewById(R.id.et_address_des);
        iv_profile = view.findViewById(R.id.iv_profile);

        /*onclick on button weights*/
        btn_submit = (Button) view.findViewById(R.id.btn_submit);
        btn_addmore = (Button) view.findViewById(R.id.btn_addmore);
        btn_submit.setOnClickListener(this);
        btn_addmore.setOnClickListener(this);
        iv_profile.setOnClickListener(this);


        /*Spinner typecast to initilize*/
        spinner_selectcountry = view.findViewById(R.id.spinner_selectcountry);
        spinner_select_state = view.findViewById(R.id.spinner_select_state);
        spinner_select_city = view.findViewById(R.id.spinner_select_city);
        /*onItemsleectedListner On Spinners*/
        spinner_selectcountry.setOnItemSelectedListener(country_listener);
        spinner_select_state.setOnItemSelectedListener(state_listener);
        spinner_select_city.setOnItemSelectedListener(city_listener);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Loading...");
        hideSoftKeyboard();

    }

    /*OnAdapterView OnitemsSelectedListner object creates*/
    private AdapterView.OnItemSelectedListener country_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long llong) {
            if (position > 0) {
                Country country = (Country) spinner_selectcountry.getItemAtPosition(position);
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

        }
    };
    private AdapterView.OnItemSelectedListener state_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long llong) {
            if (position > 0) {
                State state = (State) spinner_select_state.getItemAtPosition(position);
                state_str = state.getStateName();
                Log.e("SpinnerSates", "onItemSelected: country: " + state.getStateName() + " " + state.getCountryName());
                if (Utils.isOnline(getActivity())) {
                    call_cityApi(state.getStateName(), state.getCountryName());
                } else {
                    Toast.makeText(getActivity(), "make sure your device connect to internet", Toast.LENGTH_LONG).show();

                }
            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };
    private AdapterView.OnItemSelectedListener city_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long llong) {
            if (position > 0) {
                final City city = (City) spinner_select_city.getItemAtPosition(position);
                city_str = city.getCityName();
                Log.e(TAG, "City OnItemSelectedListener" + "selectcity " + city_str);

            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };


    /*call Country Api for using to set value on spinner*/
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
                                if (countries.get(i).getCountryName().equalsIgnoreCase("")) {
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
                            city_str = cities.get(0).getCityName();
                            cityArrayAdapter = new ArrayAdapter<City>(getActivity(), R.layout.simple_spinner_dropdown_item, cities);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    spinner_select_city.setAdapter(cityArrayAdapter);
                                    EnableRunTimePermission();
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


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_submit:
                validationforfoodcharity();
                break;
            case R.id.btn_addmore:
                break;
            case R.id.iv_profile:
                insetImageDailog();
                break;
        }
    }


    /*validation*/
    private void validationforfoodcharity() {
        if (!isvalidation()) {
            return;
        }/*spinner value*/ else if (country_str.isEmpty()) {
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
                callCreatefoodApi();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.network_connection_error), Toast.LENGTH_LONG).show();
            }
        }


    }

    /*call Create food Api*/
    private void callCreatefoodApi() {
        String profilenamepath = "";

        Log.e(TAG, "callCreatefoodCharityApi");

        if (product.equalsIgnoreCase("")) {
            product = "0";
        }
        if (qauntity.equalsIgnoreCase("")) {
            qauntity = "0";
        }

        OkHttpClient okHttpfoodCharity = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        builder.addFormDataPart("account_id", user_login_id)
                .addFormDataPart("title", title)
                .addFormDataPart("name_of_organization", name_of_organization)
                .addFormDataPart("description", description)
                .addFormDataPart("email", email)
                .addFormDataPart("tel", tel)
                .addFormDataPart("c_type", "F")
                .addFormDataPart("[product]", product)
                .addFormDataPart("[quantity]", qauntity)
                .addFormDataPart("country", country_str)
                .addFormDataPart("state", state_str)
                .addFormDataPart("city", city_str)
                .addFormDataPart("address", address)
                .addFormDataPart("additional_info", additional_info);
        final MediaType MEDIA_TYPE_PNG = null;
        if (image_profileByte != null) {
            profilenamepath = "profile_image" + file.getName();
            Log.e(TAG, "imag builder path=> " + profilenamepath + "\nbyte=>" + image_profileByte);
            builder.addFormDataPart("avtar", profilenamepath, RequestBody.create(MEDIA_TYPE_PNG, image_profileByte));
        }
        Log.e(TAG, "MultipartBody Create =>" + "account_id" + user_login_id + "\ntitle" + title + "\nname_of_organization " + name_of_organization + "\ndescription" + description
                + "\nc_type" + "\nemail " + email + "\ntel" + tel + "\nproduct" + product +
                "\nquantity " + qauntity + "\ncountry " + country_str + "\nstate " + state_str + "\ncity " + city_str + "\naddress " + address + "\npayment_method " + "\npaypal_email " + additional_info + "\navtar " + file.getName() + "soucesname " + profilenamepath);

        RequestBody requestBody = builder.build();
        Request requestfoodcharity = new Request.Builder()
                /* .url(BASE_URL + route)*/
                .url("https://www.benslist.com/Api/charity_create.inc.php")
                .post(requestBody)
                .build();
        progressDialog.show();
        okHttpfoodCharity.newCall(requestfoodcharity).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, " Charity multipart OnFailure " + "onFailure:");
                progressDialog.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                progressDialog.dismiss();
                if (response.isSuccessful()) {

                    String mresponse = response.body().string();
                    Log.e(TAG, "Multipart Response : " + mresponse);
                    if (!mresponse.equalsIgnoreCase("{}")) {
                        try {
                            final JSONObject jsonObject = new JSONObject(mresponse);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    try {
                                        if (jsonObject.getString("result").equalsIgnoreCase("success")) {
//                                            Toast.makeText(getActivity(), "Profile update Successfully", Toast.LENGTH_LONG).show();
                                            Utils.isAlertBox(getActivity(), "Ben's List", "Food Charity Create Successfully");
                                            clearWeights();
                                        }
                                        Log.e(TAG, "response sucess result=> " + jsonObject.getString("result"));

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

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
        et_address_des.setText("");
        et_email.setText("");
        et_telephone.setText("");
        et_product.setText("");
        et_qauntity.setText("");
        et_description.setText("");
    }

    //    boolena validation
    private boolean isvalidation() {
        boolean valid = true;
        title = et_title.getText().toString().trim();
        name_of_organization = et_name_organization.getText().toString().trim();
        description = et_description.getText().toString().trim();
        email = et_email.getText().toString().trim();
        tel = et_telephone.getText().toString().trim();
        product = et_product.getText().toString().trim();
        qauntity = et_qauntity.getText().toString().trim();
        address = et_description.getText().toString().trim();
        additional_info = et_address_des.getText().toString().trim();

        /*for title*/
        if (title.isEmpty()) {
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

        if (address.isEmpty()) {
            et_description.setError(getResources().getString(R.string.add_desc_fields_error));
            valid = false;
//            Toast.makeText(getActivity(), getResources().getString(R.string.add_desc_fields_error), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Address  fields is request " + address);
        } else {
            et_description.setError(null);
        }

        if (additional_info.isEmpty()) {
            et_address_des.setError(getResources().getString(R.string.additonal_info_error));
            valid = false;
//            Toast.makeText(getActivity(), getResources().getString(R.string.add_desc_fields_error), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Address  fields is request " + address);
        } else {
            et_address_des.setError(null);
        }

        return valid;
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

    /*.......................select or upload image Process............................*/
    /*Runtime Permissions*/
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

    /*runtime permissionb*/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permission Gratded, Now your application  access CAMERA" + grantResults.length);
                } else {
                    Log.e(TAG, "Permission Canceled, Now your application cannot access CAMERA");
                    Toast.makeText(getActivity(), "Permission Canceled, Now your application cannot access CAMERA.", Toast.LENGTH_LONG).show();

                }

                break;
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

    /*set profile and get image resouce path and url */
    private void setProfilePic(int resultCode, Intent data) {
        Bitmap bm = ImagePicker.getImageFromResult(context, resultCode, data);
        CropImage.activity(UiHelper.getImageUri(context, bm)).start(context, this);
        Log.e(TAG, "setProfilePic=> " + resultCode + "\tdata=> " + data + "");
    }


}
