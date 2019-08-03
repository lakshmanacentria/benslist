package com.acentria.benslist.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.acentria.benslist.R;
import com.acentria.benslist.response.DonateCharityResponse;

import java.util.List;

public class ProductqunatiyAdapter extends RecyclerView.Adapter<ProductqunatiyAdapter.ViewHolder> {

    private View view;
    private Context mcontext;
    private onClickQuanity monClickQuanity;
    public List<DonateCharityResponse> mlist;
    private String TAG = "ProductqunatiyAdapter=> ";


    public ProductqunatiyAdapter(List<DonateCharityResponse> list, Context context, onClickQuanity onClickQuanity) {
        this.mlist = list;
        this.mcontext = context;
        this.monClickQuanity = onClickQuanity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_product_quanity_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            if (mlist != null) {
                if (mlist.size() > 0) {

                    holder.et_product.setText(mlist.get(position).getProduct());
                    if (holder.et_qauntity.getText().toString().equalsIgnoreCase("")) {
                        holder.et_qauntity.setText("");
                    }
                    mlist.get(position).setManualQuanty(holder.et_qauntity.getText().toString());
                    Log.e(TAG, "manual quantity" + mlist.get(position).getManualQuanty());
                    monClickQuanity.onItemClick(position, holder.et_product.getText().toString(), holder.et_qauntity.getText().toString());

                }
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
        private EditText et_product, et_qauntity;

        public ViewHolder(View itemView) {
            super(itemView);
            et_product = itemView.findViewById(R.id.et_product);
            et_qauntity = itemView.findViewById(R.id.et_qauntity);

        }
    }

    public interface onClickQuanity {
        void onItemClick(int posi, String product, String qunatity);
    }
}
