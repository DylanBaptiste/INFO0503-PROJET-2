package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

import org.json.JSONObject;

/**
 * Classe correspondant à un serveur UDP.
 * La chaine de caractères "Bonjour" est envoyée au serveur.
 * @author Cyril Rabat
 * @version 20/10/2019
 */
public class ServeurUDP {

    // Le port d'écoute du serveur
    public static int portEcoute = 2025;
    
    public static void creerCompte(JSONObject msg){
    	JSONObject json = new JSONObject();
		
    	String loginU = msg.getString("login");
    	json.put("login", loginU);
    	json.put("password", msg.getString("password"));
		json.put("creationDate", Calendar.getInstance().getTime().toString() );

		File fichier = new File("users/"+loginU+".json");
		if(!fichier.exists()){
			try {
				fichier.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			System.out.println("Un utilisateur à tenté de créé un compte mais a échoué: "+loginU+": "+json.toString());
			return;
		}
			
		try{
			FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
			ecritureFichier.write(json.toString());
			ecritureFichier.close();
			System.out.println("Nouveau utilisateur créé:		"+loginU+": "+json.toString());
			return;
		}
		catch(IOException e){
			System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+json.toString()+"\n"+e);
			return;
		}
    }

    public static void main(String[] args) {
        // Création de la socket
        DatagramSocket socket = null;
        try {        
            socket = new DatagramSocket(portEcoute);
        } catch(SocketException e) {
            System.err.println("Erreur lors de la création de la socket : " + e);
            System.exit(-1);
        }

        // Création du message
        byte[] tampon = new byte[4];
        DatagramPacket msg = new DatagramPacket(tampon, tampon.length);

        // Lecture du message du client
        System.out.println("En attente d'un message sur le port " + portEcoute);
        
        
        while(true){
            try {
                socket.receive(msg);
                JSONObject json = new JSONObject(new String(msg.getData(), 0, msg.getLength()));
                
                if( json.has("action") ){
                	switch(json.getInt("action")){
	                	case 1:
	                		creerCompte(json);
	                		break;
	                	default: break;
                	}
        		}
                
            } catch(IOException e) {
                System.err.println("Erreur lors de la réception du message : " + e);
                socket.close();
                System.exit(-1);
            }
        }
        
        
    }

}