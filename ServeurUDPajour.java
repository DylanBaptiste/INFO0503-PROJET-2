package ServeurUDP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.net.DatagramSocket;
import java.net.DatagramPacket;

import java.net.SocketException;

import java.util.Calendar;

import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;



/**
 * Classe correspondant à un serveur UDP.
 * La chaine de caractères "Bonjour" est envoyée au serveur.
 * @author Cyril Rabat
 * @version 20/10/2019
 */
public class ServeurUDP {

    // Le port d'écoute du serveur

    public final static int portEcoute = 2345;
    public static  DatagramSocket socket = null;

    public static void main(String[] args) {
    Thread t = new Thread(new Runnable(){
           
    	public void run(){
    		// Création de la socket
			try {
			    socket = new DatagramSocket(portEcoute);
			    

			} catch(SocketException e) {
			    System.err.println("Erreur lors de la création de la socket : " + e);
			    System.exit(-1);
			}

			// Création du message
			byte[] tampon = new byte[800];
			DatagramPacket packet = new DatagramPacket(tampon, tampon.length);


			// Lecture du message du client
      
			System.out.println("En attente d'un message sur le port " + portEcoute);

			while(true){
			    try {
			        socket.receive(packet);

			        JSONObject json = new JSONObject(new String(packet.getData(), 0, packet.getLength()));

			          if( json.has("action") ){
			              switch(json.getInt("action")){
			                  case 1:
			                      login(json,packet, socket);
			                  break;
			                  case 2:
			                      
			                      creerCompte(json,packet, socket);
			                  break;
			                  case 3:
			                      creerActivite(json, packet, socket);
			                  break;
			                  case 4:
			                      ReceptionActivite(json, packet, socket);
			                  break;
			                  case 5:
			                      FinActivite(json, packet, socket);
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
     });  
          
  //Lancement du serveur
  t.start();
}

   

    // Message en cas d'erreur
private static void sendError(String errorMessage, DatagramPacket msg, DatagramSocket socket) throws JSONException, IOException{
   
            SendReponse(msg,new JSONObject().put("error", errorMessage).toString(), socket);
        }

 protected static void FinActivite(JSONObject json, DatagramPacket msg, DatagramSocket socket) throws IOException {
    // TODO Auto-generated method stub
     SendReponse(msg,"Fin de l'activite", socket );
}

protected static void ReceptionActivite(JSONObject json, DatagramPacket msg, DatagramSocket socket) {
    // TODO Auto-generated method stub

}

protected static void creerActivite(JSONObject json , DatagramPacket msg, DatagramSocket socket) throws JSONException, IOException {
    // TODO Auto-generated method stub
    JSONObject jsonQuerry  = null;
    try{
        jsonQuerry = json;
    }catch(Exception e){
        sendError("Les données envoyées ne sont pas au format json", msg, socket);
        return;
    }
    //Recuperation des infos du json
    String loginU     = "";
    String activityU  = "";
    
    if( jsonQuerry.has("login") ){
        loginU = jsonQuerry.getString("login");
        loginU.replaceAll("[%~/. ]", "");
    }
    else{
        sendError("Aucun login envoyé", msg, socket);
        return;
    }

    if( jsonQuerry.has("activity") ){
        activityU = jsonQuerry.getString("activity");
    }
    else{
        sendError("Aucune activité envoyé", msg, socket);
        return;
    }
    
    
    //Ecriture
    
    JSONObject jsonEcriture = new JSONObject();
    jsonEcriture.put("activity", activityU);
    jsonEcriture.put("creationDate", Calendar.getInstance().getTime().toString() );
     File repertoire = new File("activity/"+loginU+"/");

     repertoire.mkdirs();

    File fichier = new File("activity/"+loginU+"/"+activityU+".json");
    if(!fichier.exists()){
        fichier.createNewFile();
        sendSuccessCreate("Création d'activite reussi", loginU, msg, socket);
    }else{
        sendError("L'activite "+activityU+" existe deja", msg, socket);
        System.out.println("Un utilisateur à tenté de créé une activite mais a échoué:        "+activityU+": "+json.toString());
        return;
    }
    /*
    try{
        FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
        ecritureFichier.write(json.toString());
        sendSuccessCreate("Création d'activite reussi", loginU, msg, socket);
        ecritureFichier.close();
        System.out.println("nouvel activité créé:        "+activityU+": "+json.toString());
        return;
    }
    catch(IOException e){
        sendError("Une erreur interne au serveur d'autentification est survenu", msg, socket);
        System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+json.toString()+"\n"+e);
        return;
    }*/

    
    
    
}

public static void login(JSONObject json,DatagramPacket packet, DatagramSocket socket) throws JSONException, IOException {
    // TODO Auto-generated method stub
    String loginU     = "";
    String passwordU = "";
    System.out.println(json.toString());
    JSONObject jsonQuerry  = null;
    try{
        jsonQuerry = json;
    }catch(Exception e){
        sendError("Les données envoyées ne sont pas au format json", packet, socket);
        return;
    }




    if( jsonQuerry.has("login") ){
        loginU = jsonQuerry.getString("login");
        loginU.replaceAll("[%~/. ]", "");
    }
    else{
        sendError("Aucun login envoyé", packet, socket);
        return;
    }

    if( jsonQuerry.has("password") ){
        passwordU = jsonQuerry.getString("password");
    }
    else{
        sendError("Aucun password envoyé", packet, socket);
        return;
    }



    JSONObject localJsonUser = null;
    try{
        localJsonUser = readJson(loginU);
    }
    catch(Exception e){
        sendError(e.toString(), packet, socket);
        return;
    }



    if( passwordU.equals(localJsonUser.getString("password")) ){
        sendSuccessCreate("login reussi",loginU, packet, socket);
    }else{
        System.out.println(localJsonUser.getString("password"));

        System.out.println(passwordU);
        sendError("Mauvais mot de passe", packet, socket);
        return;
    }

}

//message En cas de succes
 private static void sendSuccessCreate(String successMessage, String id,DatagramPacket msg, DatagramSocket socket) throws JSONException, IOException{
	 		
            SendReponse(msg, new JSONObject().put("success", successMessage).put("id", id).toString(), socket);

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

protected static void creerCompte(JSONObject msg , DatagramPacket packet, DatagramSocket socket) throws JSONException, IOException {

    // TODO Auto-generated method stub
    String loginU     = "";
    String passwordU = "";
    String passwordConfirmU = "";

    JSONObject jsonQuerry  = null;
    try{
        jsonQuerry = msg;
    }catch(Exception e){
        sendError("Les données envoyées ne sont pas au format json", packet, socket);
        return;
    }

    // LoginU prend la valeur du login dans le json
    if( jsonQuerry.has("login") ){
        loginU = jsonQuerry.getString("login");
        loginU.replaceAll("[%~/. ]", "");
    }
    else{
        sendError("Aucun login envoyé", packet, socket);
        return;
    }

    // PasswordU prend la valeur du PasswordU dans le json
    if( jsonQuerry.has("password") ){
        passwordU = jsonQuerry.getString("password");
    }
    else{
        sendError("Aucun password envoyé", packet, socket);
        return;
    }

    if( jsonQuerry.has("passwordConfirm") ){
    	passwordConfirmU = jsonQuerry.getString("passwordConfirm");
    }
    else{
        sendError("Aucun mot de passe de confirmation envoyé", packet, socket);
        return;
    }

    if( !(passwordU.equals(passwordConfirmU)) ){
        sendError("Le mot de passe et mot de passe de confirmation ne sont pas les mêmes.", packet, socket);
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
        sendError("L'utilisateur "+loginU+" existe deja", packet, socket);
        System.out.println("Un utilisateur à tenté de créé un compte mais a échoué:        "+loginU+": "+json.toString());
        return;
    }

    try{
        FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
        ecritureFichier.write(json.toString());
        sendSuccessCreate("Création de compte reussi", loginU, packet, socket);
        ecritureFichier.close();
        System.out.println("Nouveau utilisateur créé:        "+loginU+": "+json.toString());
        return;
    }
    catch(IOException e){
        sendError("Une erreur interne au serveur d'autentification est survenu", packet, socket);
        System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+json.toString()+"\n"+e);
        return;
    }

}





public static void SendReponse(DatagramPacket msg,String data, DatagramSocket socket) throws IOException {

	System.out.println("msg.getAddress():" + msg.getAddress());
    System.out.println("msg.getPort():" + msg.getPort());
    
        byte[] tampon = data.getBytes();
        DatagramPacket packetreponse = new DatagramPacket(

                             tampon,             //Les données

                             tampon.length,      //La taille des données

                             msg.getAddress(), //L'adresse de l'émetteur

                             msg.getPort()     //Le port de l'émetteur

        );
        
        socket.send(packetreponse);
        packetreponse.setLength(tampon.length);
}

}
