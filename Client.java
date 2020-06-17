import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import java.security.*;

import java.util.Date;
import java.util.Scanner;



import javax.crypto.Cipher;

import javax.crypto.KeyGenerator;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class Client {
	
	public static void main(String [] args) throws Exception {

		String host = args[0]; // hostname of server
        int port = Integer.parseInt(args[1]); // port of server
        String userId = args[2];
        
		Socket s = new Socket(host, port);
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		DataInputStream dis = new DataInputStream(s.getInputStream());
			
		dos.writeUTF(genSHA256(userId));
		dos.flush();
		int messages = dis.readInt();
		System.out.println("You have recieved " + messages + " message(s)\n");
		
		//if the user has messages go through the all of the messages and decrpyt & print them.
		if(messages > 0) {
			for(int i = 0; i < messages; i++ ) {
				//get message
	            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
	            Message m = (Message) ois.readObject();
	            
	            //get private key
		    	ObjectInputStream getPrivKey = new ObjectInputStream(new FileInputStream(userId + ".prv"));
		    	PrivateKey priv = (PrivateKey) getPrivKey.readObject();
		    	getPrivKey.close();
		    	
		    	//decrypt the RSA to get the AES key
		    	byte[] AESKeyBytes = decryptRSA(priv, m.key);
		    	SecretKey AESKey = new SecretKeySpec(AESKeyBytes, 0, AESKeyBytes.length, "AES");
		    
		    	//decrypt the msg with the AESkey
	            IvParameterSpec ivspec = new IvParameterSpec(m.iv);
		    	byte[] decrpyptedMsg = decryptAES(AESKey, ivspec, m.encryptedMsg);
		    	
		    	//save the decrypted msg to a string and split it by new line
		    	String msg = new String(decrpyptedMsg);
		    	String lines[] = msg.split("\\r?\\n"); 
		    	
		    	//set up the signature
		    	ObjectInputStream sigStream = new ObjectInputStream(new FileInputStream(lines[0] + ".pub"));
		    	PublicKey sigPubKey = (PublicKey) sigStream.readObject();
		    	sigStream.close();
	            Signature pubSig = Signature.getInstance("SHA1withRSA");
	            pubSig.initVerify(sigPubKey);
	            pubSig.update(m.recipientHash.getBytes("UTF-8"));
	            pubSig.update(m.timestamp.toString().getBytes("UTF-8"));
	            pubSig.update(m.encryptedMsg);
	            pubSig.update(m.iv);
	            pubSig.update(m.key);
	            
	            //print to user
		    	System.out.println(lines[0] + "'s message: ");
		    	System.out.println(lines[1]);
		    	System.out.println("Recieved on: " + m.timestamp);
	            if(pubSig.verify(m.signature) == false) {
	            	System.out.println("Insecure: Signature not verified!\n");
	            } else {
	            	System.out.println("Secure: Signature is verified.\n");
	            }
		    	
			}
		}
		Scanner scan = new Scanner(System.in);
		while(true) {
			System.out.println("Would you like to send a message? (Y/N)");
		    String ans = scan.nextLine();
		    if(ans.equalsIgnoreCase("no") || ans.equalsIgnoreCase("n")) {
		    	s.close();
		    	break;
		    } else if (ans.equalsIgnoreCase("yes") || ans.equalsIgnoreCase("y")) {
		    	//generating IV & key
		    	byte[] iv = new byte[128/8]; 
		    	SecureRandom sr = new SecureRandom();
		    	sr.nextBytes(iv);
		    	IvParameterSpec ivspec = new IvParameterSpec(iv);
		    	KeyGenerator kgen = KeyGenerator.getInstance("AES");
		    	SecretKey secretKey = kgen.generateKey();
		    	
	    	    //decide receipient and send stores info in message object ecrpyted 
		    	System.out.println("Who to?");
			    String Recipient = scan.nextLine();
			    System.out.println("Type your message: ");
			    String Message = scan.nextLine();
			    String toEncrpyt = userId + "\n" + Message;	
			    
		    	Message m = new Message();
		    	m.recipientHash = genSHA256(Recipient); //sha256 of recipient
		    	m.timestamp = new Date();
		    	m.encryptedMsg = encryptAES(secretKey, ivspec, toEncrpyt); //encrpyts message with aes
		    	m.iv = ivspec.getIV();	
				try {
					//encrypting AES secret key with RSA and setting it to the message
					ObjectInputStream getRecipientPubKey = new ObjectInputStream(new FileInputStream(Recipient + ".pub"));
					PublicKey pub = (PublicKey) getRecipientPubKey.readObject();
					getRecipientPubKey.close();
					m.key = encryptRSA(pub, secretKey.getEncoded());
					//generate and assign signature to message
					Signature sig = Signature.getInstance("SHA1withRSA");
					ObjectInputStream userPriv = new ObjectInputStream(new FileInputStream(userId + ".prv"));
					PrivateKey priv = (PrivateKey) userPriv.readObject();
					userPriv.close();
					sig.initSign(priv);
					sig.update(m.recipientHash.getBytes("UTF-8"));
					sig.update(m.timestamp.toString().getBytes("UTF-8"));
					sig.update(m.encryptedMsg);
					sig.update(m.iv);
					sig.update(m.key);
					m.signature = sig.sign();
					
					//SEND
					ObjectOutputStream send = new ObjectOutputStream(s.getOutputStream());
					send.writeObject(m); // Save object
					send.flush();
					System.out.println("Message Sent.");
				} catch (FileNotFoundException e) {
					System.out.println("Could not find " + Recipient + "'s public key in directory. Make sure you have their public key.");

				}
			}
		}
		scan.close();
		

	}
	//METHODS USED IN THE CLASS
	private static String genSHA256(String s) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(
                s.getBytes(StandardCharsets.UTF_8));
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
	}
	
	private static byte[] encryptAES(SecretKey key, IvParameterSpec ivspec, String unencrypted) 
			throws Exception {
    	Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    	cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
	    return cipher.doFinal(unencrypted.getBytes("UTF-8"));
	}
	private static byte[] decryptAES(SecretKey key, IvParameterSpec ivspec, byte[] encrypted) 
			throws Exception {
    	Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    	cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
	    return cipher.doFinal(encrypted);
	}
	
	private static byte[] encryptRSA(PublicKey key, byte[] unencrypted) 
			throws Exception {
	    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");   
	    cipher.init(Cipher.ENCRYPT_MODE, key);  
	    return cipher.doFinal(unencrypted);
	}
	
	private static byte[] decryptRSA(PrivateKey key, byte[] encrypted) 
			throws Exception {
	    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	    cipher.init(Cipher.DECRYPT_MODE, key);
	    return cipher.doFinal(encrypted);
    
	}
}

