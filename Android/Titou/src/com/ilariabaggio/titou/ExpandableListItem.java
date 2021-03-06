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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * This custom object is used to populate the list adapter. It contains a reference
 * to an image, title, and the extra text to be displayed. Furthermore, it keeps track
 * of the current state (collapsed/expanded) of the corresponding item in the list,
 * as well as store the height of the cell in its collapsed state.
 */
public class ExpandableListItem implements OnSizeChangedListener {

    private String mSongTitle;
    private String mSongArtist;
    private String mLocation;
    private String mLocationTime;
    private String mLocationDate;
    private boolean mIsExpanded;
    private int mBPM_level;
    private int mCollapsedHeight;
    private int mExpandedHeight;

    public ExpandableListItem(String title, String artist, int collapsedHeight,
    		String location,int BPM_level){
    	
        Calendar calendar;
        calendar = Calendar.getInstance(); 
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault());
    	SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm",java.util.Locale.getDefault());
    	
        Date date = calendar.getTime();
        
        mSongTitle = title;
        mSongArtist = artist;
        mCollapsedHeight = collapsedHeight;
        mIsExpanded = false;
        mLocation = location;
        mLocationDate = dateFormat.format(date);
        mLocationTime = timeFormat.format(date);
        mBPM_level = BPM_level;
        mExpandedHeight = -1;
    }

    public boolean isExpanded() {
        return mIsExpanded;
    }

    public void setExpanded(boolean isExpanded) {
        mIsExpanded = isExpanded;
    }

    public String getSongTitle() {
        return mSongTitle;
    }
    
    public String getSongArtist() {
        return mSongArtist;
    }
    
    public int getBPM_level() {
        return mBPM_level;
    }

    public int getCollapsedHeight() {
        return mCollapsedHeight;
    }

    public void setCollapsedHeight(int collapsedHeight) {
        mCollapsedHeight = collapsedHeight;
    }

    public String getLoc() {
        return mLocation;
    }

    public void setLoc(String text) {
    	mLocation = text;
    }
    
    public String getLocTime() {
        return mLocationTime;
    }

    public void setLocTime(String text) {
    	mLocationTime = text;
    }
    public String getLocDate() {
        return mLocationDate;
    }

    public void setLocDate(String text) {
    	mLocationDate = text;
    }
    
    public int getExpandedHeight() {
        return mExpandedHeight;
    }

    public void setExpandedHeight(int expandedHeight) {
        mExpandedHeight = expandedHeight;
    }

    @Override
    public void onSizeChanged(int newHeight) {
        setExpandedHeight(newHeight);
    }
}
