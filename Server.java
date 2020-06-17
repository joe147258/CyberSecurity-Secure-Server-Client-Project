import java.io.*;
import java.net.*;
import java.util.*;

class Server {
	
    static ArrayList<Message> Messages = new ArrayList<Message>();

	public static void main(String [] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		ServerSocket server = new ServerSocket(port);
		System.out.println("Waiting for a connection...");
		Socket clientConnection = server.accept();
		int newClient = 3; //newClient = 1 (new connected), = 2(sending another message), 3= first connection to server
        while(true) {
        	if(clientConnection.isClosed()) {
                System.out.println("Waiting for a connection...");
                clientConnection = server.accept();
                newClient = 1;
        	} else {
        		if(newClient != 3) newClient = 2;
        		else newClient = 1;
        	}
            try {
                DataInputStream dis = new DataInputStream(clientConnection.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientConnection.getOutputStream());
                
                String UserUTF = "";
                if(newClient == 1) {
    	            UserUTF = dis.readUTF();
                }
                
                int messagesRecieved = 0;
                for(int j = 0; j < Messages.size(); j++) {
                    if(Messages.get(j).recipientHash.equals(UserUTF)) {
                    	messagesRecieved++;
                    }
                }

                //if its a newly connected client
                if(newClient == 1) {
                    System.out.println("Client Connection recieved");
                    dos.writeInt(messagesRecieved);
                    dos.flush();
                    if(messagesRecieved != 0) {
                    	//logic if user has a message
                    	for(int i = 0; i < Messages.size(); i++) {
            		    	ObjectOutputStream save = new ObjectOutputStream(clientConnection.getOutputStream());
            		        save.writeObject(Messages.get(i)); // Save object
            		        save.flush();
            		        Messages.remove(i);
            		        i--;
                    	}
                    	
                    }
                }

                ObjectInputStream ois = new ObjectInputStream(clientConnection.getInputStream());
                Message m = (Message) ois.readObject();;
                Messages.add(m);
            }
            catch(SocketException | EOFException e) {
            	System.out.println("Client has disconnected.");
            	clientConnection.close();
            }
            server.close();
        }
	}
}
