package com.acentria.benslist.controllers;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.response.DonateCharityResponse;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CharityDetailActivity extends AppCompatActivity implements View.OnClickListener {

    private final String EXTRA_IMAGE = "Lakshman";
    private String TAG = "CharityDetailActivity=> ";
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appbar;
    private ImageView iv_bgimage, iv_img;
    private TextView tv_refrence_no, tv_orgname, tv_psoted, tv_email, tv_contactno, tv_description, tv_others, tv_addinalinfo, tv_country, tv_state, tv_city;
    private TextView tv_total_title, tv_remaingtitle;
    private Button btn_donateknow;
    private LinearLayout ll_total, ll_remainingl;
    private boolean appBarExpanded = true;
    private Toolbar animToolbar;
    private String ref_no, imglink = "", price;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("CharityDetailActivity");
        setContentView(R.layout.activity_charity_detail_layout);
        setweigts();

    }


    /*totla weights typacast or insiliation*/
    private void setweigts() {

        tv_refrence_no = findViewById(R.id.tv_refrence_no);
        tv_orgname = findViewById(R.id.tv_orgname);
        tv_psoted = findViewById(R.id.tv_psoted);
        tv_email = findViewById(R.id.tv_email);
        tv_contactno = findViewById(R.id.tv_contactno);
        tv_description = findViewById(R.id.tv_description);
        tv_others = findViewById(R.id.tv_others);
        tv_addinalinfo = findViewById(R.id.tv_addinalinfo);
        tv_country = findViewById(R.id.tv_country);
        tv_state = findViewById(R.id.tv_state);
        tv_city = findViewById(R.id.tv_city);
        iv_img = findViewById(R.id.iv_img);
        iv_bgimage = findViewById(R.id.iv_bgimage);


        tv_total_title = findViewById(R.id.tv_total_title);
        tv_remaingtitle = findViewById(R.id.tv_remaingtitle);
        ll_total = findViewById(R.id.ll_total);
        ll_remainingl = findViewById(R.id.ll_remaining);
        btn_donateknow = findViewById(R.id.btn_donateknow);
        btn_donateknow.setOnClickListener(this);

        animToolbar = findViewById(R.id.anim_toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        appbar = findViewById(R.id.appbar);
        ActionBar actionBar = getSupportActionBar();
        ((ActionBar) actionBar).setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeButtonEnabled(true);

//        setSupportActionBar(animToolbar);
        if (getIntent().getExtras() != null) {

            ref_no = getIntent().getStringExtra("Ref_no");
            imglink = getIntent().getStringExtra("ImgLink");
            Log.e(TAG, "imgLink " + imglink);
            Glide.with(this).load(imglink).placeholder(R.mipmap.seller_no_photo).diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate().into(iv_bgimage);
            Glide.with(this).load(imglink).placeholder(R.mipmap.seller_no_photo).diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate().into(iv_img);
            price = getIntent().getStringExtra("ammount");
            Log.e(TAG, "refno" + ref_no);
            if (getIntent().getStringExtra("is_foodside") != null) {

                setTitle("Food Charity Detail");
            }

        }



        /*colApsing toolbar*/
        appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) > 200) {
                    appBarExpanded = false;
                } else {
                    appBarExpanded = true;
                }
                invalidateOptionsMenu();
            }
        });


        collapsingToolbar.setTitle("CharityDetail");
        collapsingToolbar.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));
//        collapsingToolbar.getStatusBarScrim().applyTheme(R.style.TextAppearance_AppCompat);
        collapsingToolbar.getScrimVisibleHeightTrigger();
        ViewCompat.setTransitionName(findViewById(R.id.appbar), EXTRA_IMAGE);
        /*progress dialog inilisize*/
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Loading...");
        if (Utils.isOnline(this)) {
            callApi();
        } else {
            Toast.makeText(this, getResources().getString(R.string.network_connection_error), Toast.LENGTH_LONG).show();
        }


    }

    private void callApi() {
        if (ref_no == null) {
            return;
        }

        OkHttpClient okHttpClientforcity = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("ref_number", ref_no)
                .build();

        final Request request = new Request.Builder()
                /* .url(BASE_URL + route)*/
                .url("https://www.benslist.com/Api/charity_details.inc.php")
                .post(requestBody)
                .build();
        progressDialog.show();
        okHttpClientforcity.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                progressDialog.dismiss();
                Log.e(TAG, "failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String mresponse = response.body().string();
                    Log.e(TAG, "Resonse=> " + mresponse);
                    if (!mresponse.equalsIgnoreCase("[]")) try {
                        JSONArray itemArray = new JSONArray(mresponse);


                        final JSONObject jsonObject = itemArray.getJSONObject(0);
                        Type type = new TypeToken<List<DonateCharityResponse>>() {
                        }.getType();
                        final List<DonateCharityResponse> list = (new Gson()).fromJson(itemArray.toString(), type);
                        Log.e(TAG, "jsonObject pos 0" + jsonObject);
                        Log.e(TAG, jsonObject.getString("title"));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                setweightsdatafromdynamically(list);
                            }
                        });


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    else {
                        Log.e(TAG, "reponse [] arary return" + mresponse);
                    }
                } else {
                    progressDialog.dismiss();
                    Log.e(TAG, "Unsucess");
                }
            }
        });


    }

    private void setweightsdatafromdynamically(List<DonateCharityResponse> list) {

        if (list.get(0).getAmount().equalsIgnoreCase("0")) {
            tv_total_title.setText("Total Goods");
            tv_remaingtitle.setText("Remaining Goods");
        } else {
            tv_total_title.setText("Total Amount");
            tv_remaingtitle.setText("Remaining Amount");
        }
        for (int i = 0; i < list.size(); i++) {

            LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            TextView tv_total = new TextView(this);
            TextView tv_remaining = new TextView(this);
//            tv_total.setTypeface(null, Typeface.BOLD);
            tv_total.setTextSize(16);
            tv_total.setTextColor(Color.BLACK);
//            tv_remaining.setTypeface(null, Typeface.BOLD);
            tv_remaining.setTextSize(16);
            tv_remaining.setTextColor(Color.BLACK);
            tv_total.setLayoutParams(lparams);
            tv_remaining.setLayoutParams(lparams);
            if (list.get(i).getAmount().equalsIgnoreCase("0")) {
                tv_total.setText(list.get(i).getProduct() + " : " + list.get(i).getQuantity());
                tv_remaining.setText(list.get(i).getProduct() + " : " + list.get(i).getQuantityRemain());
            } else {
                tv_total.setText("$" + list.get(i).getAmount());
                tv_remaining.setText("$" + list.get(i).getRemainingAmount());
                tv_total.setTypeface(null, Typeface.BOLD);
                tv_remaining.setTypeface(null, Typeface.BOLD);
                tv_total.setTextColor(getResources().getColor(R.color.about_app_link_color));
                tv_remaining.setTextColor(getResources().getColor(R.color.about_app_link_color));
            }
            this.ll_total.addView(tv_total);
            this.ll_remainingl.addView(tv_remaining);
            Log.e(TAG, "list" + list.get(i).getImage());
        }
//        tv_email.setText(jsonObject.getString("email"));
//        imglink = "";

        imglink = "https://www.benslist.com/files/" + list.get(0).getImage();
        Glide.with(this).load(imglink).placeholder(R.mipmap.seller_no_photo).diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate().into(iv_img);
        tv_refrence_no.setText(list.get(0).getRefNumber());
        tv_orgname.setText(list.get(0).getNameOfOrganization());
        tv_psoted.setText(list.get(0).getPostedDate());
        tv_email.setText(list.get(0).getEmail());
        tv_contactno.setText(list.get(0).getTel());
        tv_description.setText(list.get(0).getDescription());
        tv_country.setText(list.get(0).getCountry());
        tv_state.setText(list.get(0).getState());
        tv_city.setText(list.get(0).getCity());


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    /*donate charity to submit call api*/
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_donateknow:
                if(getIntent().getStringExtra("is_foodside")!=null){
                    startActivity(new Intent(this, CharityDonateActivity.class).putExtra("Ref_no", ref_no).putExtra("is_foodside",getIntent().getStringExtra("is_foodside")));
                }else {
                    startActivity(new Intent(this, CharityDonateActivity.class).putExtra("Ref_no", ref_no));

                }
                break;
        }

    }
}
