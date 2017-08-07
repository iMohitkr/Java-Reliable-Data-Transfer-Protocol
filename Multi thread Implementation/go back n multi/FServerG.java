
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
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
public class FServerG {

    public static List<Integer> CONSIGNMENT_TO_FORGET = new ArrayList<Integer>(); 
  public static int PORT;
  public static File f;
    
    public static void main(String[] args) {
        // TODO code application logic here
        PORT = Integer.parseInt(args[0]);
        
        CONSIGNMENT_TO_FORGET.add(Integer.parseInt(args[1]));
        CONSIGNMENT_TO_FORGET.add(Integer.parseInt(args[1]));
        CONSIGNMENT_TO_FORGET.add(Integer.parseInt(args[1]));
        CONSIGNMENT_TO_FORGET.add(Integer.parseInt(args[1]));
        DatagramSocket socket=null;
        int count = 0;
        String strMsg;
        int srcPort;
        InetAddress srcAddr;
        byte [] inBuffer;
        DatagramPacket inPacket;
        
        String filename;
        
      try {
          socket = new DatagramSocket(PORT);
          System.out.println("\n Running...\n");
      } catch (SocketException ex) {
          Logger.getLogger(FServerG.class.getName()).log(Level.SEVERE, null, ex);
      }
        while(true)
  		{ 
            try {
                System.out.println("\n Waiting for a client...\n");
                
                inBuffer = new byte[100];
                inPacket= new DatagramPacket(inBuffer,inBuffer.length);
                
                socket.receive(inPacket);
                
                
                srcPort= inPacket.getPort();
                srcAddr = inPacket.getAddress();
                strMsg = new String(inPacket.getData(), 0 , inPacket.getLength());
                
                if(strMsg.substring(0,7).equalsIgnoreCase("REQUEST")){
                    
                    filename =strMsg.substring(7, strMsg.length()-2);
                    
                    
                    System.out.println("\n Client= "+srcAddr+ " : "+srcPort +" $$Requested File :" +filename);
                    
                    if(checkFile(filename)){ //checking whether the requested file exists or not
                        System.out.println("Serving Client Request---");
                        
						//initiating the thread to transfer the file
                        ThreadG worker = new ThreadG(filename, count++,srcAddr,srcPort);
                        
                        worker.start();
                    }
                    else
                    {
                        System.out.println("File does not exist!!!");
                        filename="Error";
                        byte[] outBuffer = (filename).getBytes();
                        DatagramPacket outPacket = new DatagramPacket(outBuffer, 0, outBuffer.length,srcAddr,srcPort);
                        socket.send(outPacket);
                        
                    }
                    
                    
                }
                else
                {
                    System.out.println("Error! Request packet is not found!!");
                }
            } catch (IOException ex) {
                Logger.getLogger(FServerG.class.getName()).log(Level.SEVERE, null, ex);
            }
                 
  		}
        
    }
    
    public static boolean checkFile(String filename){
       f = new File(filename);
       
       return f.canRead();
    }
}
