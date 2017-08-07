

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
 * @author Mohit
 */
public class ThreadG extends Thread {
    
    int Sw = 4;
    int Sf =0;
    int Sn=0;
    int Sr =0;
    int maxDelay = 1000;
    ArrayList<DatagramPacket> d;
    public static byte[] RDT = new byte[] { 0x52, 0x44, 0x54 };
    public static byte[] SEQ_0 = new byte[] { 0x30 };
    public static byte[] SEQ_1 = new byte[] { 0x31 };
    public static byte[] END = new byte[] { 0x45, 0x4e, 0x44 };
    public static byte[] CRLF = new byte[] { 0x0a, 0x0d };
    public static int CONSIGNMENT = 512;
    private DatagramPacket [] storePkt;
    private Timer timer;
    
    String filename1;  int i;InetAddress srcAddr; int srcPort; DatagramSocket socket;
   
   
    ThreadG(String filename, int i, InetAddress srcAddr, int srcPort,DatagramSocket socket) {
        this.storePkt = new DatagramPacket[4];
        this.filename1=System.getProperty("user.dir") + "\\"+filename;
        this.i=i;
        
            this.socket= socket;
        
        this.srcAddr=srcAddr;
        this.srcPort=srcPort;
        
    }
        
    @Override
    public void run(){
         
            // System.out.println("Client "+srcAddr+":"+srcPort+" Requested file : "+filename1+"\n");
            // System.out.println("\nInitiating file transfer------");
            
             sendFile(filename1);
        }

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
            //System.out.println(nPkt+"nPkt");
            for(int i=0;i<nPkt;i++){
                
                
               temp = Integer.valueOf(i).byteValue();
                SEQ_0[0]=temp;
                
                bytesRead = myFIS.read(myData);
                //System.out.println("data consignment has " + bytesRead + " bytes");  
                
                if(i!=(nPkt-1))
                {
                    myMsg = concatenateByteArrays(RDT, SEQ_0, myData, CRLF);
                }
                else
                {
                    //System.out.println(bytesRead+"Byets");
                    myLastData = new byte[bytesRead];
                   System.arraycopy(myData, 0, myLastData, 0, bytesRead);
                        
                  myMsg = concatenateByteArrays(RDT, SEQ_0, myLastData, END, CRLF);
                }
                
                //printlnBytesAsHex(myMsg);
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
       
     int maxPacket = d.size() - 1;
     int lastSeqNum = 0;
    int multiplier = 0;
       
	DatagramPacket inPacket;
  
        while(Sr <= maxPacket) {
            
            if(Sn<(Sw*(multiplier+1)) && Sn<=maxPacket){
                if(!FServerG.CONSIGNMENT_TO_FORGET.contains(Sn)){
                    
                    try {
                        socket.send(d.get(Sn));
                        //printlnBytesAsHex(packet.getData());
                       
                        System.out.println("\nSENT CONSIGNMENT "+Sn+"\n");
                        
                    } catch (IOException ex) {
                        Logger.getLogger(FServerG.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    
                }
                else{
                    System.out.println("Forgot CONSIGNMENT "+Sn);
                }
                if(Sr == Sn) {
                   // System.out.println("Starting timer!");
                    timer.cancel();
                  timer=new Timer();
		  timer.schedule(new Timeout(), 30);
		}
                Sn++;
                
            }
            else
            {
                byte[] inBuffer = new byte[100];
	     inPacket= new DatagramPacket(inBuffer,inBuffer.length);
                try {
                    socket.receive(inPacket);
                } catch (IOException ex) {
                    Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
                }
                
             String ack = new String(inPacket.getData(), 0 , inPacket.getLength());
             
             if(ack.contains("END"))
             {
                 //System.out.println("Cancelling timer!");
                 timer.cancel();
                 System.out.println("END");
                System.exit(1);
                // break;
             }
              
             Byte t = inPacket.getData()[3];
             int ack_no =t.intValue() ;
            
              System.out.println("\n\nRecieved ACK "+ack_no);
              
              if(ack_no==Sn){
                  multiplier++;
                   System.out.println("\n\nShift Window\n\n");
                  timer.cancel();
              }
              if(ack_no>=lastSeqNum ){
              Sr = ack_no; 
              
                  //System.out.println("Starting timer!");
                  timer.cancel();
                  timer=new Timer();
                  timer.schedule(new Timeout(), 30);
              
              lastSeqNum = ack_no;
              }
             if(Sr-1 == maxPacket) break;  
            
            }
        }
	
	timer.cancel();
        
        System.out.close();
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
    
    class Timeout extends TimerTask {
        
    Thread t;
    
    
    
    @Override
    public void run() {
	  // Restart timer
          Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
          timer.cancel();
          timer = new Timer();
      timer.schedule(new Timeout(), 30);
        
            System.out.println(" ");
            System.out.println("Timeout");
	  //System.out.println("Resending all unacknowledged packets");
	  for(int i = Sr; i <= Sn - 1; i++) {
	    try {
	      
		  socket.send(d.get(i));
                  System.out.println("\n\nSENT CONSIGNMENT "+i+"\n");
	    } catch (IOException ex) { 
              Logger.getLogger(ThreadG.class.getName()).log(Level.SEVERE, null, ex);
          }
            
            
	  }
          
    }
  }
    
}
