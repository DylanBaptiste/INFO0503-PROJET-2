package com.test;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

public class AppUDP {
	public static void main(String[] args) {

		

		Scanner saisieUtilisateur = new Scanner(System.in);

		ClientUDP client = new ClientUDP();
		int recupOption = 0;
		String reponse = "";
		do {
			client.displayMenu(reponse);
			reponse = "";
			System.out.print("Votre choix: ");
			recupOption = Integer.parseInt(saisieUtilisateur.nextLine());

			switch (recupOption) {
				case 1:
					reponse = client.seConnecter(saisieUtilisateur);
					try{
						client.setLogin(new JSONObject(reponse).getJSONObject("data").getString("login"));
					}
					catch(Exception e){}
					reponse = manageResult(reponse);
					break;

				case 2: 
					reponse = client.creerCompte(saisieUtilisateur);
					try{
						client.setLogin(new JSONObject(reponse).getJSONObject("data").getString("login"));
					}
					catch(Exception e){}
					reponse = manageResult(reponse);
					break;

				case 3:
					try {
						boolean ok = false;
						String res = client.startActivity(saisieUtilisateur);
						System.out.println(manageResult(res));
						try{
							ok = new JSONObject(res).has("success");
						}
						catch(Exception e){}
						
						if(ok){
							String input = "";
							while(true){
								System.out.print("Recolter mes données GPS (y/n): ");
								input = saisieUtilisateur.nextLine();
								
								if(input.equals("y")){
									client.saveGPSdata(saisieUtilisateur);
								}else{
									reponse = client.closeActivity();
									reponse = manageResult(reponse);
									break;
								}
								input = "";
								TimeUnit.SECONDS.sleep(1);
							}
						}

					} catch (Exception e) {
						reponse = e.getMessage();
					}
					
					break;
				
				
				case 8: System.out.println("Salut mon pote."); break;
				case 9: client.deconnexion(); break;
				default: System.out.println("Cette action n'existe pas."); break;
        	}
            
        }while(recupOption != 8);
        
	}
	
	private static String getKey(JSONObject json, String key){
		return json.has(key) ? (String) json.optString(key) : "";
	}

	private static String manageResult(String resultRequest){
		try{
			JSONObject jsonRequest = new JSONObject(resultRequest);
			String success = getKey(jsonRequest, "success");
			String error   = getKey(jsonRequest, "error");

			if(success != ""){
				resultRequest = "Succes: "+ success;
			}else{
				resultRequest = "Erreur: "+ error;
			}
		}catch(JSONException e){
			resultRequest += "";
		}
		
		return resultRequest;
	}

}
