package com.acentria.benslist.chatprocess;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.response.MarchentListResponse;
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
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MarchentListActivity extends AppCompatActivity implements Marchent_ListAdapter.OnClickPosi {


    private RecyclerView rv_recyclerviw;
    private TextView tv_no_records;
    private ProgressDialog progressDialog;
    private String TAG = MarchentListActivity.class.getName();
    private List<MarchentListResponse> list;
    private boolean is_chatpost = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Chat now");
        setContentView(R.layout.activity_chat_layout);
        ActionBar actionBar = getSupportActionBar();
        ((ActionBar) actionBar).setDisplayHomeAsUpEnabled(true);
        setweigits();

    }

    private void setweigits() {
        rv_recyclerviw = findViewById(R.id.rv_recyclerviw);
        tv_no_records = findViewById(R.id.tv_no_records);
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Loading...");

        if (Utils.isOnline(this)) {
            call_marchrentlistApi();
        } else {
            tv_no_records.setVisibility(View.VISIBLE);
            rv_recyclerviw.setVisibility(View.GONE);
            tv_no_records.setText(getResources().getString(R.string.network_connection_error));
//            Toast.makeText(this, getResources().getText(R.string.network_connection_error), Toast.LENGTH_LONG).show();
        }


    }

    private void call_marchrentlistApi() {
        final String account_id;
        if (!Account.loggedIn) {

            return;
        } else {
            account_id = Account.accountData.get("id");
            Log.e(TAG, "account_id=> " + Account.accountData.get("id"));
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", account_id)
                .build();

        Request request = new Request.Builder()
                .url("https://www.benslist.com/Api/Chat/chat_merchant_list.inc.php")
                .post(requestBody)
                .build();
        progressDialog.show();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Log.e(TAG, "error");
                        rv_recyclerviw.setVisibility(View.GONE);
                        tv_no_records.setVisibility(View.VISIBLE);
                        tv_no_records.setText(getResources().getString(R.string.server_error));
                        Toast.makeText(MarchentListActivity.this, getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String respo = response.body().string();
                    Log.e(TAG, "onResponse: \n" + respo);
                    if (!respo.equalsIgnoreCase("[]")) {
                        try {
                            JSONArray itemArray = new JSONArray(respo);
                            Type type = new TypeToken<ArrayList<MarchentListResponse>>() {
                            }.getType();
                            list = (new Gson()).fromJson(itemArray.toString(), type);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    tv_no_records.setVisibility(View.GONE);
                                    rv_recyclerviw.setLayoutManager(new LinearLayoutManager(MarchentListActivity.this));
                                    rv_recyclerviw.setAdapter(new Marchent_ListAdapter(MarchentListActivity.this, list, MarchentListActivity.this, is_chatpost));
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    } else {
                        progressDialog.dismiss();
                        rv_recyclerviw.setVisibility(View.GONE);
                        tv_no_records.setVisibility(View.VISIBLE);
                        tv_no_records.setText("No record found");


                        Log.e(TAG, "arrary blank get" + respo);
                    }


                } else {
                    progressDialog.dismiss();
                    rv_recyclerviw.setVisibility(View.GONE);
                    tv_no_records.setVisibility(View.VISIBLE);
                    tv_no_records.setText("No record found");
                    Log.e(TAG, "unsuccess");

                }
            }
        });


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

    @Override
    public void OnPosiClieck(int pos, String user_login_id, String marchentid, String postid) {
        Log.e(TAG, "OnPosiClieck " + pos + "\tUserlogin id " + user_login_id + "\t marchent_id" + marchentid + "\tpostid" + postid);
        if (!is_chatpost) {
            startActivity(new Intent(this, ChatPostListActivity.class).putExtra("merchant_id", marchentid));

        }
        /*Only come if becone this actvity send ischatpost boolean false than we need only call false condition*/

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        /*not use*/


    }
}
