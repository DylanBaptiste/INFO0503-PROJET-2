package com.test;

import java.util.Scanner;

public class clientUDP {

	public static void main(String[] args) {

		Scanner saisieUtilisateur = new Scanner(System.in);

		Client client = new Client();
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
					//si success client.setLogin
					break;

				case 2: 
					reponse = client.creerCompte(saisieUtilisateur);
					//si success client.setLogin
					break;

				case 3:
					try {
						System.out.println(client.startActivity(saisieUtilisateur));
						
						while(true){
							System.out.println("Envoyer les données GPS (y/n)");
							if(!saisieUtilisateur.nextLine().equals("n")){
								client.sendGPSdata(saisieUtilisateur);
							}else{
								//demander au serveur la fermeture de l'activité ?
								break;
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
       

        

        // Fermeture de la socket
        
    }

}
