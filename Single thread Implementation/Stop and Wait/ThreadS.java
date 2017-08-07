import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
public class ThreadS extends Thread  {
    
    public static byte[] RDT = new byte[] { 0x52, 0x44, 0x54 };
    public static byte[] SEQ_0 = new byte[] { 0x30 };
    public static byte[] SEQ_1 = new byte[] { 0x31 };
    public static byte[] END = new byte[] { 0x45, 0x4e, 0x44 };
    public static byte[] CRLF = new byte[] { 0x0a, 0x0d };
    public static int CONSIGNMENT = 512;
    
    String filename1;  int i;InetAddress srcAddr; int srcPort; DatagramSocket socket;
    
   File f;FileOutputStream fos = null;
            
    ThreadS(String filename, int i, InetAddress srcAddr, int srcPort,DatagramSocket socket) {
        this.filename1=System.getProperty("user.dir") + "\\"+filename;
        this.i=i;
       
            this.socket= socket;
        
        this.srcAddr=srcAddr;
        this.srcPort=srcPort;
        
        
    }
    
    @Override
    public void run() 
     {                  
            // System.out.println("Received request for "+filename1+" from "+srcAddr+" port "+srcPort+"\n");
             
             //System.out.println("\nInitiating file transfer------");
        
             sendFile(filename1); 
        
     }
    
    public void sendFile(String fileName1){
        
        File fileName = new File(fileName1);
        int bytesRead;
        String ack;
        int nPkt;
        int ack_no;
        Byte temp;
        byte[] myData = new byte[CONSIGNMENT];
        byte[] myMsg;
        byte[] myLastData;
         
        try {
            FileInputStream myFIS = new FileInputStream(fileName);
            
            nPkt = (int) Math.ceil(fileName.length()/((float)CONSIGNMENT));
            //System.out.println(nPkt+"nPkt");
            for(int i=0;i<nPkt;i++){
                
                temp =Integer.valueOf(i).byteValue();
                SEQ_0[0]=temp;
                
                bytesRead = myFIS.read(myData);
                //System.out.println("data consignment has " + bytesRead + " bytes");  
                
                if(i!=(nPkt-1))
                {
                    
                    myMsg = concatenateByteArrays(RDT, SEQ_0 , myData, CRLF);
                }
                else
                {
                    //System.out.println(bytesRead+"Byets");
                    myLastData = new byte[bytesRead];
                        for (int j=0; j<bytesRead; j++) {
                            myLastData[j] = myData[j];
                        }
                        
                  myMsg = concatenateByteArrays(RDT, SEQ_0 , myLastData, END, CRLF);
                }
                
                //printlnBytesAsHex(myMsg);
                DatagramPacket outPacket = new DatagramPacket(myMsg, 0, myMsg.length,srcAddr,srcPort);
               
                int pktno = i;
                sendPacket(outPacket,i,pktno,temp);
              
            }
            socket.setSoTimeout(0);
            System.out.println("END");
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
     public void sendPacket(DatagramPacket packet,int i,int pktno,Byte temp){
        
        while(true)
        {
            if(!FServer.CONSIGNMENT_TO_FORGET.contains(i))
                {
                    try {
                            socket.send(packet);
                            //printlnBytesAsHex(packet.getData());
                              System.out.println("\nSENT CONSIGNMENT "+pktno+"\n");
                        
                        } catch (IOException ex) {
                            Logger.getLogger(FServer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                     
                }
                 
            else {
                System.out.println("\nForgot CONSIGNMENT "+i+"\n");
                i=-1;
            }
          
            byte[] inBuffer = new byte[100];
            DatagramPacket inPacket = new DatagramPacket(inBuffer,inBuffer.length);
             try{
              socket.setSoTimeout(2000);
              socket.receive(inPacket);
             
             String ack = new String(inPacket.getData(), 0 , inPacket.getLength());
             
             if(ack.contains("END"))
             {
                 return;
             }
             
             int ack_no = new Byte(inPacket.getData()[3]).intValue();
             int temp_no =temp.intValue();
              System.out.println("\n\nRecieved ACK "+ack_no+"\n");
             if(ack_no==(temp_no+1)){
                 break;
                 
             }
             System.out.println("\nRepeating Previous CONSIGNMENT "+pktno+"\n");
             
             }
             catch(SocketTimeoutException ex){
                 System.out.println("\n***Time out Occured***\n");
                 System.out.println("\nRepeating previous consignment\n");
                    
             } catch (IOException ex) {
            Logger.getLogger(FServer.class.getName()).log(Level.SEVERE, null, ex);
        }
           
        }
      
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
  
}
