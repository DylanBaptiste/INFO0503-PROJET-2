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

import java.net.Socket;
import java.util.Calendar;
import java.util.Scanner;

import org.json.JSONException;
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
    	
    	String message = "";
        try {
            message = reader.readLine();
        } catch(IOException e) {
            System.err.println("Erreur lors de la lecture : " + e);
            System.exit(0);
        }
    	JSONObject json = new JSONObject(message);
    	
    	 if( json.has("action") ){
             switch(json.getInt("action")){
                 case 1:
				try {
					login(json);
				} catch (JSONException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
                 break;
                 case 2:		
				try {
					creerCompte(json);
				} catch (JSONException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
                 break;
                 case 3:
				try {
					creerActivite(json);
				} catch (JSONException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                 break;
                 case 4:
                     ReceptionActivite(json);
                 break;
                 case 5:
				try {
					FinActivite(json);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                 break;
                 default: break;
             }
         }
    	
    	

        // Fermeture des flux et des sockets
        try {
            reader.close();
            writer.close();
            socketClient.close();
        } catch(IOException e) {
            System.err.println("Erreur lors de la fermeture des flux et des sockets : " + e);
            System.exit(0);
        }
    }

private static void sendError(String errorMessage) throws JSONException, IOException{
    	   
        SendReponse(new JSONObject().put("error", errorMessage).toString());
    }

protected static void FinActivite(JSONObject json) throws IOException {
// TODO Auto-generated method stub
	SendReponse("Fin de l'activite");
}

protected static void ReceptionActivite(JSONObject json) {
// TODO Auto-generated method stub

}

protected static void creerActivite(JSONObject json) throws JSONException, IOException {
	// TODO Auto-generated method stub
		JSONObject jsonQuerry  = null;
		try{
			jsonQuerry = json;
		}catch(Exception e){
			sendError("Les données envoyées ne sont pas au format json");
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
	    fichier.createNewFile();
	    sendSuccessCreate("Création d'activite reussi", loginU);
	}else{
	    sendError("L'activite "+activityU+" existe deja");
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

public static void login(JSONObject json) throws JSONException, IOException {
	// TODO Auto-generated method stub
	String loginU     = "";
	String passwordU = "";
	System.out.println(json.toString());
	JSONObject jsonQuerry  = null;
	try{
	    jsonQuerry = json;
	}catch(Exception e){
	    sendError("Les données envoyées ne sont pas au format json");
	    return;
	}
	
	
	
	
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
	    localJsonUser = readJson(loginU);
	}
	catch(Exception e){
	    sendError(e.toString());
	    return;
	}
	
	
	
	if( passwordU.equals(localJsonUser.getString("password")) ){
	    sendSuccessCreate("login reussi",loginU);
	}else{
	    System.out.println(localJsonUser.getString("password"));
	
	    System.out.println(passwordU);
	    sendError("Mauvais mot de passe");
	    return;
	}

}

//message En cas de succes
private static void sendSuccessCreate(String successMessage, String id) throws JSONException, IOException{
 		
        SendReponse(new JSONObject().put("success", successMessage).put("id", id).toString());

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
	
	protected static void creerCompte(JSONObject msg) throws JSONException, IOException {
	
	// TODO Auto-generated method stub
	String loginU     = "";
	String passwordU = "";
	String passwordConfirmU = "";
	
	JSONObject jsonQuerry  = null;
	try{
	    jsonQuerry = msg;
	}catch(Exception e){
	    sendError("Les données envoyées ne sont pas au format json");
	    return;
	}
	
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
	    fichier.createNewFile();
	}else{
	    sendError("L'utilisateur "+loginU+" existe deja");
	    System.out.println("Un utilisateur à tenté de créé un compte mais a échoué:        "+loginU+": "+json.toString());
	    return;
	}
	
	try{
	    FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
	    ecritureFichier.write(json.toString());
	    sendSuccessCreate("Création de compte reussi", loginU);
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





	public static void SendReponse(String data) throws IOException {
		writer.write(data);
		writer.flush();
	}

    
}