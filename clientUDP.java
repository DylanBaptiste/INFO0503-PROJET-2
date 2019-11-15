package com.test;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ClientUDP {

	public final int portEcoute = 2000;
	public final int TIME_OUT_DELAY = 3000;
	public final int AMOUNT_TO_SEND_GPSDATA = 3;

	private String login = "";
	private String activity;
	private DatagramSocket socket = null;
	private List<GPSdata> GPSdataList = new ArrayList<GPSdata>();

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		System.out.println(login);
		this.login = login;
	}

	public void deconnexion() {
		this.login = "";
	}

	private void setActivity(String a) {
		this.activity = a;
	}

	private void clearActivity() {
		this.activity = "";
	}

	public ClientUDP() {
		try {
			this.socket = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println("Erreur lors de la création de la socket : " + e);
			System.exit(-1);
		}
	}

	public String seConnecter(Scanner saisieUtilisateur) {

		System.out.print("login: ");
		String RecupLogin = saisieUtilisateur.nextLine();
		System.out.print("mot de passe: ");
		String RecupPassword = saisieUtilisateur.nextLine();

		JSONObject data = new JSONObject().put("action", 1).put("login", RecupLogin).put("password", RecupPassword);

		try {
			return sendUDPWithResponse(data.toString());
		} catch (Exception e) {
			return e.getMessage();
		}

	}

	public String creerCompte(Scanner saisieUtilisateur) {

		System.out.print("login: ");
		String login = saisieUtilisateur.nextLine();
		System.out.print("password: ");
		String password = saisieUtilisateur.nextLine();
		System.out.print("passwordConfirm");
		String passwordConfirm = saisieUtilisateur.nextLine();

		JSONObject data = new JSONObject().put("action", 2).put("login", login).put("password", password)
				.put("passwordConfirm", passwordConfirm);

		try {
			return sendUDPWithResponse(data.toString());
		} catch (Exception e) {
			return e.getMessage();
		}

	}

	// throws une exception car si il y a un probleme je veux pas juste
	// afficher l'erreur comme dans seConnecter ou creerCompte
	public String startActivity(Scanner saisieUtilisateur) throws Exception {

		System.out.print("Activité: ");
		String activity = saisieUtilisateur.nextLine();

		this.setActivity(activity);

		JSONObject data = new JSONObject().put("action", 3).put("login", this.login).put("activity", this.activity);

		try {
			return sendUDPWithResponse(data.toString());
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public String closeActivity() throws Exception {

		if (this.GPSdataList.size() > 0) {
			try {
				this.sendGPSdata();
			} catch (Exception e) {
				throw new Exception("Impossible d'envoyer les "+this.GPSdataList.size()+" dernières données GPS avant de fermer l'activité.");
			}
		}

		JSONObject data = new JSONObject().put("action", 5).put("login", this.login).put("activity", this.activity);;

		try {
			String res = sendUDPWithResponse(data.toString());
			this.clearActivity();
			return res;
		} catch (Exception e) {
			throw new Exception("Impossible de fermer l'activité: " + e.getMessage());
		}
	}

	public void sendGPSdata() throws Exception {

	
		JSONArray jsonGPSdata = new JSONArray();
		for (GPSdata element : this.GPSdataList) {
			jsonGPSdata.put(element.toJSON());
		}

		JSONObject data = new JSONObject().put("action", 4).put("login", this.login).put("activity", this.activity).put("GPSdata", jsonGPSdata);

		try {
			this.sendUDPWithResponse(data.toString());
			this.GPSdataList.clear();
		} catch (Exception e) {
			throw new Exception("Impossible d'envoyer les " + this.GPSdataList.size() + " dernieres données GPS.");
		}

	}

	public void saveGPSdata(Scanner saisieUtilisateur) {

		System.out.print("Latitude: ");
		float latitude = saisieUtilisateur.nextFloat();
		System.out.print("Longitude: ");
		float longitude = saisieUtilisateur.nextFloat();

		saisieUtilisateur.nextLine();

		this.GPSdataList.add(new GPSdata(latitude, longitude));

		// tout les X nouveaux ajouts on tente d'envoyer
		if (this.GPSdataList.size() % AMOUNT_TO_SEND_GPSDATA == 0) {
			try {
				this.sendGPSdata();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
	}
	
	/**
	 * Envoie une request UDP
	 * @param data le JSON stringifié
	 * @throws Exception UnknownHostException ou IOException
	 */
	public void sendUDP(String data) throws Exception {
		
		System.out.print("\nEnvoie...");
		
		try {
            
		    byte[] tampon = data.getBytes();
		    DatagramPacket dataRequest = new DatagramPacket(tampon, tampon.length, InetAddress.getByName(null), portEcoute);
			this.socket.send(dataRequest); 
			System.out.println("réussi.");
		} catch(UnknownHostException e) {
            System.out.println("Erreur lors de la création du message : " + e);
            throw e;
		} catch(IOException e) {
            System.out.println("Erreur lors de l'envoi du message : " + e);
            throw e;
		}
	}
	
	/**
	 * Envoie une request UDP et attend une réponse
	 * @param data le JSON stringifié
	 * @return le réponse du serveur
	 * @throws Exception SocketTimeoutException ou IOException
	 */
	public String sendUDPWithResponse(String data) throws Exception{
		
		try{
			sendUDP(data);
		} catch(Exception e){
			throw new Exception("La fonction sendUDP à échoué dans sendUDPWithResponse: " + e.getMessage());
		}
		
        byte[] tampon = new byte[800];
        DatagramPacket response = new DatagramPacket(tampon, tampon.length);
        try {
			this.socket.setSoTimeout(TIME_OUT_DELAY);
			this.socket.receive(response);
		} catch (SocketTimeoutException e) {
			throw new Exception("Serveur Timeout !");
		} catch (IOException e) {
			throw new Exception(e.toString());
		}
        
        return new String(response.getData(), 0, response.getLength());
	}
	
	public void displayMenu(String r){

		System.out.println("\n\n\n"+r);
		System.out.print("\n----======= MENU =======-----");
		System.out.print( this.login.equals("") ?  "\n| (1) login" : "\n| Vous êtes connecté en tant que " +this.login);
		System.out.print( this.login.equals("") ?  "\n| (2) Créer un compte" : "");
		System.out.print( this.login.equals("") ?  "" : "\n| (3) Commencer une activité");
		System.out.print("\n| (8) quitter");  
		System.out.print( this.login.equals("") ?  "" : "\n| (9) Se déconnecter");
		System.out.print("\n----======= **** =======-----\n\n");

	}
	

}

