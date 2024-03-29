package com.test;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;


public class ServeurTCPMulti {

    public static final int portEcoute = 2001;

    public static void main(String[] args) {
        // Création de la socket serveur
        ServerSocket socketServeur = null;
        try {    
            socketServeur = new ServerSocket(portEcoute);
            System.out.println("En ecoute sur " + portEcoute);
        } catch(IOException e) {
            System.err.println("Création de la socket impossible : " + e);
            System.exit(0);
        }

        // Attente des connexions des clients
        try {
            Socket socketClient;
            while(true) {
                
                socketClient = socketServeur.accept();
                System.out.println("Recu");
                ThreadConnexion t = new ThreadConnexion(socketClient);
                t.start();
            }
        } catch(IOException e) {
            System.err.println("Erreur lors de l'attente d'une connexion : " + e);
            
        }
    
        // Fermeture de la socket
        try {
            socketServeur.close();
        } catch(IOException e) {
            System.err.println("Erreur lors de la fermeture de la socket : " + e);
            System.exit(0);
        }
    }

}