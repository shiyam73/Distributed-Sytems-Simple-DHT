package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import android.database.Cursor;

class Message implements Serializable {

	
	private static final long serialVersionUID = 1L;
	public String msg;
	public String type;
	public String socket;
	public String node_id;
	public Neighbours nodes;
	public String[] contentValues;
	public String selection;
	public Map<String,String> map;
	
	
	Message(String type, String node_id)
	{
		this.type= type;
		this.node_id= node_id;
	}
	
	Message(String type,String node_id, Neighbours nodes)
	{
		
		this.type= type;
		this.nodes= nodes;
		this.node_id= node_id;
	}
	
	Message(String type,String node_id,String[] insert)
	{
		this.type= type;
		this.node_id= node_id;
		this.contentValues = insert;
	}
	
	Message(String type,String node_id,String selection)
	{
		this.type= type;
		this.node_id= node_id;
		this.selection = selection;
	}
	
	Message (String type,Map<String,String> map)
	{
		this.type= type;
		this.map = map;
	}
	
	Message (String type,String node_id,Map<String,String> map)
	{
		this.type= type;
		this.node_id= node_id;
		this.map = map;
	}
	
	Message(String socket)
	{
		this.socket = socket;
	}
}