package edu.buffalo.cse.cse486586.simpledht;

class Node
{
    protected String data;
    protected Node next, prev;
 
    /* Constructor */
    public Node()
    {
        next = null;
        prev = null;
        data = null;
    }
    /* Constructor */
    public Node(String d, Node n, Node p)
    {
        data = d;
        next = n;
        prev = p;
    }
    /* Function to set link to next node */
    public void setLinkNext(Node n)
    {
        next = n;
    }
    /* Function to set link to previous node */
    public void setLinkPrev(Node p)
    {
        prev = p;
    }    
    /* Funtion to get link to next node */
    public Node getLinkNext()
    {
        return next;
    }
    /* Function to get link to previous node */
    public Node getLinkPrev()
    {
        return prev;
    }
    /* Function to set data to node */
    public void setData(String d)
    {
        data = d;
    }
    /* Function to get data from node */
    public String getData()
    {
        return data;
    }
}
 
/* Class linkedList */
class CircularLinkedList
{
    protected Node start;
    protected Node end ;
    public int size;
 
    /* Constructor */
    public CircularLinkedList()
    {
        start = null;
        end = null;
        size = 0;
    }
    /* Function to check if list is empty */
    public boolean isEmpty()
    {
        return start == null;
    }
    /* Function to get size of list */
    public int getSize()
    {
        return size;
    }
    
    public void insertAtEnd(String val)
    {
        Node nptr = new Node(val, null, null);        
        if (start == null)
        {
            nptr.setLinkNext(nptr);
            nptr.setLinkPrev(nptr);
            start = nptr;
            end = start;
        }
        else
        {
            nptr.setLinkPrev(end);
            end.setLinkNext(nptr);
            start.setLinkPrev(nptr);
            nptr.setLinkNext(start);
            end = nptr;    
        }
        size++;
    }
    
    public Node getNode(String avd)
    {
    	/*if( start != null ) {
        	Node itr = start; 
            do {
                if( itr.data.equals( avd ) ) {
                    return itr;
                }
                itr = itr.next;
            } while( itr != start );
        }
    	*/
    	if(start != null)
    	{
    		Node temp = start;
    		while (temp.getLinkNext() != start) 
            {
    			if( temp.data.equals( avd ) )
    			{
                    return temp;
    			}
    			temp = temp.getLinkNext();
            }
    			
    	}

    	return null;
    }
 
    /* Function to display status of list */
    public void display()
    {
      //  System.out.print("\nCircular Doubly Linked List = ");
        Node ptr = start;
        if (size == 0) 
        {
            System.out.print("empty\n");
            return;
        }
        if (start.getLinkNext() == start) 
        {
            System.out.print(start.getData()+ " <-> "+ptr.getData()+ "\n");
            return;
        }
        System.out.print(start.getData()+ " <-> ");
        ptr = start.getLinkNext();
        while (ptr.getLinkNext() != start) 
        {
            System.out.print(ptr.getData()+ " <-> ");
            ptr = ptr.getLinkNext();
        }
        System.out.print(ptr.getData()+ " <-> ");
        ptr = ptr.getLinkNext();
        System.out.print(ptr.getData()+ "\n");
    }
  /*  
    public static void main(String[] args)
    {            
        LinkedList2 list = new LinkedList2(); 
        System.out.println("Circular Doubly Linked List Test\n");          

        int a[] = {1,2,3,4,5};
        
        for(int i=0;i<a.length;i++)
        {
        	list.insertAtEnd(a[i]);
        }
        list.display();
        
    }*/
    
}