package com.camera;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class GridAdapter extends BaseAdapter {
    private Context context;
    private List<String> labels;

    public GridAdapter(Context c, List<String> labels) {
        context = c;
        this.labels = labels;
    }

    public int getCount() {
        return labels.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View gridView = convertView;

        if (convertView == null) {
            // get layout from mobile.xml
            gridView = inflater.inflate(R.layout.grid_item, null);
        }
        // set value into textview
        TextView textView = (TextView) gridView
                .findViewById(R.id.grid_item_label);
        textView.setText(labels.get(position));
        ImageView imageView = (ImageView) gridView.findViewById(R.id.grid_item_number);
        imageView.setImageResource(R.drawable.folder_image);
        if (position == getCount() - 1) {
            imageView.setImageResource(R.drawable.folder_plus);
        }
        return gridView;
    }
}