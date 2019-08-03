package com.acentria.benslist.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.adapters.CharityDonateAdapter;
import com.acentria.benslist.response.DonateCharityResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DonateFoodFrag extends Fragment {
    private final String TAG = "DonateFoodFrag=>";
    RecyclerView rv_donate_charity;
    private ProgressDialog progressDialog;
    private List<DonateCharityResponse> list = new ArrayList<>();
    private CharityDonateAdapter adatper;
    private TextView tv_no_records;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(TAG, "onActivityCreated");
        if (Utils.isOnline(getActivity())) {
            call_donatefoodApi();
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.network_connection_error), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_food_donate, container, false);
        initializeUI(view);

        Log.e(TAG, "view ");
        return view;
    }

    private void initializeUI(View view) {
        rv_donate_charity = view.findViewById(R.id.rv_donate_charity);
        tv_no_records = view.findViewById(R.id.tv_no_records);
        /*progress dialog inilisize*/
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Loading...");


    }

    /*call donate food */
    private void call_donatefoodApi() {

        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder().url("https://www.benslist.com/Api/food_donate_list.inc.php").build();
        progressDialog.show();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                progressDialog.dismiss();
                tv_no_records.setVisibility(View.VISIBLE);
                rv_donate_charity.setVisibility(View.GONE);
                Log.e(TAG, "onfailure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    progressDialog.dismiss();
                    String json_response = response.body().string();
                    Log.e(TAG, "onResponse: \n" + json_response);

                    if (!json_response.equalsIgnoreCase("[]")) {
                        try {
                            JSONArray itemArray = new JSONArray(json_response);
                            Type type = new TypeToken<List<DonateCharityResponse>>() {
                            }.getType();
                            list = (new Gson()).fromJson(itemArray.toString(), type);
                            for (int i = 0; i < list.size(); i++) {
                                Log.e(TAG, "posted_date " + list.get(i).getPostedDate() + "\t as posit" + i);

                            }

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    rv_donate_charity.setVisibility(View.VISIBLE);
                                    tv_no_records.setVisibility(View.GONE);
                                    rv_donate_charity.setLayoutManager(new LinearLayoutManager(getActivity()));
                                    adatper = new CharityDonateAdapter(list, getActivity(),true);
                                    rv_donate_charity.setAdapter(adatper);
                                    adatper.notifyDataSetChanged();
                                }
                            });


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    } else {
                        progressDialog.dismiss();
                        tv_no_records.setVisibility(View.VISIBLE);
                        rv_donate_charity.setVisibility(View.GONE);
                        Log.e(TAG, "Arrary blank get");
                    }


                } else {
                    progressDialog.dismiss();
                    tv_no_records.setVisibility(View.VISIBLE);
                    rv_donate_charity.setVisibility(View.GONE);
                    Log.e(TAG, "Unsucess Response");
                }

            }
        });

    }
}
