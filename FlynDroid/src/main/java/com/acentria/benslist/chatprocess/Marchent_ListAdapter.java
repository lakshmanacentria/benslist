package com.acentria.benslist.chatprocess;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.acentria.benslist.R;
import com.acentria.benslist.response.ChatPostResponse;
import com.acentria.benslist.response.MarchentListResponse;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;


public class Marchent_ListAdapter extends RecyclerView.Adapter<Marchent_ListAdapter.ViewHolder> {
    private String TAG = Marchent_ListAdapter.class.getName();
    private Context mcontext;
    private List<MarchentListResponse> mlist;
    private List<ChatPostResponse> mlist_chatpost;
    private OnClickPosi monClickPosi;
    private boolean mischatpost;
    private String linke_url = "";

    public Marchent_ListAdapter(Context context, List<MarchentListResponse> list, OnClickPosi onClickPosi, boolean ischatpost) {
        this.mcontext = context;
        this.mlist = list;
        this.monClickPosi = onClickPosi;
        this.mischatpost = ischatpost;
        Log.e(TAG, "Marchent List ischatpost=> " + mischatpost);
    }

    public Marchent_ListAdapter(ChatPostListActivity context, OnClickPosi onClickPosi, List<ChatPostResponse> list, boolean ischatpost) {
        this.mcontext = context;
        this.mlist_chatpost = list;
        this.monClickPosi = onClickPosi;
        this.mischatpost = ischatpost;
        Log.e(TAG, "Chat Post List ischatpost=> " + mischatpost);
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_marchent_list_layout, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        try {
            if (mischatpost) {

                if (mlist_chatpost.size() > 0) {
                    holder.tv_description.setVisibility(View.VISIBLE);
                    holder.tv_description.setText(mlist_chatpost.get(position).getJobPosition() + "");
                    holder.tvmarchent_name.setText(mlist_chatpost.get(position).getTitle());
                    Log.e(TAG, "marchentname" + mlist_chatpost.get(position).getTitle());
                    /*https://www.benslist.com/files/05-2019/ad182/thumb_15567757831380865109.jpeg*/
                    linke_url = "https://www.benslist.com/files/" + mlist_chatpost.get(position).getMainPhoto();
                    Log.e(TAG, "url" + linke_url);

                    Glide.with(mcontext).load(linke_url).placeholder(R.mipmap.seller_no_photo).diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate().into(holder.img_profile);

                    holder.constrain_parent.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            monClickPosi.OnPosiClieck(position, mlist_chatpost.get(position).getUserId(), mlist_chatpost.get(position).getMerchantId(), mlist_chatpost.get(position).getPostId(), mlist_chatpost.get(position).getUsername());
                        }
                    });
                }

            } else {
                holder.tvmarchent_name.setText(mlist.get(position).getUsername());
                Log.e(TAG, "marchentname" + mlist.get(position).getUsername());

                holder.constrain_parent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        monClickPosi.OnPosiClieck(position, "user_loginid", mlist.get(position).getId(), "postidnot use", mlist.get(position).getUsername());
                    }
                });
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public int getItemCount() {
        if (mischatpost) {
            return mlist_chatpost.size();
        } else {
            return mlist.size();
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView img_profile;
        private TextView tvmarchent_name, tv_description;
        private ConstraintLayout constrain_parent;

        public ViewHolder(View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.img_profile);
            tvmarchent_name = itemView.findViewById(R.id.tvmarchent_name);
            tv_description = itemView.findViewById(R.id.tv_description);
            constrain_parent = itemView.findViewById(R.id.constrain_parent);
        }
    }


    public interface OnClickPosi {
        void OnPosiClieck(int pos, String user_login_id, String marchentid, String post_id, String username);
    }
}
