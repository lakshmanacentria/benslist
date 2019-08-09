package com.acentria.benslist.chatprocess;

import android.app.Activity;
import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.response.ChatMessageResponse;

import java.util.List;

public class ChatMessageAdatper extends RecyclerView.Adapter<ChatMessageAdatper.ViewHolder> {
    private String TAG = ChatMessageAdatper.class.getName(), img_url;
    private Context mcontext;
    private List<ChatMessageResponse> mlist;
    private boolean is_chat_bayer;


    public ChatMessageAdatper(Context context, List<ChatMessageResponse> list, boolean is_chat_bayer) {
        this.mcontext = context;
        this.mlist = list;
        this.is_chat_bayer = is_chat_bayer;
        Log.e(TAG, "is_chat_bayer " + is_chat_bayer);
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chat_history_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            if (mlist.size() > 0) {

//                Log.e(TAG, "onBindViewHolder:merchent sms " + mlist.get(position).getMerchantMessage());
//                Log.e(TAG, "onBindViewHolder: user sms" + mlist.get(position).getUserMessage());
                Log.e(TAG, "onBindViewHolder: " + mlist.size());

                if (is_chat_bayer) {
                    /*bayer side login*/
                    if (mlist.get(position).getMerchantMessage().equalsIgnoreCase("") /*|| mlist.get(position).getMerchantMessage() != null*/) {
                        /*user message will be display on right side */
                        holder.tv_date.setText(mlist.get(position).getDate());
                        holder.tv_fname.setText(Account.accountData.get("username"));
                        holder.tv_message.setText(mlist.get(position).getUserMessage());
                        Utils.imageLoaderDisc.displayImage(Account.accountData.get("photo"), holder.imageview_right, Utils.imageLoaderOptionsDisc);
                        holder.const_left.setVisibility(View.GONE);
                        Log.e(TAG, "is_chat_bayer=>" + is_chat_bayer + "\tuser right side" + mlist.get(position).getUserMessage() + "\tmarchent " + mlist.get(position).getMerchantMessage());
                    } else {
                        /*marchent message will be display left side dispay*/
                        holder.tv_date_left.setText(mlist.get(position).getDate());
                        holder.tv_lname.setText(mlist.get(position).getMerchent_name());
                        holder.tv_message_left.setText(mlist.get(position).getMerchantMessage());
                        /*image null display*/
                        holder.const_left.setVisibility(View.VISIBLE);
                        holder.const_right.setVisibility(View.GONE);
                        Log.e(TAG, "is_chat_bayer=>" + is_chat_bayer + "\tseller left side " + mlist.get(position).getUserMessage() + "merchent " + mlist.get(position).getMerchantMessage());

                    }
                } else {
                    /*seller(merchent) side login*/
                    if (mlist.get(position).getUserMessage().equalsIgnoreCase("")) {
                        holder.tv_date.setText(mlist.get(position).getDate());
                        holder.tv_fname.setText(Account.accountData.get("username"));
                        holder.tv_message.setText(mlist.get(position).getMerchantMessage());
                        Utils.imageLoaderDisc.displayImage(Account.accountData.get("photo"), holder.imageview_right, Utils.imageLoaderOptionsDisc);
                        holder.const_left.setVisibility(View.GONE);
                        Log.e(TAG, "is_chat_bayer=>" + is_chat_bayer + "\tseller right" + mlist.get(position).getUserMessage() + "merchent " + mlist.get(position).getMerchantMessage());

                    } else {

                        holder.tv_date_left.setText(mlist.get(position).getDate());
                        holder.tv_lname.setText(mlist.get(position).getMerchent_name());
                        holder.tv_message_left.setText(mlist.get(position).getUserMessage());
//                        Utils.imageLoaderDisc.displayImage(Account.accountData.get("photo"), holder.imageview_left, Utils.imageLoaderOptionsDisc);
                        holder.const_left.setVisibility(View.VISIBLE);
                        holder.const_right.setVisibility(View.GONE);
                        Log.e(TAG, "is_chat_bayer=>" + is_chat_bayer + "bayerside left " + mlist.get(position).getUserMessage() + "\tmerchent " + mlist.get(position).getMerchantMessage());

                    }
                }


            } else {
                Log.e(TAG, "onBindViewHolder: list size less than zero ");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return mlist.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_date;
        TextView tv_fname;
        TextView tv_message;
        //        ConstraintLayout constraintLayout;
        ImageView imageview_right;

        ConstraintLayout const_right;
        ConstraintLayout const_left;
        TextView tv_lname;
        TextView tv_message_left;
        TextView tv_date_left;

        //        ConstraintLayout constraintLayout_left;
        ImageView imageview_left;
        ConstraintLayout const_parent;

        public ViewHolder(View itemView) {
            super(itemView);
            const_parent = itemView.findViewById(R.id.const_parent);
            tv_date = itemView.findViewById(R.id.tv_date);
            tv_fname = itemView.findViewById(R.id.tv_fname);
            tv_message = itemView.findViewById(R.id.tv_message);
            const_right = itemView.findViewById(R.id.const_right);
            imageview_right = itemView.findViewById(R.id.circularImageView);

            const_left = itemView.findViewById(R.id.const_left);
            tv_lname = itemView.findViewById(R.id.tv_lname);
            tv_message_left = itemView.findViewById(R.id.tv_messagee);
            tv_date_left = itemView.findViewById(R.id.tv_datee);
            imageview_left = itemView.findViewById(R.id.circularImageVieww);


        }
    }
}
