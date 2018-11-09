package com.baseform.power;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class DeploymentAdapter extends ArrayAdapter<DeploymentItem> {

    private int resourceLayout;
    private Context mContext;

    public DeploymentAdapter(Context context, int resource, List<DeploymentItem> items) {
        super(context, resource, items);
        this.resourceLayout = resource;
        this.mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(mContext);
            v = vi.inflate(resourceLayout, null);
        }

        final DeploymentItem p = getItem(position);

        if (p != null) {
            View item = v.findViewById(R.id.deploymentItem);
            TextView name = v.findViewById(R.id.deploymentName);
            ImageView icon = v.findViewById(R.id.deploymentIcon);

            name.setText(p.getName());
            icon.setImageDrawable(p.getIcon());

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                Intent intent = new Intent(mContext, PageActivity.class);
                intent.putExtra("url", p.getUrl());
                mContext.startActivity(intent);
                }
            });

        }

        return v;
    }

}