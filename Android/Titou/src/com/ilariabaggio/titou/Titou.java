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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;


public class Titou extends Activity {
	private static final String TAG = "Titou_Debug";
    private final int CELL_DEFAULT_HEIGHT = 200;

    private ExpandingListView mListView;
    private boolean contamination = false;
    private boolean isPlaying = false;
    final int RECIEVE_MESSAGE = 1;		// Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    Handler hdler;
    private StringBuilder sb = new StringBuilder();
    private int beat_img_list[] = {R.drawable.beat_lowest,R.drawable.beat_low,R.drawable.beat_middle_low,R.drawable.beat_middle_high,R.drawable.beat_high,R.drawable.beat_highest};
    private String back_color_list[] = {"#002F55","#15BAE6","#86BC24","#FCC306","#FF5000","#B51F29"};
    private ConnectedThread mConnectedThread;
    private String [][] songs = {
    		{"Beach Boys","California Girls"},
    		{"Garbage","Run Baby Run"},
    		{"Buffalo Springfield","For What Its Worth"},
    		{"Brian Eno ","By this river"},
    		{"Charles Aznavour","For me formidable"},
    		{"Johnny Cash","Solitary Man"},
    		{"The Troggs","With a girl like you"}
    		}; 
    
    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
   
    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "00:06:66:66:35:80";
    
    private List<ExpandableListItem> mData = new ArrayList<ExpandableListItem>();
    
    ImageButton btnPlay;
    ImageButton btnPrevious;
    ImageButton btnNext;
    ImageButton btnContamination;
    ImageButton btnShareSong;
    LinearLayout player;
    TextView playerArtist;
    TextView playerSong;
    
    WebView wvgif;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
      
        wvgif = (WebView) findViewById(R.id.webViewGif);
        wvgif.loadUrl("file:///android_asset/titou_progress_medium.gif");
        wvgif.setBackgroundColor(0x00000000);
        
        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnShareSong = (ImageButton) findViewById(R.id.btnShareSong);
        btnContamination = (ImageButton) findViewById(R.id.btnContamination);
        player = (LinearLayout) findViewById(R.id.player);
        playerArtist = (TextView) findViewById(R.id.songArtist);
        playerSong = (TextView) findViewById(R.id.songTitle);

        List<ExpandableListItem> mData = new ArrayList<ExpandableListItem>();
        mData.add(new ExpandableListItem("Tender","Blur", CELL_DEFAULT_HEIGHT,"Library",R.drawable.beat_middle_high));
        mData.add(new ExpandableListItem("Jubel","Klingande", CELL_DEFAULT_HEIGHT,"Gym",R.drawable.beat_high));
        mData.add(new ExpandableListItem("Happy Pills","Nora Jones", CELL_DEFAULT_HEIGHT,"Home",R.drawable.beat_lowest));
        
        CustomArrayAdapter adapter = new CustomArrayAdapter(this, R.layout.list_view_item, mData);
        mListView = (ExpandingListView)findViewById(R.id.exp_list_view);
        mListView.setAdapter(adapter);
        mListView.setDivider(null);
        setListViewHeightBasedOnChildren(mListView);
        
        hdler = new Handler() {
        	@Override
			public void handleMessage(android.os.Message msg) {
        		switch (msg.what) {
                case RECIEVE_MESSAGE:													// if receive massage
                	byte[] readBuf = (byte[]) msg.obj;
                	String strIncom = new String(readBuf, 0, msg.arg1);					// create string from bytes array
                	sb.append(strIncom);// append string
                	Log.d(TAG, "Message recived: " + strIncom);
                	int endOfLineIndex = sb.indexOf("\n");// determine the end-of-line
                	if (endOfLineIndex > 0) { 											// if end-of-line,
                		String sbprint = sb.substring(0, endOfLineIndex);				// extract string
                		Log.d(TAG, sbprint);
                		sb.delete(0, sb.length());
                		if (sbprint.charAt(0) == 'P' && Character.isDigit(sbprint.charAt(1))){
                			sbprint.replaceAll("\n", "");
                    		int song = Character.getNumericValue(sbprint.charAt(1));
                    		Log.d(TAG, "Song index " + Integer.toString(song));
                    		playerArtist.setText(songs[song][0]);
                    		playerSong.setText(songs[song][1]);
            				if(isPlaying){
            					btnPlay.setImageResource(R.drawable.btn_play);
            					isPlaying = false;	
            				}
            				else{
            					btnPlay.setImageResource(R.drawable.btn_pause);
            					isPlaying = true;
            				}
                		}
                		else if (sbprint.charAt(0) == 'S' && Character.isDigit(sbprint.charAt(1))){
                			sbprint.replaceAll("\n", "");
                    		int song = Character.getNumericValue(sbprint.charAt(1));
                    		Log.d(TAG, "Song index " + Integer.toString(song));
                    		playerArtist.setText(songs[song][0]);
                    		playerSong.setText(songs[song][1]);
                    		btnPlay.setImageResource(R.drawable.btn_pause);
        					isPlaying = true;
                		}
                		else if (sbprint.charAt(0) == 'C'){
                    		Log.d(TAG, "Toggle contamination.");
                    		if (!contamination){
                    			contamination = true;
                    			btnContamination.setImageResource(R.drawable.img_btn_contamination);
                    		}
                    		else{
                    			contamination = false;
                    			btnContamination.setImageResource(R.drawable.img_btn_contamination_low);
                    		}
                    	}
                    	else if (sbprint.charAt(0) == 'M' && Character.isDigit(sbprint.charAt(1))){
                    		sbprint.replaceAll("\n", "");
                    		int song = Character.getNumericValue(sbprint.charAt(1));
                    		int BPM = Integer.parseInt(sbprint.substring(2, endOfLineIndex));
                    		Log.d(TAG, "Song index " + Integer.toString(song));
                    		Log.d(TAG, "Raw BPM " + Integer.toString(BPM));
                    		if (BPM >= 200 )	BPM = 199;
                    		else if (BPM < 50) BPM = 50;
                    		int BPM_level = (int) Math.floor((float)BPM/25.00);
                    		BPM_level = BPM_level -2;
                    		Log.d(TAG, "Adding entry with BPM " + Integer.toString(BPM) + " and song " + songs[song][0] + " " + songs[song][1]);
                    		addEntryList(song,beat_img_list[BPM_level]);
                    		player.setBackgroundColor(Color.parseColor(back_color_list[BPM_level]));
                    	} 
                    }
                	break;
        		}
            };
    	};
    	
        btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
        checkBTState();
        
        btnPlay.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
            	//btnPlay.setEnabled(false);
				if(isPlaying){
					btnPlay.setImageResource(R.drawable.btn_play);
					isPlaying = false;	
				}
				else{
					btnPlay.setImageResource(R.drawable.btn_pause);
					isPlaying = true;
				}
            	mConnectedThread.writeBT("P");	// Send "1" via Bluetooth
            	//Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnPrevious.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
            	//btnPrevious.setEnabled(false);
            	mConnectedThread.writeBT("B");	// Send "1" via Bluetooth
            	//Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnNext.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
            	//btnNext.setEnabled(false);
            	mConnectedThread.writeBT("N");	// Send "1" via Bluetooth
            	//Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
         });
        
        btnShareSong.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
            	//btnReq.setEnabled(false);
            	mConnectedThread.writeBT("S");	// Send "1" via Bluetooth
            	//Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });
        btnContamination.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
        		if (!contamination){
        			contamination = true;
        			btnContamination.setImageResource(R.drawable.img_btn_contamination);
        		}
        		else{
        			contamination = false;
        			btnContamination.setImageResource(R.drawable.img_btn_contamination_low);
        		}
            	//btnReq.setEnabled(false);
            	mConnectedThread.writeBT("C");	// Send "1" via Bluetooth
            	//Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
         }); 
    }
    
    public void addEntryList(int song,int BPM_level){
    	
        ExpandableListItem[] values = new ExpandableListItem[] {
                new ExpandableListItem(songs[song][0],songs[song][1], CELL_DEFAULT_HEIGHT,"Trevano",BPM_level),
        };
        
        ExpandableListItem obj = values[0];
        mData.add(0,new ExpandableListItem(obj.getSongTitle(),obj.getSongArtist(),
                obj.getCollapsedHeight(), obj.getLoc(),obj.getBPM_level()));
        
        CustomArrayAdapter adapter = new CustomArrayAdapter(this, R.layout.list_view_item, mData);
        mListView = (ExpandingListView)findViewById(R.id.exp_list_view);
        mListView.setAdapter(adapter);
        mListView.setDivider(null);
        setListViewHeightBasedOnChildren(mListView);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(TAG, "...onResume - try connecting...");
          
		// Set up a pointer to the remote node using it's address.
		BluetoothDevice device = btAdapter.getRemoteDevice(address);
      
  		try {
  			btSocket = createBluetoothSocket(device);
  		} catch (IOException e) {
  			errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
  		}
     
  		// Discovery is resource intensive.  Make sure it isn't going on
		// when you attempt to connect and pass your message.
		btAdapter.cancelDiscovery();
		  
		// Establish the connection.  This will block until it connects.
		Log.d(TAG, "...Connecting...");
		try {
			btSocket.connect();
			Log.d(TAG, "....Connection ok...");
			// Create a data stream so we can talk to server.
			Log.d(TAG, "...Create Socket...");
			mConnectedThread = new ConnectedThread(btSocket);
			mConnectedThread.start();
		} catch (IOException e) {
			try {
				Log.d(TAG, "....Connection didnt work, closing...");
				btSocket.close();
			} catch (IOException e2) {
				errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
			}
		}
    }
   
    @Override
    public void onPause() {
      super.onPause();
   
      Log.d(TAG, "...In onPause()...");
    
      try     {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
      }
    }
        
    public static void setListViewHeightBasedOnChildren(ExpandingListView listView) {
        ListAdapter listAdapter = listView.getAdapter(); 
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
    
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }
    
    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) { 
          errorExit("Fatal Error", "Bluetooth not support");
        } else {
          if (btAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth ON...");
          } else {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
          }
        }
      }
    
    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }
    
    private class ConnectedThread extends Thread {
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    @Override
		public void run() {
	        byte[] buffer = new byte[256];  // buffer store for the stream
	        int bytes; // bytes returned from read()

	        // Keep listening to the InputStream until an exception occurs
	        while (true) { 
	        	try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
	                hdler.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void writeBT(String message) {
	    	Log.d(TAG, "...Data to send: " + message + "...");
	    	message += "\n";
	    	try {
	            mmOutStream.write(message.getBytes());
	        } catch (IOException e) {
	            Log.d(TAG, "...Error data send: " + e.getMessage() + "...");     
	          }
	    }
	}
}