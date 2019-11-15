package com.test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

public class ThreadConnexion extends Thread {

    private BufferedReader reader;
    private static PrintWriter writer;
    private Socket socketClient;

    public ThreadConnexion(Socket socketClient) {
        this.socketClient = socketClient;

        // Association d'un flux d'entrée et de sortie
        try {
            reader = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream())), true);
        } catch(IOException e) {
            System.err.println("Association des flux impossible : " + e);
            System.exit(0);
        }
    }

    @Override
    public void run() {

		InetSocketAddress remote = (InetSocketAddress)socketClient.getRemoteSocketAddress();
    	String debug = "";
        debug = "Thread : " + Thread.currentThread().getName() + ". ";
        debug += "Demande de l'adresse : " + remote.getAddress().getHostAddress() +".";
        debug += " Sur le port : " + remote.getPort() + ".\n";
        System.out.println("\n" + debug);
		
		while(!socketClient.isClosed()){ 
			String message = "";
			try {
				message = reader.readLine();
			} catch(IOException e) {
				System.err.println("Erreur lors de la lecture : " + e);
				sendError("Erreur lors de la lecture de la requete");
				System.exit(0);
			}
			JSONObject json = null;
			try{
				json = new JSONObject(message);
			}catch(Exception e){
				sendError("Les données envoyées ne sont pas au format json");

			}
			
			if( json.has("action") ){
				switch(json.getInt("action")){
					case 1: login(json); break;
					case 2:	creerCompte(json); break;
					case 3: creerActivite(json); break;
					case 4: ReceptionActivite(json); break;
					case 5: FinActivite(json); break;
					case 8 :
						try {
							System.out.println("fermeture du thread : "+ Thread.currentThread().getName());
							reader.close();
							writer.close();
							socketClient.close();
						} catch(IOException e) {
							System.err.println("Erreur lors de la fermeture des flux et des sockets : " + e);
						}
						break;
					default: sendError("Cette action n'existe pas"); break;
				}
			}
			else{
				sendError("Aucune action demandé");
			}
		}

    }

/**
     * * Renvoye au client une message d'erreur au format JSON: {"error": "errorMessage"}
     * @param errorMessage
     * @param packet
     * @param socket
     */
    private static void sendError(String errorMessage){
        SendReponse(new JSONObject().put("error", errorMessage).toString());
    }

    /**
     * Renvoye au client une message de reussite au format JSON: {"success": "successMessage", data: {...}}
     * @param successMessage le message de reussite
     * @param data des données à associées à la reponse
     * @param packet
     * @param socket
     */
    private static void sendSuccess(String successMessage, JSONObject data){		
        SendReponse(new JSONObject().put("success", successMessage).put("data", data).toString());
    }

protected static void FinActivite(JSONObject json) {
	String loginU = "";
	String activityU = "";

	if( json.has("login") && !json.getString("login").equals("") ){
		loginU = json.getString("login");
		loginU.replaceAll("[%~/. ]", "");
	}
	else{
		sendError("Vous n'etes pas connecté");
		return;
	}

	if( json.has("activity") ){ activityU = json.getString("activity"); }
	else{
		sendError("Aucune activité envoyé");
		return;
	}

	//recuperation du fichier
	JSONObject newFile = null;
	try{
		newFile = readActivity(loginU, activityU);
	}catch(Exception e){
		sendError(e.getMessage());
		return;
	}

	//Ecriture
	newFile.put("closeDate",  Calendar.getInstance().getTime().toString());

	File fichier = new File("activity/"+loginU+"/"+activityU+".json");
	if(fichier.exists()){
		try{
			FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
			ecritureFichier.write(newFile.toString());
			ecritureFichier.close();
			sendSuccess("Activité fermé", null);
			return;
		}
		catch(IOException e){
			sendError("Une erreur interne au serveur est survenu");
			System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+newFile.toString()+"\n"+e);
			return;
		}
	}else{
		sendError("L'activite "+activityU+" n'existe pas");
		
		return;
	}
}

private static JSONObject readActivity(String loginU, String activityU) throws Exception{
	try  {
		return new JSONObject( readFile("activity/"+loginU+"/"+activityU+".json") );
	}
	catch (FileNotFoundException e){
		throw new Exception("L'activité "+activityU+" n'existe  pas.");
	}
}
private static String readFile(String path) throws Exception{
	FileInputStream fs = null;
	String json = "";

	File fichier = new File(path);
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
		throw e;
	}

	return json;
}
protected static void ReceptionActivite(JSONObject json) {
	System.out.println("ReceptionActivite: " + json.toString());

	String loginU = "";
	String activityU = "";
	JSONArray newGPSdatas = null;

	if( json.has("login") && !json.getString("login").equals("") ){
		loginU = json.getString("login");
		loginU.replaceAll("[%~/. ]", "");
	}
	else{
		sendError("Vous n'etes pas connecté");
		return;
	}

	if( json.has("activity") ){ activityU = json.getString("activity"); }
	else{
		sendError("Aucune activité envoyé");
		return;
	}

	if( json.has("GPSdata") ){ newGPSdatas = json.getJSONArray("GPSdata"); }
	else{
		sendError("Aucune données GPS envoyé");
		return;
	}

	JSONObject oldFile = null;
	try{
		oldFile = readActivity(loginU, activityU);
	}catch(Exception e){
		sendError(e.getMessage());
		return;
	}
	
	//anciennes + nouvelles GPSdata fusion 
	JSONArray oldGPSdatas = oldFile.getJSONArray("GPSdata");

	for (Object object : newGPSdatas) {
		oldGPSdatas.put(object);
	}

	/*List<GPSdata> newList = new ArrayList<GPSdata>();
	for (Object element : oldGPSdatas) { newList.add(new GPSdata( (JSONObject)element ) ); }
	for (Object element : newGPSdatas) { newList.add(new GPSdata( (JSONObject)element ) ); }

	//nouvelle List -> JSONArray
	JSONArray jsonGPSdata = new JSONArray();
	for (GPSdata element : newList) {
		jsonGPSdata.put(element.toJSON());
	}*/
   
	//Ecriture
	JSONObject jsonEcriture = new JSONObject();
	jsonEcriture.put("creationDate", oldFile.getString("creationDate") );
	jsonEcriture.put("GPSdata", oldGPSdatas );

	File fichier = new File("activity/"+loginU+"/"+activityU+".json");
	if(fichier.exists()){
		try{
			FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
			ecritureFichier.write(jsonEcriture.toString());
			ecritureFichier.close();
			sendSuccess("Données sauvegardées", null);
			return;
		}
		catch(IOException e){
			sendError("Une erreur interne au serveur d'autentification est survenu");
			System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+jsonEcriture.toString()+"\n"+e);
			return;
		}
	}else{
		sendError("L'activite "+activityU+" n'existe pas");
		
		return;
	}

}

protected static void creerActivite(JSONObject jsonQuerry) {

	//Recuperation des infos du json
	String loginU     = "";
	String activityU  = "";
	
	if( jsonQuerry.has("login") ){
	    loginU = jsonQuerry.getString("login");
	    loginU.replaceAll("[%~/. ]", "");
	}
	else{
	    sendError("Aucun login envoyé");
	    return;
	}
	
	if( jsonQuerry.has("activity") ){
	    activityU = jsonQuerry.getString("activity");
	}
	else{
	    sendError("Aucune activité envoyé");
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
	    try{ fichier.createNewFile();}catch(Exception e){sendError("Impossible de creer votre activité");}
	    sendSuccess("Création d'activite reussi", new JSONObject().put("login", loginU));
	}else{
	    sendError("L'activite "+activityU+" existe deja");
	    System.out.println("Un utilisateur à tenté de créé une activite mais a échoué: "+activityU+": "+jsonEcriture.toString());
	    return;
	}




}

public static void login(JSONObject jsonQuerry) {
	String loginU     = "";
	String passwordU = "";
	System.out.println(jsonQuerry.toString());

	
	
	
	
	if( jsonQuerry.has("login") ){
	    loginU = jsonQuerry.getString("login");
	    loginU.replaceAll("[%~/. ]", "");
	}
	else{
	    sendError("Aucun login envoyé");
	    return;
	}
	
	if( jsonQuerry.has("password") ){
	    passwordU = jsonQuerry.getString("password");
	}
	else{
	    sendError("Aucun password envoyé");
	    return;
	}
	
	
	
	JSONObject localJsonUser = null;
	try{
	    localJsonUser = readUser(loginU);
	}
	catch(Exception e){
	    sendError(e.toString());
	    return;
	}
	
	
	
	if( passwordU.equals(localJsonUser.getString("password")) ){
	    sendSuccess("login reussi", new JSONObject().put("login", loginU));
	}else{
	    System.out.println(localJsonUser.getString("password"));
	
	    System.out.println(passwordU);
	    sendError("Mauvais mot de passe");
	    return;
	}

}


private static JSONObject readUser(String loginU) throws Exception{
	try  {
		return new JSONObject( readFile("users/"+loginU+".json") );
	}
	catch (FileNotFoundException e){
		throw new Exception("L'utilisateur "+loginU+" n'existe  pas.");
	}
}
	
	private static void creerCompte(JSONObject jsonQuerry) {
	
	String loginU     = "";
	String passwordU = "";
	String passwordConfirmU = "";
	

	
	// LoginU prend la valeur du login dans le json
	if( jsonQuerry.has("login") ){
	    loginU = jsonQuerry.getString("login");
	    loginU.replaceAll("[%~/. ]", "");
	}
	else{
	    sendError("Aucun login envoyé");
	    return;
	}
	
	// PasswordU prend la valeur du PasswordU dans le json
	if( jsonQuerry.has("password") ){
	    passwordU = jsonQuerry.getString("password");
	}
	else{
	    sendError("Aucun password envoyé");
	    return;
	}
	
	if( jsonQuerry.has("passwordConfirm") ){
		passwordConfirmU = jsonQuerry.getString("passwordConfirm");
	}
	else{
	    sendError("Aucun mot de passe de confirmation envoyé");
	    return;
	}
	
	if( !(passwordU.equals(passwordConfirmU)) ){
	    sendError("Le mot de passe et mot de passe de confirmation ne sont pas les mêmes.");
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
	   try{ fichier.createNewFile();}catch(Exception e){sendError("Impossible de creer votre compte");}
	}else{
	    sendError("L'utilisateur "+loginU+" existe deja");
	    System.out.println("Un utilisateur à tenté de créé un compte mais a échoué: "+loginU+": "+json.toString());
	    return;
	}
	
	try{
	    FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
	    ecritureFichier.write(json.toString());
	    sendSuccess("Création de compte reussi", new JSONObject().put("login", loginU));
	    ecritureFichier.close();
	    System.out.println("Nouveau utilisateur créé:        "+loginU+": "+json.toString());
	    return;
	}
	catch(IOException e){
	    sendError("Une erreur interne au serveur d'autentification est survenu");
	    System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+json.toString()+"\n"+e);
	    return;
	}

}


	public static void SendReponse(String data) {
		System.out.println("data :" + data);
		writer.println(data);
	}

    
}