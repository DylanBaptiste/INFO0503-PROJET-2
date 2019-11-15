package ClientTCP;



import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

public class ClientTCP {
	private static String getKey(JSONObject json, String key){
		return json.has(key) ? json.optString(key) : "";
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

	public static void main(String[] args) {

		Scanner saisieUtilisateur = new Scanner(System.in);
		
		Client client = new Client();
		
		int recupOption = 0;
		String resultRequest = "";
		do {
			client.displayMenu(resultRequest);
			resultRequest = "";
			System.out.print("Votre choix: ");
			recupOption = Integer.parseInt(saisieUtilisateur.nextLine());

			switch (recupOption) {
				case 1:
				{
				resultRequest = client.seConnecter(saisieUtilisateur);
					System.out.println("le resultat recu :"+ resultRequest);
					try{
						JSONObject jsonRequest = new JSONObject(resultRequest);
						client.setLogin(getKey(jsonRequest,"id"));
						
					}
					catch(Exception e){}

					resultRequest = manageResult(resultRequest);
					
					break;
				}
					
					
					

				case 2: 
				{
					resultRequest = client.creerCompte(saisieUtilisateur);
					try{
						JSONObject jsonRequest = new JSONObject(resultRequest);
						client.setLogin(getKey(jsonRequest, "id"));
						
					}
					catch(Exception e){}

					resultRequest = manageResult(resultRequest);
					
					break;
				}
				case 3:
					try {
						System.out.println(client.startActivity(saisieUtilisateur));
						
						while(true){
							System.out.println("Données GPS (y/n)");
							if(!saisieUtilisateur.nextLine().equals("n")){
								client.saveGPSdata(saisieUtilisateur);
							}else{
								//demander au serveur la fermeture de l'activité ?
								//client.sendGPSdata(); -> je le fait dans le closeActivity
								client.closeActivity();
								break;
							}
							
						}

					} catch (Exception e) {
						resultRequest = e.getMessage();
					}
					
					break;
				
				
				case 8: 
				System.out.println("Salut mon pote.");
				client.aurevoir();
				
				break;
				case 9: client.deconnexion(); 
				break;
				default: System.out.println("Cette action n'existe pas."); 
				break;
        	}
            
        }while(recupOption != 8);
       

        

        // Fermeture de la socket
        
    }

}