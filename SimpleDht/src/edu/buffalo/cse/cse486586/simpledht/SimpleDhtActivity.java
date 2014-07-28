package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {

	
	String TAG= "Shiyam";
	static String node_id;
	static String myPort = null;
	private ContentResolver mContentResolver;
	private Handler uiHandle= new Handler();
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);
        mContentResolver = getContentResolver();
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
        
        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				LDump(v);
			}
		});
        
        findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//querySample(v);
				deleteTesting(v);
			}
		});
       
       // myPort = getPortString();
        
       // joinChord();
    }
    
    
    private void joinChord()
    {
    	
    	node_id = getPortString();
    	Log.v("ID",node_id+"");
    	Message msg = new Message("join", node_id);
    	Message socket = new Message("11108");
    	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, socket);
    }
    
    private String getPortString()
    {
    	TelephonyManager telephone = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
    	String portStr = telephone.getLine1Number().substring(telephone.getLine1Number().length() - 4);	
    	final String port = String.valueOf((Integer.parseInt(portStr)));
    	return port;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }
    
    public class ClientTask extends AsyncTask<Message, Void, Void> {

        @Override
        protected Void doInBackground(Message... msgs) {
            try {
                String remotePort = msgs[1].socket;
                Log.v("SOCKET",remotePort);
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));
                
                Message msgToSend = msgs[0];
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(msgToSend);
                out.flush();
                out.close();
                
                
                 // TODO: Fill in your client code that sends out a message.
                 
                
                socket.close();
            } catch (UnknownHostException e) {
                Log.e("Client", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("Client", "ClientTask socket IOException");
            }

            return null;
        }

		public void executeOnExecutor(Executor serialExecutor, Message m,
				String string) {
			// TODO Auto-generated method stub
			
		}
    }
    
    /*public void updateTextView(String message) {
    	final String msg= message;
    	uiHandle.post(new Runnable() {
    		public void run() {
    			TextView textView = (TextView)findViewById(R.id.textView1);
    			textView.setMovementMethod(new ScrollingMovementMethod());
    	    	//Log.v(TAG, "updating textview");
    	    	textView.append(msg+"\n");
    	    	//Log.v(TAG, "updated textview");
       		}
    	});
    }*/
    
    public void LDump(View view) {
    	//SimpleDhtProvider.dump_flag = true;
    	Cursor resultCursor = mContentResolver.query(SimpleDhtProvider.CONTENT_URI, null, "@", null, "local");
    	if (resultCursor.moveToFirst()) {
    	    while (!resultCursor.isAfterLast()) {
    	    	int keyIndex = resultCursor.getColumnIndex("key");
    	        int valueIndex = resultCursor.getColumnIndex("value");
    	    	String returnKey = resultCursor.getString(keyIndex);
    	        String returnValue = resultCursor.getString(valueIndex);
    	        updateTextView(returnKey+" "+returnValue);
    	        resultCursor.moveToNext();
    	    	}
    	    }
    	//SimpleDhtProvider.dump_flag = false;
    }
    
    public void querySample(View view) {
    	//SimpleDhtProvider.dump_flag = true;
    	Cursor resultCursor = mContentResolver.query(SimpleDhtProvider.CONTENT_URI, null, "*", null, "local");
    	if(resultCursor != null)
    	{
    		if (resultCursor.moveToFirst()) 
    		{
    			while (!resultCursor.isAfterLast()) 
    			{
    				int keyIndex = resultCursor.getColumnIndex("key");
    				int valueIndex = resultCursor.getColumnIndex("value");
    				String returnKey = resultCursor.getString(keyIndex);
    				String returnValue = resultCursor.getString(valueIndex);
    				updateTextView(returnKey+" "+returnValue);
    				resultCursor.moveToNext();
    			}
    		}
    	}
    	//SimpleDhtProvider.dump_flag = false;
    }
    
    public void deleteTesting(View v)
    {
    	updateTextView(" ");
    	System.out.println("Inside deleteTestin()");
    	int a = mContentResolver.delete(SimpleDhtProvider.CONTENT_URI, "key"+0, null);
    	updateTextView(a+"");
    }
    
    public void updateTextView(String message) {
    	final String msg= message;
    	uiHandle.post(new Runnable() {
    		public void run() {
    			TextView textView = (TextView)findViewById(R.id.textView1);
    			textView.setMovementMethod(new ScrollingMovementMethod());
    	    	//Log.v(TAG, "updating textview");
    	    	textView.append(msg+"\n");
    	    	//Log.v(TAG, "updated textview");
       		}
    	});
    }


}
