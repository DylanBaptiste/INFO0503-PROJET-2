package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;



public class ServeurUDP {

   public final static int port = 2345;
   public final static DatagramSocket server =null;
   public static void main(String[] args){
    
      Thread t = new Thread(new Runnable(){
         public void run(){
            try {
               
               //Création de la connexion côté serveur, en spécifiant un port d'écoute
               DatagramSocket server = new DatagramSocket(port);
               
               while(true){
                  
                  //On s'occupe maintenant de l'objet paquet
                  byte[] buffer = new byte[8192];
                  DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                                   
                  //Cette méthode permet de récupérer le datagramme envoyé par le client
                  //Elle bloque le thread jusqu'à ce que celui-ci ait reçu quelque chose.
                  server.receive(packet);
                  
                  ///// fonction //////
                  // Reception de notre JSON 
                  JSONObject json = new JSONObject(new String(packet.getData(), 0, packet.getLength()));
                  if( json.has("action") ){
                  	switch(json.getInt("action")){
  	                	case 1:
  	                		creerCompte(json,packet);
  	                	break;
  	                	case 2:
  	                		login(json,packet);
  	                	break;
  	                	case 3:
  	                		creerActivite(json, packet);
  	                	break;
  	                	case 4:
  	                		ReceptionActivite(json, packet);
  	                	break;
  	                	case 5:
  	                		FinActivite(json, packet);
  	                	break;
  	                	default: break;
                  	}
                  }
                  /*
                  //nous récupérons le contenu de celui-ci et nous l'affichons
                  String str = new String(packet.getData());
                  print("Reçu de la part de " + packet.getAddress() 
                                    + " sur le port " + packet.getPort() + " : ");
                  println(str);
                  println(json.toString());
                  
                  //On réinitialise la taille du datagramme, pour les futures réceptions
                  packet.setLength(buffer.length);
                                    
                  //et nous allons répondre à notre client, donc même principe
                 	byte[] tampon = str.getBytes();
          	    	DatagramPacket packetreponse = new DatagramPacket(

          	                         tampon,             //Les données 

          	                         tampon.length,      //La taille des données

          	                         packet.getAddress(), //L'adresse de l'émetteur

          	                         packet.getPort()     //Le port de l'émetteur

          	    );
          	    server.send(packetreponse);
          	    packetreponse.setLength(tampon.length);*/
               }
            } catch (SocketException e) {
               e.printStackTrace();
            } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
         }
      });  
      
      //Lancement du serveur
      t.start();
      
      Thread cli1 = new Thread(new UDPClient("Cysboy", 1000));
      Thread cli2 = new Thread(new UDPClient("John-John", 1000));
      
      cli1.start();
      cli2.start();
      
   }
   
//message En cas de succes
 private static void sendSuccessCreate(String successMessage, String id,DatagramPacket msg) throws JSONException, IOException{

			SendReponse(msg, new JSONObject().put("success", successMessage).put("id", id).toString());

	}

// Message en cas d'erreur
private static void sendError(String errorMessage, DatagramPacket msg) throws JSONException, IOException{
			SendReponse(msg,new JSONObject().put("error", errorMessage).toString());
		}
 
 protected static void FinActivite(JSONObject json, DatagramPacket msg) throws IOException {
	// TODO Auto-generated method stub
	 SendReponse(msg,"Fin de l'activite" );
}

protected static void ReceptionActivite(JSONObject json, DatagramPacket msg) {
	// TODO Auto-generated method stub
	
}

protected static void creerActivite(JSONObject json , DatagramPacket msg) {
	// TODO Auto-generated method stub
	
}

protected static void login(JSONObject json,DatagramPacket packet) throws JSONException, IOException {
	// TODO Auto-generated method stub
	String loginU	 = "";
	String passwordU = "";

	JSONObject jsonQuerry  = null;
	try{
		jsonQuerry = new JSONObject(json);
	}catch(Exception e){
		sendError("Les données envoyées ne sont pas au format json", packet);
		return;
	}

	

	
	if( jsonQuerry.has("login") ){
		loginU = jsonQuerry.getString("login");
		loginU.replaceAll("[%~/. ]", "");
	}
	else{
		sendError("Aucun login envoyé", packet);
		return;
	}

	if( jsonQuerry.has("password") ){
		passwordU = jsonQuerry.getString("password");
	}
	else{
		sendError("Aucun password envoyé", packet);
		return;
	}

	
	
	JSONObject localJsonUser = null;
	try{
		localJsonUser = readJson(loginU);
	}
	catch(Exception e){
		sendError(e.toString(), packet);
		return;
	}


	
	if( passwordU.equals(localJsonUser.getString("password")) ){
		sendSuccessCreate("login reussi",loginU, packet);
	}else{
		System.out.println(localJsonUser.getString("password"));

		System.out.println(passwordU);
		sendError("Mauvais mot de passe", packet);
		return;
	}

}

private static JSONObject readJson(String loginU) throws Exception{
	FileInputStream fs = null;
	String json = "";

	File fichier = new File("users/"+loginU+".json");
	try  {
		fs = new FileInputStream ( fichier.getAbsoluteFile() );
		Scanner scanner = new Scanner ( fs );

		while ( scanner.hasNext() )
			json += scanner.nextLine();

		scanner.close();
		json = json.replaceAll("[\t\r\n ]", "");
		fs.close();
	}
	catch (FileNotFoundException e){
		throw new Exception("L'utilisateur "+loginU+" n'existe  pas.");
	}

	return new JSONObject( json );
}

protected static void creerCompte(JSONObject msg , DatagramPacket packet) throws JSONException, IOException {
	// TODO Auto-generated method stub
	String loginU	 = "";
	String passwordU = "";
	String confirmPasswordU = "";

	JSONObject jsonQuerry  = null;
	try{
		jsonQuerry = new JSONObject(msg);
	}catch(Exception e){
		sendError("Les données envoyées ne sont pas au format json", packet);
		return;
	}
	
	// LoginU prend la valeur du login dans le json
	if( jsonQuerry.has("login") ){
		loginU = jsonQuerry.getString("login");
		loginU.replaceAll("[%~/. ]", "");
	}
	else{
		sendError("Aucun login envoyé", packet);
		return;
	}

	// PasswordU prend la valeur du PasswordU dans le json
	if( jsonQuerry.has("password") ){
		passwordU = jsonQuerry.getString("password");
	}
	else{
		sendError("Aucun password envoyé", packet);
		return;
	}

	if( jsonQuerry.has("confirmPassword") ){
		confirmPasswordU = jsonQuerry.getString("confirmPassword");
	}
	else{
		sendError("Aucun mot de passe de confirmation envoyé", packet);
		return;
	}

	if( !(passwordU.equals(confirmPasswordU)) ){
		sendError("Le mot de passe et mot de passe de confirmation ne sont pas les mêmes.", packet);
		return;
	}

	
	
	
	// Creation du fichier et ecriture 
	JSONObject json = new JSONObject();
	json.put("password", passwordU);
	json.put("creationDate", Calendar.getInstance().getTime().toString() );
	 File repertoire = new File("users/");
		
	 repertoire.mkdirs();
	
	File fichier = new File("users/"+loginU+".json");
	if(!fichier.exists()){
		fichier.createNewFile();
	}else{
		sendError("L'utilisateur "+loginU+" existe deja", packet);
		System.out.println("Un utilisateur à tenté de créé un compte mais a échoué:		"+loginU+": "+json.toString());
		return;
	}
		
	try{
		FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
		ecritureFichier.write(json.toString());
		sendSuccessCreate("Création de compte reussi", loginU, packet);
		ecritureFichier.close();
		System.out.println("Nouveau utilisateur créé:		"+loginU+": "+json.toString());
		return;
	}
	catch(IOException e){
		sendError("Une erreur interne au serveur d'autentification est survenu", packet);
		System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+json.toString()+"\n"+e);
		return;
	}

}


public static synchronized void print(String str){
      System.out.print(str);
}

public static synchronized void println(String str){
      System.err.println(str);
}
   
public static void SendReponse(DatagramPacket msg,String data) throws IOException {

	    byte[] tampon = data.getBytes();
	    DatagramPacket packetreponse = new DatagramPacket(

	                         tampon,             //Les données 

	                         tampon.length,      //La taille des données

	                         msg.getAddress(), //L'adresse de l'émetteur

	                         msg.getPort()     //Le port de l'émetteur

	    );
	    server.send(packetreponse);
	    packetreponse.setLength(tampon.length);
}
   
   
   
   
  
  public static class UDPClient implements Runnable{
      String name = "";
      long sleepTime = 1000;
      
      public UDPClient(String pName, long sleep){
         name = pName;
         sleepTime = sleep;
      }
      
      public void run(){
         int nbre = 0;
         while(true){
            String envoi = name + "-" + (++nbre);
            byte[] buffer = envoi.getBytes();
            
            try {
               //On initialise la connexion côté client
               DatagramSocket client = new DatagramSocket();
               
               //On crée notre datagramme
               InetAddress adresse = InetAddress.getByName("127.0.0.1");
               DatagramPacket packet = new DatagramPacket(buffer, buffer.length, adresse, port);
               
               //On lui affecte les données à envoyer
               packet.setData(buffer);
               
               //On envoie au serveur
               client.send(packet);
               
               //Et on récupère la réponse du serveur
               byte[] buffer2 = new byte[8196];
               DatagramPacket packet2 = new DatagramPacket(buffer2, buffer2.length, adresse, port);
               client.receive(packet2);
               print(envoi + " a reçu une réponse du serveur : ");
               println(new String(packet2.getData()));
               
               try {
                  Thread.sleep(sleepTime);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
               
            } catch (SocketException e) {
               e.printStackTrace();
            } catch (UnknownHostException e) {
               e.printStackTrace();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }      
   }  
}

