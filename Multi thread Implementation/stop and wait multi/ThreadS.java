import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
    
 File f;FileOutputStream fos = null; FileWriter g;
            BufferedWriter pw=null;
            
    ThreadS(String filename, int i, InetAddress srcAddr, int srcPort) {
        this.filename1=System.getProperty("user.dir") + "\\"+filename;
        this.i=i;
       
        try {
            this.socket= new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(ThreadS.class.getName()).log(Level.SEVERE, null, ex);
        }
        
            f = new File("log Output"+i+".txt");
        while(f.exists()){
           i++;
           f = new File("log Output"+i+".txt");
        }
        try {
            fos = new FileOutputStream(f);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ThreadS.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            g= new FileWriter(f);
        } catch (IOException ex) {
            Logger.getLogger(ThreadS.class.getName()).log(Level.SEVERE, null, ex);
        }
             pw = new BufferedWriter(g);
        this.srcAddr=srcAddr;
        this.srcPort=srcPort;
        
        
    }
    
    @Override
    public void run() 
     {                  
        try {
            pw.write("Received request for "+filename1+" from "+srcAddr+" port "+srcPort+"\n");
            pw.newLine();
            pw.write("\nInitiating file transfer------");
            
            sendFile(filename1);
            
            pw.flush();
            pw.close();
            g.close();
            fos.close();
            
        } catch (IOException ex) {
            Logger.getLogger(ThreadS.class.getName()).log(Level.SEVERE, null, ex);
        }
        
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
            //pw.write(nPkt+"nPkt");
            for(int i=0;i<nPkt;i++){
                
                temp =Integer.valueOf(i).byteValue();
                SEQ_0[0]=temp;
                
                bytesRead = myFIS.read(myData);
                //pw.write("data consignment has " + bytesRead + " bytes");  
                
                if(i!=(nPkt-1))
                {
                    
                    myMsg = concatenateByteArrays(RDT, SEQ_0 , myData, CRLF);
                }
                else
                {
                    //pw.write(bytesRead+"Byets");
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
            pw.newLine();
            pw.write("END");
            
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
                            pw.newLine();
                              pw.write("\nSENT CONSIGNMENT "+pktno+"\n");
                              pw.newLine();
                        
                        } catch (IOException ex) {
                            Logger.getLogger(FServer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                     
                }
                 
            else {
                try {
                    pw.newLine();
                    pw.write("\nForgot CONSIGNMENT "+i+"\n");
                    pw.newLine();
                    i=-1;
                } catch (IOException ex) {
                    Logger.getLogger(ThreadS.class.getName()).log(Level.SEVERE, null, ex);
                }
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
             pw.newLine();
             pw.newLine();
              pw.write("\n\nRecieved ACK "+ack_no+"\n");
              pw.newLine();
             if(ack_no==(temp_no+1)){
                 break;
                 
             }
             pw.newLine();
             pw.write("\nRepeating Previous CONSIGNMENT "+pktno+"\n");
             
             }
             catch(SocketTimeoutException ex){
                try {
                    pw.newLine();
                    pw.write("\n***Time out Occured***\n");
                    pw.newLine();
                    pw.write("\nRepeating previous consignment\n");
                    pw.newLine();
                } catch (IOException ex1) {
                    Logger.getLogger(ThreadS.class.getName()).log(Level.SEVERE, null, ex1);
                }
                 
                    
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
