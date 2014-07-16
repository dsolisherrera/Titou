/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.ilariabaggio.titou;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

/**
 * This is a custom array adapter used to populate the listview whose items will
 * expand to display extra content in addition to the default display.
 */
public class CustomArrayAdapter extends ArrayAdapter<ExpandableListItem> {

    private List<ExpandableListItem> mData;
    private int mLayoutViewResourceId;

    public CustomArrayAdapter(Context context, int layoutViewResourceId,
                              List<ExpandableListItem> data) {
        super(context, layoutViewResourceId, data);
        mData = data;
        mLayoutViewResourceId = layoutViewResourceId;
    }

    /**
     * Populates the item in the listview cell with the appropriate data. This method
     * sets the thumbnail image, the title and the extra text. This method also updates
     * the layout parameters of the item's view so that the image and title are centered
     * in the bounds of the collapsed view, and such that the extra text is not displayed
     * in the collapsed state of the cell.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ExpandableListItem object = mData.get(position);

        if(convertView == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            convertView = inflater.inflate(mLayoutViewResourceId, parent, false);
        }

        RelativeLayout relativeLayout = (RelativeLayout)(convertView.findViewById(
                R.id.item_relative_layout));
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams
                (LayoutParams.MATCH_PARENT, object.getCollapsedHeight());
        relativeLayout.setLayoutParams(linearLayoutParams);        


        TextView songTitleView = (TextView)convertView.findViewById(R.id.songTitle_list);
        TextView songArtistView = (TextView)convertView.findViewById(R.id.songArtist_list);
        ImageView beat_view = (ImageView)convertView.findViewById(R.id.beat_view);
        TextView loctextView = (TextView)convertView.findViewById(R.id.location_text_view);
        TextView loctimetextView = (TextView)convertView.findViewById(R.id.location_time_view);
        TextView locdatetextView = (TextView)convertView.findViewById(R.id.location_date_view);
        

        songTitleView.setText(object.getSongTitle());
        songArtistView.setText(object.getSongArtist());
        beat_view.setImageResource(object.getBPM_level());
        loctextView.setText(object.getLoc());
        loctimetextView.setText(object.getLocTime());
        locdatetextView.setText(object.getLocDate());

        convertView.setLayoutParams(new ListView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        ExpandingLayout expandingLayout = (ExpandingLayout)convertView.findViewById(R.id
                .expanding_layout);
        expandingLayout.setExpandedHeight(object.getExpandedHeight());
        expandingLayout.setSizeChangedListener(object);

        if (!object.isExpanded()) {
            expandingLayout.setVisibility(View.GONE);
        } else {
            expandingLayout.setVisibility(View.VISIBLE);
        }

        return convertView;
    }
}