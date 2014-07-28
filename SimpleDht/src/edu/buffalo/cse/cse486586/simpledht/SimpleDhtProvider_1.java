package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import edu.buffalo.cse.cse486586.simpledht.SimpleDhtActivity.ClientTask;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

public class SimpleDhtProvider extends ContentProvider {

	private myDatabase database;
	private SQLiteDatabase sqlDb;
	private static final String AUTHORITY = "edu.buffalo.cse.cse486586.simpledht.provider";
	private SortedMap<String, String> chord;
	private static final String BASE_PATH = myDatabase.TABLE_NAME;
	public static final Uri CONTENT_URI = Uri.parse("content://"+ AUTHORITY + "/" + BASE_PATH);
	public static final String TAG= "Shiyam";
	private static String node_id = null;
	private static Neighbours receivedNodes;
	private Handler uiHandle= new Handler();
	private ArrayList<String> forJoin;
	private boolean query_reply_flag = false;
	private boolean global_reply_flag = false;
	private Map<String,String> cursorMap = null;
	private Map<String,String> dumpCursorMap = null;
	private Cursor replyCursor;
	private Cursor dumpCursor;
	private String deleteGlobalId = null;
	
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
    	
    	sqlDb = database.getWritableDatabase();
    	
    	//int a = sqlDb.delete(myDatabase.TABLE_NAME,selection ,selectionArgs);
    	System.out.println("Inside delete()");
    	int a = sqlDb.delete(myDatabase.TABLE_NAME,myDatabase.KEY_FIELD+"=?",selectionArgs);
    	
    	if(selection != null)
    	{
    		if(selection.equals("@"))
    		{
    			a = sqlDb.delete(myDatabase.TABLE_NAME,null,null);
    		}
    		else if(selection.equals("*"))
    		{
    			Message msg;
    			a = sqlDb.delete(myDatabase.TABLE_NAME,null,null);
    			if(deleteGlobalId != null)
    				msg = new Message("delete_global",deleteGlobalId);
    			else
    				msg = new Message("delete_global",node_id);
    			Message socket = new Message(getPortAddr(receivedNodes.succ)+"");
    			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,socket);
    		}
    		else
    		{
    			a = sqlDb.delete(myDatabase.TABLE_NAME,myDatabase.KEY_FIELD+"=?",new String[]{selection});
    			Message msg;
    			if(a == 0)
    			{
    				if(deleteGlobalId != null)
        				msg = new Message("delete_global",deleteGlobalId);
        			else
        				msg = new Message("delete_global",node_id);
        			Message socket = new Message(getPortAddr(receivedNodes.succ)+"");
        			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,socket);
    			}
    		}

    	}
    	
        return a;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
    	System.out.println("Inside insert()");
    	long rowId=0;
    	String hashKey,hashNode,hashPred;
    	final ContentValues insertValues = new ContentValues(values);
    	sqlDb = database.getWritableDatabase();
    	String key =(String) insertValues.get(myDatabase.KEY_FIELD);
    	//String[] insert = null;
    	
    	
    	try 
    	{
    		hashKey = genHash(key);
    		hashNode = genHash(node_id);
    		hashPred= genHash(receivedNodes.pred);

    		//if(hashKey.compareTo(hashNode) <= 0 && hashKey.compareTo(hashPred) > 0)
    		if(condition1(hashKey,hashNode,hashPred))
    		{
    			//System.out.println("Inside():: 1");
    			rowId= sqlDb.replace(myDatabase.TABLE_NAME, myDatabase.VALUE_FIELD, insertValues);
    		}
    		//else if((hashKey.compareTo(hashNode) <= 0 || hashKey.compareTo(hashPred) > 0) && node_id.equals(receivedNodes.isOnly))
    		else if(condition2(hashKey,hashNode,hashPred))
    		{
    			//System.out.println("Inside():: 2");
    			rowId= sqlDb.replace(myDatabase.TABLE_NAME, myDatabase.VALUE_FIELD, insertValues);
    		}
    		else
    		{
    			//System.out.println("Inside():: 3");
    			String[] insert = {key, (String)insertValues.get(myDatabase.VALUE_FIELD)};
    			Message msg = new Message("insert",node_id,insert);
    			Message socket = new Message(getPortAddr(receivedNodes.succ)+"");
        		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, socket);
    		}
    	} 
    	catch (NoSuchAlgorithmException e) 
    	{
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	System.out.println(rowId);
    	
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			System.out.println("Insert successful "+node_id);
			return newUri;
		}
		else {
			Log.e(TAG, "Insert to db failed");
			System.out.println("Insert failed");
			 return null;
		}
       
    }
    
    public boolean condition1(String a1,String a2,String a3)
    {
    	return ( a1.compareTo(a2) <= 0 && a1.compareTo(a3) > 0);
    }
    
    public boolean condition2(String a1,String a2,String a3)
    {
    	return ((a1.compareTo(a2) <= 0 || a1.compareTo(a3) > 0) && (node_id.equals(receivedNodes.isOnly)));
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
    	database = new myDatabase(getContext());
		database.getWritableDatabase();
		chord = new TreeMap<String, String>();
		forJoin = new ArrayList<String>();
		ExecutorService serverExecutor= Executors.newSingleThreadExecutor();
		serverExecutor.execute(new Server());
		
		node_id = getPortString();
		//Log.v("SDHTP OnCreate()",node_id);
		System.out.println("SDHTP OnCreate()"+node_id);
		receivedNodes = new Neighbours(node_id, node_id, node_id);
		try
		{
			Message msg = new Message("join", node_id);
			Message socket = new Message("11108");
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, socket);
		}
		catch(Exception e)
		{
			System.out.println("Inside oncreate catch");
			receivedNodes = new Neighbours(node_id, node_id, node_id);
		}

    	
		return true;
    }

    private String getPortString()
    {
    	TelephonyManager telephone = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
    	String portStr = telephone.getLine1Number().substring(telephone.getLine1Number().length() - 4);	
    	final String port = String.valueOf((Integer.parseInt(portStr)));
    	return port;
    }
    
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
    	
    	sqlDb= database.getReadableDatabase();
    	//Cursor cursor= sqlDb.rawQuery("select * from "+myDatabase.TABLE_NAME, null);
    	Cursor cursor = null;
    	if(selection != null)
    	{
    		if(selection.equals("@"))
    		{
    			
    			cursor = sqlDb.query(myDatabase.TABLE_NAME, // a. table
    					projection, // b. column names
    					null, // c. selections 
    					null, // d. selections args
    					null, // e. group by
    					null, // f. having
    					null, // g. order by
    					null); // h. limit
    			System.out.println(selection+" "+cursor.getCount());
    			return cursor;
    		}
    		else if(selection.equals("*"))
    		{
    			cursor = sqlDb.query(myDatabase.TABLE_NAME, // a. table
    					projection, // b. column names
    					null, // c. selections 
    					null, // d. selections args
    					null, // e. group by
    					null, // f. having
    					null, // g. order by
    					null); // h. limit
    			dumpCursorMap = cursorToHash(cursor);
    			cursor =  dumpQuery(dumpCursorMap);
    		}
    		else
    		{
    			
    			System.out.println("Inside query :: if 1");
    			cursor =  singleQuery(selection,projection);
    			/*cursor = sqlDb.query(myDatabase.TABLE_NAME, // a. table
    					projection, // b. column names
    					myDatabase.KEY_FIELD+"=?", // c. selections 
    	                new String[]{selection},
    					null, // e. group by
    					null, // f. having
    					null, // g. order by
    					null); // h. limit
*/    			return cursor;
    		}

    	}
    	else
    	{
    		System.out.println("Inside query :: else");
    		cursor= sqlDb.rawQuery("select * from "+myDatabase.TABLE_NAME, null);
    	}
		return cursor;
    }

    
    public Cursor dumpQuery(Map<String,String> temp)
    {
    	Cursor cursor = null;
    	Message msg = new Message("global_dump",node_id,temp);
		Message socket = new Message(getPortAddr(receivedNodes.succ)+"");
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,socket);
		waitForDumpReply();
		if(dumpCursor != null)
			cursor = dumpCursor;
		return cursor;
    }
    
    public Cursor singleQuery(String key,String[] projection)
    {
    	System.out.println("KEY "+key);
    	String hashKey,hashNode,hashPred;
    	Cursor cursor = null;
    	try 
    	{
    		hashKey = genHash(key);
    		hashNode = genHash(node_id);
    		hashPred= genHash(receivedNodes.pred);
    		
    		System.out.println(node_id+" "+receivedNodes.pred+" "+(hashKey.compareTo(hashNode) <= 0)+" "+(hashKey.compareTo(hashPred) > 0));
    		//if(hashKey.compareTo(hashNode) <= 0 && hashKey.compareTo(hashPred) > 0)
    		if(condition1(hashKey,hashNode,hashPred))
    		{
    			System.out.println("Inside singleQuery :: 1");
    			cursor = sqlDb.query(myDatabase.TABLE_NAME, // a. table
    					projection, // b. column names
    					myDatabase.KEY_FIELD+"=?", // c. selections 
    	                new String[]{key},
    					null, // e. group by
    					null, // f. having
    					null, // g. order by
    					null); // h. limit
    		}
    		//else if((hashKey.compareTo(hashNode) <= 0 || hashKey.compareTo(hashPred) > 0) && node_id.equals(receivedNodes.isOnly))
    		else if(condition2(hashKey,hashNode,hashPred))
    		{
    			System.out.println("Inside singleQuery :: 2");
    			cursor = sqlDb.query(myDatabase.TABLE_NAME, // a. table
    					projection, // b. column names
    					myDatabase.KEY_FIELD+"=?", // c. selections 
    	                new String[]{key},
    					null, // e. group by
    					null, // f. having
    					null, // g. order by
    					null); // h. limit
    			if(cursor.getCount() <= 0)
    			{
    				System.out.println("Inside singleQuery :: 2a");
        			Message msg = new Message("query",node_id,key);
        			Message socket = new Message(getPortAddr(receivedNodes.succ)+"");
        			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,socket);
        			waitForReply();
        			if(replyCursor != null)
        				cursor = replyCursor;
    			}
    		}
    		else
    		{
    			System.out.println("Inside singleQuery :: 3");
    			Message msg = new Message("query",node_id,key);
    			Message socket = new Message(getPortAddr(receivedNodes.succ)+"");
    			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,socket);
    			waitForReply();
    			if(replyCursor != null)
    				cursor = replyCursor;
    		}
    	} 
    	catch (NoSuchAlgorithmException e) 
    	{
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	return cursor;
    }
    
    public void waitForReply()
    {
    	while(true)
    	{
    		if(query_reply_flag)
    		{
    			query_reply_flag = false;
    			break;
    		}
    	}
    }
    
    public void waitForDumpReply()
    {
    	while(true)
    	{
    		if(global_reply_flag)
    		{
    			global_reply_flag = false;
    			break;
    		}
    	}
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    
    public void onJoin(String node)
    {
    	String hash;
    	CircularLinkedList list = new CircularLinkedList();
    	if(!chord.containsValue(node))
    	{
    		try {
				hash = genHash(node);
				chord.put(hash,node);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	for(Entry<String,String> entry : chord.entrySet())
    	{
    		String a = entry.getValue();
    		forJoin.add(a);
    		//list.insertAtEnd(a);
    		//System.out.println("In Map "+a);
    	}
    	
    	System.out.println("Size "+forJoin.size());
    	//sendingJoin(list);
    	sendingJoinArray(forJoin);
    	//list.display();
    	//Log.v("SHIYAM ON JOIN",node);
    }
    
    public void sendingJoinArray(ArrayList<String> list)
    {
    	
    	String port = null;
    	Neighbours nodes;
    //	String isOnly = list.get(0);
    	
    	for(int i=0;i<forJoin.size();i++)
		{
    		port = forJoin.get(i);
    		int portAddr= getPortAddr(port);
    		String isOnly = list.get(0);
    		
			if(i == 0)
			{
				if(forJoin.size() == 1)
				{
					
					//System.out.println("Pred "+forJoin.get(forJoin.size()-1)+" "+forJoin.get(i)+" succ "+forJoin.get(forJoin.size()-1));
					nodes = new Neighbours(forJoin.get(forJoin.size()-1), forJoin.get(forJoin.size()-1), isOnly);
				}
				else
				{
					//System.out.println("Pred "+forJoin.get(forJoin.size()-1)+" "+forJoin.get(i)+" succ "+forJoin.get(i+1));
					nodes = new Neighbours(forJoin.get(forJoin.size()-1), forJoin.get(i+1),isOnly);
				}
			}
			else if(i == forJoin.size()-1)
			{
				//System.out.println("Pred "+forJoin.get(i-1)+" "+forJoin.get(i)+" succ "+forJoin.get(0));
				nodes = new Neighbours(forJoin.get(i-1), forJoin.get(0),isOnly);
			}
			else
			{
				//System.out.println("Pred "+forJoin.get(i-1)+" "+forJoin.get(i)+" succ "+forJoin.get(i+1));
				nodes = new Neighbours(forJoin.get(i-1),forJoin.get(i+1),isOnly);
			}
			
			Message msg= new Message("update_nodes",port,nodes);
    		Message socket = new Message(portAddr+"");
    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, socket);
		}
    	
    	forJoin.clear();
    }
    
    /*public void sendingJoin(CircularLinkedList list)
    {
    	Node temp = list.start.getLinkNext();
    	String port = null;
    	Neighbours nodes;
    	//System.out.println("TEMP "+temp.data+" "+list.start.data);
    	
    	//
    	do
    		//while (temp != list.start) 
        {
    		port = temp.data;
    		int portAddr= getPortAddr(port);
    		nodes = new Neighbours(temp.prev.data, temp.next.data,"");
    		//System.out.println(temp.prev.data+"<-"+temp.data+"->"+temp.next.data);
    		//Log.v("CHORD",temp.prev.data+" "+temp.next.data+" "+portAddr);
    	//	Node eval= list.getNode(port);
    	//	nb[0]= eval.prev.data;
    	//	nb[1]= eval.next.data;
    	//	nb[2] = chord.get(chord.firstKey());
    	//	Message msg= new Message("update",SimpleDhtMainActivity.Node_id ,nb);
    	//	pool.execute(new Send(msg,portAddr));
    		Message msg= new Message("update_nodes",temp.data,nodes);
    		Message socket = new Message(portAddr+"");
    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, socket);
            temp = temp.getLinkNext();
        }while (temp != list.start);
    }*/
    
    public int getPortAddr(String n_id) {
    	if(n_id.equals("5554"))
    		return 11108;
    	else if(n_id.equals("5556"))
    		return 11112;
    	else if(n_id.equals("5558"))
    		return 11116;
    	else if(n_id.equals("5560"))
    		return 11120;
    	else if(n_id.equals("5562"))
    		return 11124;
    	return -1;
    }
    
    
    
 /*********************** FOR MANUAL DISPLAY***************/
    
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
    
    
 /*********************************************************/
    
/******************* SERVER CODE ******************/
    class Receiver implements Runnable {

    	static final String TAG = "Shiyam Receiver";
    	Socket sock= null;
    	Message msg;

    	Receiver (Message s) {
    		this.msg= s;
    	}

    	public void run() {
    		Log.i(TAG, "recvd msg: "+ msg.type);
    		if (msg.type.equals("join")) 
    		{
    			onJoin(msg.node_id);
    		}
    		else if (msg.type.equals("update_nodes")) 
    		{
    			//onJoin(msg.node_id);
    			//System.out.println(msg.nodes.getPred()+" "+msg.node_id+" "+msg.nodes.getSucc());
    			receivedNodes = msg.nodes;
    		}
    		else if(msg.type.equals("insert"))
    		{
    			System.out.println("Insert");
    			ContentValues val = new ContentValues();
    			val.put(myDatabase.KEY_FIELD, msg.contentValues[0]);
    			val.put(myDatabase.VALUE_FIELD, msg.contentValues[1]);
    			//SimpleDhtMainActivity.mContentResolver.insert(SimpleDhtProvider.CONTENT_URI, val);
    			getContext().getContentResolver().insert(SimpleDhtProvider.CONTENT_URI, val);
    		}
    		else if(msg.type.equals("query"))
    		{
    			Map<String,String> cursorMap =  new HashMap<String, String>();
    			System.out.println("Query from "+msg.node_id);
    			Cursor cursor = getContext().getContentResolver().query(SimpleDhtProvider.CONTENT_URI, null, msg.selection, null,null);
    			
    			if(cursor != null)
    			{
    				/*if (cursor.moveToFirst()) 
    	    		{
    	    			while (!cursor.isAfterLast()) 
    	    			{
    	    				int keyIndex = cursor.getColumnIndex("key");
    	    				int valueIndex = cursor.getColumnIndex("value");
    	    				String returnKey = cursor.getString(keyIndex);
    	    				String returnValue = cursor.getString(valueIndex);
    	    				cursorMap.put(returnKey, returnValue);
    	    				cursor.moveToNext();
    	    			}
    	    		}*/
    				cursorMap = cursorToHash(cursor);
    				Message reply = new Message("query_reply",cursorMap);
    				Message socket = new Message(getPortAddr(msg.node_id)+"");
    				System.out.println("Cursor not null");
    				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, reply, socket);
    				
    			}
    		}
    		else if(msg.type.equals("query_reply"))
    		{
    			query_reply_flag = true;
    			cursorMap = msg.map;
    			MatrixCursor mc = new MatrixCursor(new String[]{myDatabase.KEY_FIELD,myDatabase.VALUE_FIELD});
    			for(Entry<String,String> entry : cursorMap.entrySet())
    			{
    				mc.newRow().add(entry.getKey()).add(entry.getValue());
    			}
    			replyCursor = mc;
    			cursorMap.clear();
    			//mc.newRow().add(selection).add(val);
    		}
    		else if(msg.type.equals("global_dump"))
    		{
    			System.out.println("Inside global dump else if");
    			if(msg.node_id.equals(node_id))
    			{
    				System.out.println("Inside global dump else if ( if");
    				global_reply_flag = true;
        			dumpCursorMap = msg.map;
        			MatrixCursor mc = new MatrixCursor(new String[]{myDatabase.KEY_FIELD,myDatabase.VALUE_FIELD});
        			for(Entry<String,String> entry : dumpCursorMap.entrySet())
        			{
        				mc.newRow().add(entry.getKey()).add(entry.getValue());
        			}
        			dumpCursor = mc;
        			dumpCursorMap.clear();
        			//cursorMap.clear();
    			}
    			else
    			{
    				System.out.println("Inside receiver global dump else MSG ID-> "+msg.node_id+" receiver_-> "+node_id);
    				Cursor cursor = getContext().getContentResolver().query(SimpleDhtProvider.CONTENT_URI, null, null, null,null);
    				dumpCursorMap = msg.map;
    				if(cursor != null)
    				{
    					if (cursor.moveToFirst()) 
    					{
    						while (!cursor.isAfterLast()) 
    						{
    							int keyIndex = cursor.getColumnIndex("key");
    							int valueIndex = cursor.getColumnIndex("value");
    							String returnKey = cursor.getString(keyIndex);
    							String returnValue = cursor.getString(valueIndex);
    							dumpCursorMap.put(returnKey, returnValue);
    							cursor.moveToNext();
    						}
    					}
    				}
    				for(Entry<String,String> entry : dumpCursorMap.entrySet())
    				{
    					System.out.println(entry.getKey()+" "+entry.getValue());
    				}
    				Message msg1 = new Message("global_dump",msg.node_id,dumpCursorMap);
    				Message socket = new Message(getPortAddr(receivedNodes.succ)+"");
    				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg1,socket);
    			}
    			
    			//mc.newRow().add(selection).add(val);
    		}
    		else if(msg.type.equals("delete_global"))
    		{
    			deleteGlobalId = msg.node_id;
    			if(!msg.node_id.equals(node_id))
    			{
    				getContext().getContentResolver().delete(SimpleDhtProvider.CONTENT_URI, "*",msg.contentValues);
    			}
    		}
    		else if(msg.type.equals("delete"))
    		{
    			deleteGlobalId = msg.node_id;
    			if(!msg.node_id.equals(node_id))
    			{
    				int a = getContext().getContentResolver().delete(SimpleDhtProvider.CONTENT_URI, msg.selection,null);
    			}
    		}
    	}
    }

    public Map<String,String> cursorToHash(Cursor cursor)
    {
    	Map<String,String> temp = new HashMap<String,String>();
    	if (cursor.moveToFirst()) 
		{
			while (!cursor.isAfterLast()) 
			{
				int keyIndex = cursor.getColumnIndex("key");
				int valueIndex = cursor.getColumnIndex("value");
				String returnKey = cursor.getString(keyIndex);
				String returnValue = cursor.getString(valueIndex);
				temp.put(returnKey, returnValue);
				cursor.moveToNext();
			}
		}
    	return temp;
    	
    }
    
    class Server implements Runnable {

    	static final String TAG = "Shiyam Server";
    	static final int recvPort= 10000;
    	//ExecutorService receiverExecutor= Executors.newFixedThreadPool();

    	public void run() {
    		ObjectInputStream input =null;
    		ServerSocket serverSocket= null;
    		Socket server= null;

    		try {
    			serverSocket= new ServerSocket(recvPort);
    		} catch (IOException e) {
    			Log.e(TAG, ""+e.getMessage());
    		}

    		while(true) {
    			try {
    				server= serverSocket.accept();
    				input =new ObjectInputStream(server.getInputStream());
    				Message obj;
    				try {
    					obj = (Message) input.readObject();
    					Executors.newSingleThreadExecutor().execute(new Receiver(obj)); //replace where to send this object
    				} catch (ClassNotFoundException e) {
    					Log.e(TAG, e.getMessage());
    				}
    			} 

    			catch (IOException e) {
    				Log.e(TAG, ""+e.getMessage());
    				e.printStackTrace();
    			}
    			finally {
    				if (input!= null)
    					try {
    						input.close();
    					} catch (IOException e) {
    						Log.e(TAG, ""+e.getMessage());
    					}
    				if(server!=null)
    					try {
    						server.close();
    					} catch (IOException e) {
    						Log.e(TAG, ""+e.getMessage());
    					}	
    			}
    		}
    	}    
    
    }
    
/**************************************************/
    
    
    /************* CLIENT *************/
    
    public class ClientTask extends AsyncTask<Message, Void, Void> 
    {

        @Override
        protected Void doInBackground(Message... msgs) {
            try {
                String remotePort = msgs[1].socket;

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
            	receivedNodes = new Neighbours(node_id, node_id, node_id);
                //Log.e("Client", "ClientTask UnknownHostException");
            } catch (IOException e) {
                //Log.e("Client", "ClientTask socket IOException");
            	receivedNodes = new Neighbours(node_id, node_id, node_id);
            }

            return null;
        }

		public void executeOnExecutor(Executor serialExecutor, Message m,
				String string) {
			// TODO Auto-generated method stub
			
		}
    }
    
    /**********************************/
   
}
