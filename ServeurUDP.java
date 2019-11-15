package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;;

public class ServeurUDP {

    public final static int portEcoute = 2000;
    public static DatagramSocket socket = null;

    public static void main(String[] args) {
    
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

        // Lecture du message des clients
        while(true){
            try {
                socket.receive(packet);
                System.out.print("--- Nouvelle requete\t");
                JSONObject json = null;
                try{
                    json = new JSONObject(new String(packet.getData(), 0, packet.getLength()));
                }catch(Exception e){
                    sendError("Les données envoyées ne sont pas au format json", packet, socket);
                    continue;
                }

                if( json.has("action") ){
                    switch(json.getInt("action")){
                        case 1: login(json,packet, socket); break;
                        case 2: creerCompte(json,packet, socket); break;
                        case 3: creerActivite(json, packet, socket); break;
                        case 4: ReceptionActivite(json, packet, socket); break;
                        case 5: FinActivite(json, packet, socket); break;
                        default: sendError("Cette action n'existe pas", packet, socket); break;
                    }
                }
                else{
                    sendError("Aucune action demandé", packet, socket);
                    continue;
                }

            } catch(IOException e) {
                System.err.println("Erreur lors de la réception du message : " + e);
                sendError("Aucune action demandé", packet, socket);
               
            }
        }
        
    }


    /**
     * * Renvoye au client une message d'erreur au format JSON: {"error": "errorMessage"}
     * @param errorMessage
     * @param packet
     * @param socket
     */
    private static void sendError(String errorMessage, DatagramPacket packet, DatagramSocket socket){
        SendReponse(packet, new JSONObject().put("error", errorMessage).toString(), socket);
    }

    /**
     * Renvoye au client une message de reussite au format JSON: {"success": "successMessage", data: {...}}
     * @param successMessage le message de reussite
     * @param data des données à associées à la reponse
     * @param packet
     * @param socket
     */
    private static void sendSuccess(String successMessage, JSONObject data, DatagramPacket packet, DatagramSocket socket){		
        SendReponse(packet, new JSONObject().put("success", successMessage).put("data", data).toString(), socket);
    }

    /**
     * Envoie un message vers le client
     * @param msg
     * @param data
     * @param socket
     * @throws IOException
     */
    private static void SendReponse(DatagramPacket msg, String data, DatagramSocket socket){
        byte[] tampon = data.getBytes();
        DatagramPacket packetreponse = new DatagramPacket( tampon, tampon.length, msg.getAddress(), msg.getPort() );
        packetreponse.setLength(tampon.length);
        try {
            socket.send(packetreponse);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoie:\n" + e.toString());
        }
    }

    private static void FinActivite(JSONObject json, DatagramPacket msg, DatagramSocket socket) throws IOException {
        String loginU = "";
        String activityU = "";

        if( json.has("login") && !json.getString("login").equals("") ){
            loginU = json.getString("login");
            loginU.replaceAll("[%~/. ]", "");
        }
        else{
            sendError("Vous n'etes pas connecté", msg, socket);
            return;
        }

        if( json.has("activity") ){ activityU = json.getString("activity"); }
        else{
            sendError("Aucune activité envoyé", msg, socket);
            return;
        }

        //Ecriture
        JSONObject jsonEcriture = new JSONObject();
        jsonEcriture.put("closeDate",  Calendar.getInstance().getTime().toString());

        File fichier = new File("activity/"+loginU+"/"+activityU+".json");
        if(fichier.exists()){
            try{
                FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
                ecritureFichier.write(jsonEcriture.toString());
                ecritureFichier.close();
                sendSuccess("Activité fermé", null, msg, socket );
                return;
            }
            catch(IOException e){
                sendError("Une erreur interne au serveur d'autentification est survenu", msg, socket);
                System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+jsonEcriture.toString()+"\n"+e);
                return;
            }
        }else{
            sendError("L'activite "+activityU+" n'existe pas", msg, socket);
            
            return;
        }

    }

    /**
     * Receptionne les nouvelles données GPS et les stocke dans le fichier de l'activité du client
     * @param json
     * @param msg
     * @param socket
     */
    private static void ReceptionActivite(JSONObject json, DatagramPacket msg, DatagramSocket socket) {
        System.out.println("ReceptionActivite: " + json.toString());

        String loginU = "";
        String activityU = "";
        JSONArray newGPSdatas = null;

        if( json.has("login") && !json.getString("login").equals("") ){
            loginU = json.getString("login");
            loginU.replaceAll("[%~/. ]", "");
        }
        else{
            sendError("Vous n'etes pas connecté", msg, socket);
            return;
        }

        if( json.has("activity") ){ activityU = json.getString("activity"); }
        else{
            sendError("Aucune activité envoyé", msg, socket);
            return;
        }

        if( json.has("GPSdata") ){ newGPSdatas = json.getJSONArray("GPSdata"); }
        else{
            sendError("Aucune données GPS envoyé", msg, socket);
            return;
        }

        JSONObject oldFile = null;
        try{
            oldFile = readActivity(loginU, activityU);
        }catch(Exception e){
            sendError(e.getMessage(), msg, socket);
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
                sendSuccess("Données sauvegardées", null, msg, socket);
                return;
            }
            catch(IOException e){
                sendError("Une erreur interne au serveur d'autentification est survenu", msg, socket);
                System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+jsonEcriture.toString()+"\n"+e);
                return;
            }
        }else{
            sendError("L'activite "+activityU+" n'existe pas", msg, socket);
            
            return;
        }

      

    }

    private static void creerActivite(JSONObject jsonQuerry , DatagramPacket msg, DatagramSocket socket) throws JSONException, IOException {
        System.out.println("creerActivite: " + jsonQuerry.toString());
        
        String loginU     = "";
        String activityU  = "";
        
        if( jsonQuerry.has("login") && !jsonQuerry.getString("login").equals("") ){
            loginU = jsonQuerry.getString("login");
            loginU.replaceAll("[%~/. ]", "");
        }
        else{
            sendError("Vous n'etes pas connecté", msg, socket);
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
        //jsonEcriture.put("activity", activityU);
        jsonEcriture.put("creationDate", Calendar.getInstance().getTime().toString() );
        jsonEcriture.put("GPSdata", new JSONArray() );
        File repertoire = new File("activity/"+loginU+"/");

        repertoire.mkdirs();

        File fichier = new File("activity/"+loginU+"/"+activityU+".json");
        if(!fichier.exists()){
            fichier.createNewFile();
        }else{
            sendError("L'activite "+activityU+" existe deja", msg, socket);
            System.out.println("Un utilisateur à tenté de créé une activite mais a échoué: "+activityU+": "+jsonQuerry.toString());
            return;
        }

        try{
            FileWriter ecritureFichier = new FileWriter(fichier.getAbsoluteFile());
            ecritureFichier.write(jsonEcriture.toString());
            ecritureFichier.close();
            sendSuccess("Création d'activite reussi", null, msg, socket);
            return;
        }
        catch(IOException e){
            sendError("Une erreur interne au serveur d'autentification est survenu", msg, socket);
            System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+jsonEcriture.toString()+"\n"+e);
            return;
        }
        
    }

    /**
     * Verifie la requete de login et repond en consequance au client
     * @param data le body de la requete du client
     * @param packet
     * @param socket
     * @throws JSONException
     * @throws IOException
     */
    private static void login(JSONObject jsonQuerry ,DatagramPacket packet, DatagramSocket socket) {
        System.out.println("login: " + jsonQuerry.toString());
        
        String loginU     = "";
        String passwordU = "";

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
            sendSuccess("login reussi", new JSONObject().put("login", loginU), packet, socket);
        }else{
            System.out.println(localJsonUser.getString("password"));
            System.out.println(passwordU);
            sendError("Mauvais mot de passe", packet, socket);
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


    private static JSONObject readJson(String loginU) throws Exception{
        try  {
            return new JSONObject( readFile("users/"+loginU+".json") );
        }
        catch (FileNotFoundException e){
            throw new Exception("L'utilisateur "+loginU+" n'existe  pas.");
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

        return  json;
    }

    private static void creerCompte(JSONObject msg , DatagramPacket packet, DatagramSocket socket) throws JSONException, IOException {

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
            sendSuccess("Création de compte reussi", new JSONObject().put("login", loginU), packet, socket);
            ecritureFichier.close();
            System.out.println("Nouveau utilisateur créé:        "+loginU+": "+json.toString());
            return;
        }
        catch(IOException e){
            sendError("Une erreur interne au serveur est survenu", packet, socket);
            System.out.println("Erreur d'écriture est survenu !\n"+loginU+": "+json.toString()+"\n"+e);
            return;
        }

    }


}
