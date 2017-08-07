
//all the libraries required by the program
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Mohit AND RANU
 */
 
 
public class FClientG {
	
    public static byte[] MESSAGE_START = { 0x52, 0x44, 0x54 }; // "RDT "
    public static byte[] MESSAGE_END = { 0x45, 0x4e, 0x44, 0xa, 0xd }; //" END CRLF"
    public static byte[] REQUEST={ 0x52,0x45,0x51,0x55,0x45,0x53,0x54};  // " REQUEST "
    public static byte[] ACK = new byte[] { 0x41, 0x43, 0x4b };  // " ACK "
    public static byte[] END = new byte[] { 0x45, 0x4e, 0x44 }; //" END "
    public static byte[] CRLF = new byte[] { 0x0a, 0x0d }; //"CRLF"
    
    public static int MESSAGE_FRONT_OFFSET = 4; //"RDT#"
    public static int MESSAGE_BACK_OFFSET = 2; //"CRLF"
    public static int MESSAGE_LAST_BACK_OFFSET = 5; //"ENDCRLF"
    public static int PORT;	// TO STORE THE PORT ADDRESS OF THE SERVER
    public static InetAddress addr; // TO STORE THE IP ADDRESS OF THE SERVER
    public static String serverIPaddress;  
    public static List<Integer> ACK_TO_FORGET = new ArrayList<Integer>();  // TO STORE THE ACKS NEEDED TO BE FORGOTTEN BY THE PROGRAM 
																			// FOR SIGNIFYING NULL USE VALUE OF -2 DURING RUNTIME FOR THE ACKs
    public static String filename; //	 TO STORE THE FILE NAME OF THE REQUESTED FILE
    public static DatagramSocket socket=null;  
    
    public static byte [] inBuffer= new byte[1024];
    
    public static ArrayList<byte[]> incomingFile;    
        
    
    public static void main(String[] args) {
        
		//TRYING PARSING THE IP ADDRESS OF THE SERVER
		
        try {
            addr = InetAddress.getByName(args[0]);
        } catch (UnknownHostException ex) {
            Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
        }
        
		//TRYING PARSING THE PORT ADDRESS OF THE SERVER
        PORT = Integer.parseInt(args[1]);
		
		
        filename= args[2];
		
		//ACKs to forget
        ACK_TO_FORGET.add(Integer.parseInt(args[3]));
        ACK_TO_FORGET.add(Integer.parseInt(args[4]));
        ACK_TO_FORGET.add(Integer.parseInt(args[5]));
        ACK_TO_FORGET.add(Integer.parseInt(args[6]));
        
		
        DatagramPacket inPacket;
        byte[] myMsg = concatenateByteArrays(REQUEST,filename.getBytes(), CRLF);
        
        
        try {
            socket = new DatagramSocket();
             DatagramPacket outPacket = new DatagramPacket(myMsg, 0, myMsg.length,addr,PORT);
             socket.send(outPacket); //REQUESTING THE FILE
             
             inPacket = recPacket(); // WAITING FOR THE FIRST PACKET
            String data = new String(inPacket.getData(), 0 , inPacket.getLength());
             
            if(data.endsWith("Error")){
                System.out.println("File not found!");
                socket.close();
                System.exit(0);
            }
            else{
                PORT= inPacket.getPort();
                addr = inPacket.getAddress();
                fileRecieve(inPacket);
                
            }
             
             
           socket.close(); 

        } catch (SocketException ex) {
            Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
        }
        
     
        
        
        
    }
    
    public static void fileRecieve(DatagramPacket inPacket){
        Integer temp = 0;
        byte [] te;
        String con;
        incomingFile = new ArrayList<>();
        
        Byte t;
        int ack=0;
        while(true){
            
            t= inPacket.getData()[3];// t stores the value of the consignment number
			
            con = new String(inPacket.getData(), 0 , inPacket.getLength());
                System.out.println("\nReceived CONSIGNMENT "+ t.intValue());
            if(checkPack(inPacket,temp.byteValue())){ // checking that consignment number is valid
                te= Arrays.copyOfRange(inPacket.getData(), 0, inPacket.getLength());
				//adding the valid consignment to the file buffer 
                incomingFile.add(te);
                
                temp = (temp+1);
                
                
                
                
                if(con.substring(con.length()-5,con.length()-2).equalsIgnoreCase("END")) // checking whether this is the last consignment or not
                {
                    
                    System.out.println("\n**Got all we need!Closing Socket**");
                    sendACK(-1);
                    socket.close();
                    break;
                }
                sendACK(++ack);
                
                 
                
            }
            else{
                System.out.println("Out-of order packet discarded!!");
                sendACK(ack);
            }
            inPacket=recPacket();
            
            
        }
        
		//if we got all then calling write file to save the file recieved
        writeFile();
    
        
        
    }
    
	//recPacket helps in recieving the packet
    public static DatagramPacket recPacket(){
        
        DatagramPacket inPacket=new DatagramPacket(inBuffer,inBuffer.length);
        
        try {
            socket.receive(inPacket);
        } catch (IOException ex) {
            Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        return inPacket;
    }
    
	//checkpack returns true if the packs consignment number is same as temp value
    public static boolean checkPack(DatagramPacket pack,Byte temp){
        
        int con_no = Byte.valueOf(pack.getData()[3]).intValue();
        int temp_no = temp.intValue();
        
        return con_no==temp_no;
    }
    
    public static boolean flag=false;
    
	//function to send ack
    public static void sendACK(int ack){
        
        if(ack!=-1){
            byte [] SEQ_0=new byte[1];
            SEQ_0[0]= Integer.valueOf(ack).byteValue();
        
        byte[] myMsg = concatenateByteArrays(ACK, SEQ_0, CRLF);
        DatagramPacket outPacket = new DatagramPacket(myMsg, 0, myMsg.length,addr,PORT);
        
        if(ACK_TO_FORGET.contains(ack)){
            
            if(flag){
                try {
            socket.send(outPacket);
            System.out.println("Sent ACK "+ack);
        } catch (IOException ex) {
            Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
        }
                flag=false;
            }
            else{
                flag=true;
                System.out.println("Forget ACK "+ack);
            }            
            
            
        
        
        }
        else{
            try {
            socket.send(outPacket);
            System.out.println("Sent ACK "+ack );
        } catch (IOException ex) {
            Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
        }
            
            
        }
        
        
    }
        else{
            DatagramPacket outPacket = new DatagramPacket(END, 0, END.length,addr,PORT);
            try {
                socket.send(outPacket);
            } catch (IOException ex) {
                Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    
    
    }
    
    
    
    public static byte[] concatenateByteArrays(byte[] a, byte[] b, byte[] c) {
        byte[] result = new byte[a.length + b.length + c.length ]; 
        System.arraycopy(a, 0, result, 0, a.length); 
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length+b.length, c.length);
        return result;
    }
    
    
    static public void writeFile(){
        byte[] data = new byte[512];
        int c=0;
        try {
            Scanner sc= new Scanner(System.in);
            System.out.print("Enter the name of the file for saving: ");
            filename = sc.next();
            File myFile = new File(filename);
            FileOutputStream myFOS = new FileOutputStream(myFile);
            
            for (byte[] m:incomingFile) {

                // get sequence number
               // String seqString = new String(m, MESSAGE_START.length, 1);
                //System.out.println("Sequence Number = " + seqString);

                // get last message
                if (!matchByteSequence(m, m.length-MESSAGE_END.length , MESSAGE_END.length, MESSAGE_END)) {

                    myFOS.write(m, MESSAGE_FRONT_OFFSET, m.length-MESSAGE_FRONT_OFFSET-MESSAGE_BACK_OFFSET);
                    
                    /*for (count=0; count < m.length-MESSAGE_FRONT_OFFSET-MESSAGE_BACK_OFFSET; count++) {
                        data[count] = m[MESSAGE_FRONT_OFFSET+count];
                    }*/
                    //System.out.println("Consignment = " + c);
             
                } else {
                    myFOS.write(m, MESSAGE_FRONT_OFFSET, m.length-MESSAGE_FRONT_OFFSET-MESSAGE_LAST_BACK_OFFSET);
                    //System.out.println("Last Consignment");
                }
                c++;
            }
            
            
            myFOS.close();
            System.out.println("\n\nFile download successfull!!");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FClientG.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static public boolean matchByteSequence(byte[] input, int offset, int length, byte[] ref) {
        
        boolean result = true;
        
        if (length == ref.length) {
            for (int i=0; i<ref.length; i++) {
                if (input[offset+i] != ref[i]) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    
}
