/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ruptech.tttalk_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ruptech.tttalk_android.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Adapter for the planet data used in our drawer menu,
 */
public class SlidingMenuAdapter extends ArrayAdapter<String> {
    private static final int mResource = R.layout.drawer_list_item; // xml布局文件
    protected LayoutInflater mInflater;

    public SlidingMenuAdapter(Context context, String[] objects) {
        super(context, mResource, objects);

        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View view;
        final ViewHolder holder;
        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();

        }

        String title = getItem(position);

        holder.titleTextView.setText(title);
        return view;
    }

    static class ViewHolder {
        @InjectView(android.R.id.text1)
        TextView titleTextView;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

}
