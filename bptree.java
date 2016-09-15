//Implementation of B+tree

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class NewThread extends Thread
{
	int n,k;
	static int tn=1;
	NewThread(int num)
	{
		super("Thread");
		k=tn++;
		this.n=num;
		start();
	}

	public void run()
	{
		int r1;
		Random r=new Random();
		try
		{
			for(int i=0;i<n;i++)
				{
					r1=r.nextInt(100000);
					while(node.search(node.main_root,r1))
						r1=r.nextInt(100000);
					System.out.println("T"+k+" "+(i+1)+". "+r1);
					node.main_root.insert(node.main_root,r1);
				}
		}
		catch(InterruptedException e){
			System.out.println(e);}
	}
}

class node
{
	int x[],k,n;
	node link[],flink,blink,parent;
	Lock lock;
	boolean locked;
	static int order,count,c=1;
	static node main_root;
	node(node parent)
	{
		n=c++;
		x=new int[order+1];
		link=new node[order+2];
		k=0;
		for(int i=0;i<order+2;i++)
			link[i]=null;
		flink=null;
		blink=null;
		locked=false;
		this.parent=parent;
		lock=new ReentrantLock();
	}

	static boolean leaf(node root)
	{
		for(int i=0;i<=order;i++)
			if(root.link[i]!=null)
				return false;
		return true;
	}

	static void sort_num(node root)
	{
		int temp;
		for(int i=0;i<root.k;i++)
			for(int j=0;j<root.k-i-1;j++)
				if(root.x[j] > root.x[j+1])
				{
					temp=root.x[j];
					root.x[j]=root.x[j+1];
					root.x[j+1]=temp;
				}
	}

	static void sort_links(node root)
	{
		node temp;
		for(int i=0;i<=root.k;i++)
			for(int j=0;j<=root.k-i-1;j++)
				if(root.link[j].x[0] > root.link[j+1].x[0])
				{
					temp=root.link[j];
					root.link[j]=root.link[j+1];
					root.link[j+1]=temp;
				}
	}

	static void add_num(node root,int num)
	{
		root.x[root.k++]=num;
		sort_num(root);
		if(!leaf(root))
			sort_links(root);
		if(root.k<=order)
			root.Unlock();
	}

	static void split_add_num(node child,node root,int k1,int k2)
	{
		for(int i=k1;i<=k2;i++)
			child.x[child.k++]=root.x[i];
	}

	static void split_add_links(node child,node root,int k1,int k2)
	{
		int i=0;
		for(int j=k1;j<=k2;j++)
			child.link[i++]=root.link[j];
		if(!leaf(child))
			for(i=0;i<=child.k;i++)
				child.link[i].parent=child;
	}

	static void remove_link(node root)
	{
		root.k=order/2;
		if(leaf(root))
			return;
		for(int k=order/2+1;k<order+2;k++)
			root.link[k]=null;
	}

	static void add_links(node root,node link,int num)
	{
		int i;
		for(i=0;i<order+2;i++)
			if(root.link[i]==null)
				break;
		root.link[i]=link;
		link.parent=root;
		add_num(root,num);
	}

	static void display_node(node root,char c)
	{
		System.out.print(c+":");
		for(int i=0;i<root.k;i++)
			System.out.print(root.x[i]+" ");
		System.out.println();
	}

	void Lock()
	{
		//System.out.println("Try Locking: "+this.n+" "+ Thread.currentThread().getName());
		this.lock.lock();
		locked=true;
		//System.out.println("Locked: "+this.n+" "+ Thread.currentThread().getName());
	}

	void Unlock()
	{
		if(!locked)
			return;
		locked=false;
		this.lock.unlock();
		//System.out.println("Unlocked: "+this.n+" "+ Thread.currentThread().getName());
	}

	static node splitnode(node root)
	{
		node temp=root,parent=root.parent;
		if(parent==null)
		{
			parent=new node(null);
			parent.link[0]=root;
			root.parent=parent;
		}
		parent.Lock();
		node child=new node(parent);
		if(leaf(root))
		{
			child.Lock();
			add_num(child,root.x[order/2]);
		}
		split_add_num(child,temp,order/2+1,order);
		split_add_links(child,temp,order/2+1,order+1);
		child.flink=root.flink;
		root.flink=child;
		child.blink=root;
		if(child.flink!=null)
			child.flink.blink=child;
		remove_link(root);
		node parent1=parent;
		while(parent1.flink!=null && parent1.flink.x[0] < root.x[order/2])
			parent1=parent1.flink;
		if(parent!=parent1)
		{
			parent.Unlock();
			parent1.Lock();
		}
		add_links(parent1,child,root.x[order/2]);
		root.Unlock();
		return parent1;
	}

	static void insert(node root,int num) throws InterruptedException
	{
		int flag1=0;
		root.Lock();
		while(!leaf(root))
		{
			flag1=0;
			{
				for(int i=0;i<root.k;i++)
					if(num < root.x[i])
					{
						root.Unlock();
						root=root.link[i];
						root.Lock();
						flag1=1;
						break;
					}
				if(flag1==0 && num >= root.x[root.k-1])
				{
					root.Unlock();
					root=root.link[root.k];
					root.Lock();
				}
			}
		}
		node root1=root;
		while(root.flink!=null && root.flink.x[0] < num)
			root=root.flink;
		if(root!=root1)
		{
			root1.Unlock();
			root.Lock();
		}
		add_num(root,num);
		while(root!=null && root.k > order)
		{
			root1=root;
			root=splitnode(root);
		}
		while(root1.parent!=null)
			root1=root1.parent;
		main_root=root1;
	}

	static void print_node(node root,BufferedWriter out) throws IOException
	{
		out.write(root.n+"[label=");
		out.write("\"");
		for(int i=0;i<root.k;i++)
			out.write(root.x[i]+" ");
		out.write("\"];\n");
	}

	static void image(node root,BufferedWriter out) throws IOException
	{
		if(!leaf(root))
		{
			print_node(root,out);
			for(int i=0;i<=root.k;i++)
				out.write(root.n +"->" + root.link[i].n+";\n");
			for(int i=0;i<=root.k;i++)
				image(root.link[i],out);
		}
		else
			print_node(root,out);
	}

	static void dump(node root) throws IOException
	{
		BufferedWriter out= new BufferedWriter(new FileWriter("output.dot",true));
		out.write("\ndigraph{\n\n");
		image(root,out);
		out.write("\n}\n");
		out.close();
		Runtime.getRuntime().exec("dot -Tps -O output.dot");
		Runtime.getRuntime().exec("evince output.dot.ps");
	}

	static void sort_display(node root)
	{
		int count=0;
		while(!leaf(root))
			root=root.link[0];
		while(root!=null)
		{
			for(int i=0;i<root.k;i++)
			{
				System.out.print(root.x[i]+" ");
				count++;
			}
			root=root.flink;
		}
		System.out.println("--("+count+" elements )");
	}

	static boolean search(node root,int num)
	{
		int flag1=0;
		root.Lock();
		while(!leaf(root))
		{
			flag1=0;
			for(int i=0;i<root.k;i++)
				if(num < root.x[i])
				{
					//root.link[i].Lock();
					root.Unlock();
					root=root.link[i];
					root.Lock();
					flag1=1;
					break;
				}
			if(flag1==0 && num >= root.x[root.k-1])
			{
				//root.link[root.k].Lock();
				root.Unlock();
				root=root.link[root.k];
				root.Lock();
			}
		}
		for(int i=0;i<root.k;i++)
			if(root.x[i]==num)
			{
				root.Unlock();
				return true;
			}
		root.Unlock();
		return false;
	}

	static boolean verify_node(node root,int n)
	{
		if(root==null)
			return true;
		for(int i=0;i<root.k;i++)
			if(root.x[i] > n)
				return false;
		return true;
	}

	static void verify(node root) throws IOException
	{
		if(root==null)
			return;
		for(int i=0;i<root.k;i++)
			if(!(verify_node(root.link[i],root.x[i])))
			{
				display_node(root,'a');
				System.out.println("Not correct");
				dump(root);
			}
		if(!leaf(root))
			for(int i=0;i<root.link[root.k].k;i++)
				if(root.link[root.k].x[i] < root.x[root.k - 1])
				{
					System.out.println("Not correct");
					dump(root);
				}
		for(int i=0;i<=root.k;i++)
			verify(root.link[i]);
	}
}

class bptree3
{
	public static void main(String args[]) throws IOException,InterruptedException
	{
		BufferedWriter bw= new BufferedWriter(new FileWriter("output.dot"));
		bw.close();
		Scanner k=new Scanner(System.in);
		System.out.print("Enter no. of keys: ");
		node.order=k.nextInt();
		System.out.print("Enter no. of Threads: ");
		int nt=k.nextInt();
		Random r=new Random();
		node.main_root=new node(null);
		int r1;
		while(true)
		{
			System.out.print("\n----Menu----\n1.Insert\n2.Insert n\n3.Display\n4.Printed Sort\n5.Search\n6.Verify\n7.Exit\nEnter choice: ");
			switch(k.nextInt())
			{
				case 1:
				System.out.print("Enter the number: ");
				r1=k.nextInt();
				if(!(node.search(node.main_root,r1)))
					new NewThread(r1);
				else
					System.out.println("Element already present");
				node.verify(node.main_root);
				break;

				case 2:
				System.out.print("Enter n: ");
				int n=k.nextInt();
				for(int i=0;i<nt-1;i++)
					new NewThread(n/nt);
				for(int i=0;i<n-(nt-1)*(n/nt);i++)
				{
					r1=r.nextInt(100000);
					while(node.search(node.main_root,r1))
						r1=r.nextInt(100000);
					System.out.println("M1 "+(i+1)+". "+r1);
					node.main_root.insert(node.main_root,r1);
				}
				break;

				case 3:
				node.dump(node.main_root);
				break;

				case 4:
				node.sort_display(node.main_root);
				break;

				case 5:
				System.out.print("Enter n: ");
				r1=k.nextInt();
				if(node.search(node.main_root,r1))
					System.out.println("Found");
				else
					System.out.println("Not found");
				break;

				case 6:
				node.verify(node.main_root);
				break;

				case 7:
				System.exit(0);
			}
		}
	}
}
