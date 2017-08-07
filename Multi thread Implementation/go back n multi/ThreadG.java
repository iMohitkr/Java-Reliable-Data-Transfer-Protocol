

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Mohit and Ranu
 */
public class ThreadG extends Thread {
    
    int Sw = 4; //Sw stores the window size 
    int Sf =0; // Sf stores the index value of the first packet in the current window
    int Sn=0; // Sn stores the last packet send during the window
    int Sr =0; // Sr stores the least index of packets whos ack is yet to be recieved in the current window
    int maxDelay = 30; // to wait before retransferring
    
	ArrayList<DatagramPacket> d;
	
    public static byte[] RDT = new byte[] { 0x52, 0x44, 0x54 };
    public static byte[] SEQ_0 = new byte[] { 0x30 };
    public static byte[] SEQ_1 = new byte[] { 0x31 };
    public static byte[] END = new byte[] { 0x45, 0x4e, 0x44 };
    public static byte[] CRLF = new byte[] { 0x0a, 0x0d };
    public static int CONSIGNMENT = 512;

    private Timer timer;
    
    String filename1;  int i;InetAddress srcAddr; int srcPort; DatagramSocket socket;
   File f;FileOutputStream fos = null; FileWriter g;
            BufferedWriter pw=null;
   
   //constructor to provide this thread the addresses of the client , filename requested , and the thread number
    ThreadG(String filename, int i, InetAddress srcAddr, int srcPort) {
        
		
		//to compile and run it in linux replace the dir symbol to a forward slash in the below statement
        this.filename1=System.getProperty("user.dir") + "\\"+filename;
        this.i=i;
        
        try {
            this.socket= new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
		
		//creating a log file to store the server side output of the file transfer
        f = new File("log Output"+i+".txt");
        while(f.exists()){
           i++;
           f = new File("log Output"+i+".txt");
        }
        try {
            fos = new FileOutputStream(f);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            g= new FileWriter(f);
        } catch (IOException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
             pw = new BufferedWriter(g);
        
        this.srcAddr=srcAddr;
        this.srcPort=srcPort;
        
    }
        
    @Override
    public void run(){
        
            
        
             
        try {
            
            pw.write("Client "+srcAddr+":"+srcPort+" Requested file : "+filename1+"\n");
        } catch (IOException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
             
        try {
            pw.newLine();
            pw.write("\nInitiating file transfer------");
        } catch (IOException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
             
             //initiating the send file
             sendFile(filename1);
       
             
        try {
            pw.flush();
        } catch (IOException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            pw.close();
			g.close();
			fos.close();
        } catch (IOException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
             socket.close();
             
       
        }

        
    
    //sendFile function seriralizes the file to be send into packets and store them in a 
	//temporary buffer and then calls the sendPacket function to send the packets
	
     public void sendFile(String fileName1){
        
        File fileName = new File(fileName1);
        d = new ArrayList<>();
        int bytesRead;
        String ack;
        int nPkt;
        int ack_no;
        Byte temp;
        byte[] myData = new byte[CONSIGNMENT];
         byte[] myMsg;
         byte[] myLastData;
         timer = new Timer();
        try {
            FileInputStream myFIS = new FileInputStream(fileName);
            java.io.DataInputStream mk = new java.io.DataInputStream(myFIS);
            
            nPkt = (int) Math.ceil(fileName.length()/((float)CONSIGNMENT));
            //pw.write(nPkt+"nPkt");
            for(int i=0;i<nPkt;i++){
                
                
               temp = Integer.valueOf(i).byteValue();
                SEQ_0[0]=temp;
                
                bytesRead = myFIS.read(myData);
                //pw.write("data consignment has " + bytesRead + " bytes");  
                
                if(i!=(nPkt-1))
                {
                    
                    myMsg = concatenateByteArrays(RDT, SEQ_0, myData, CRLF);
                }
                else
                {
                    //pw.write(bytesRead+"Byets");
                    myLastData = new byte[bytesRead];
                   System.arraycopy(myData, 0, myLastData, 0, bytesRead);
                        
                  myMsg = concatenateByteArrays(RDT, SEQ_0, myLastData, END, CRLF);
                }
                
                //writerBytesAsHex(myMsg);
                DatagramPacket outPacket = new DatagramPacket(myMsg, 0, myMsg.length,srcAddr,srcPort);
                
                
                d.add(outPacket);
                //sendPacket(outPacket,i,pktno,temp);
             
             
                
                
                
            }
            
            sendPacket();
            
        
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FServerG.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FServerG.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
 public void sendPacket(){
        
     
     int maxPacket = d.size() - 1; //store the last index of the temporary packet array created for the file
     int lastSeqNum = 0; // store the seq number of the last ack recieved
    int multiplier = 0; // stores the value of the window number 
       
	DatagramPacket inPacket; 
  
        while(Sr <= maxPacket) {  //to loop over the entire array of the packet array 'd'
            
            if(Sn<(Sw*(multiplier+1)) && Sn<=maxPacket){  //checking if the index belongs to the current window
                if(!FServerG.CONSIGNMENT_TO_FORGET.contains(Sn)){ // checking whether to send the packet or to forget the packet
                    
                    try {
                        socket.send(d.get(Sn));
                        //writerBytesAsHex(packet.getData());
                       pw.newLine();
                        pw.write("\nSENT CONSIGNMENT "+Sn+"\n");
                        
                        
                        
                        
                    } catch (IOException ex) {
                        Logger.getLogger(FServerG.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    
                }
                else{
                    try {
                        pw.newLine();
                        pw.write(new StringBuilder().append("Forget Consignment ").append(Sn).toString());
                    } catch (IOException ex) {
                        Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if(Sr == Sn) { // if it is the first packet of the window then starting the timer 
                    try {
                        pw.newLine();
                        pw.write("Starting timer!");
                    } catch (IOException ex) {
                        Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    timer.cancel();
                  timer=new Timer();
		  timer.schedule(new Timeout(), maxDelay);
		}
                Sn++;
                
            }
            else
            {
				
				//this else part is for recieving the acks
				
                byte[] inBuffer = new byte[100];
	     inPacket= new DatagramPacket(inBuffer,inBuffer.length);
                try {
                    socket.receive(inPacket);
                } catch (IOException ex) {
                    Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
                }
                
             String ack = new String(inPacket.getData(), 0 , inPacket.getLength());
             
             if(ack.contains("END")) //if it is the last ack
             {
                 timer.cancel();
                    try {
                        pw.newLine();
                        pw.write("END");
                    } catch (IOException ex) {
                        Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
                    }
                 break;
                 
             }
              
             Byte t = inPacket.getData()[3];
             int ack_no =t.intValue() ;
            
                try {
                    pw.newLine();
                    pw.write("\n\nRecieved acknowledgement "+ack_no);
                } catch (IOException ex) {
                    Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
                }
              
              if(ack_no==Sn){
                  multiplier++;
                    try {
                        pw.newLine();
                        pw.write("Cancelling timer!");
                    } catch (IOException ex) {
                        Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
                    }
                  timer.cancel();
              }
              
              
              
              if(ack_no>=lastSeqNum ){
              Sr = ack_no;  //updating the Sr to point to the next packet whos ack is needed 
              
                    try {
                        pw.newLine();
                        pw.write("Starting timer!");
                    } catch (IOException ex) {
                        Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
                    }
                  timer.cancel();
                  timer=new Timer();
                  timer.schedule(new Timeout(), maxDelay);
              
              lastSeqNum = ack_no;
              
              }
              
             
             if(Sr-1 == maxPacket) break;  
            
            }
            
           
            
        }
        
        
	
	timer.cancel();
        
      
     
             
    }
    
     
     
     
     
     public static byte[] concatenateByteArrays(byte[] a, byte[] b, byte[] c, byte[] d) {
        byte[] result = new byte[a.length + b.length + c.length + d.length]; 
        System.arraycopy(a, 0, result, 0, a.length); 
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length+b.length, c.length);
        System.arraycopy(d, 0, result, a.length+b.length+c.length, d.length);
        return result;
    }
    
    public static byte[] concatenateByteArrays(byte[] a, byte[] b, byte[] c, byte[] d, byte[] e) {
        byte[] result = new byte[a.length + b.length + c.length + d.length + e.length]; 
        System.arraycopy(a, 0, result, 0, a.length); 
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length+b.length, c.length);
        System.arraycopy(d, 0, result, a.length+b.length+c.length, d.length);
        System.arraycopy(e, 0, result, a.length+b.length+c.length+d.length, e.length);
        return result;
    }
    
	
	//creating a timer task to handle the timeouts
    class Timeout extends TimerTask {
        
    Thread t;
    
    
    
    @Override
    public void run() {
	  // Restart timer
          Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
          timer.cancel();
          timer = new Timer();
      timer.schedule(new Timeout(), maxDelay);
        
        try {
            pw.newLine();
            pw.write(" ");
        } catch (IOException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            pw.newLine();
            pw.write("Time out occurred!!!!");
        } catch (IOException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            pw.newLine();
            pw.write("Resending all unacknowledged packets");
        } catch (IOException ex) {
            Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
        }
		
		//send all the outstanding packets whos acks have not been recieved yet
	  for(int i = Sr; i <= Sn - 1; i++) {
	    try {
	      
		  socket.send(d.get(i));
                  pw.newLine();
                  pw.write("\nRESENT CONSIGNMENT "+i+"\n");
	    } catch (IOException ex) { 
              Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
          }
            
            
	  }
          
    }
  }
    
}
