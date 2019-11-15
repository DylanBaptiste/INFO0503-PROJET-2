package ClientTCP;



import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

public class Client {

	public final int portEcoute = 5001;
	public final int TIME_OUT_DELAY = 1000;
	public final int AMOUNT_TO_SEND_GPSDATA = 5;

	private String login;
	private String activity;
	private Socket socket = null;
	BufferedReader reader = null;
    PrintWriter writer = null;
	private List<GPSdata> GPSdataList = new ArrayList<GPSdata>();

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
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

	public Client() {
		try {
            socket = new Socket("localhost", portEcoute);
        } catch(UnknownHostException e) {
            System.err.println("Erreur sur l'hôte : " + e);
            System.exit(0);
        } catch(IOException e) {
            System.err.println("Création de la socket impossible : " + e);
            System.exit(0);
        }
		try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        } catch(IOException e) {
            System.err.println("Association des flux impossible : " + e);
            System.exit(0);
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
	/*public void test() {
		
	
		
		
		String message = "{\"action\":\"1\",\"creationDate\":\"Thu Nov 14 11:45:26 CET 2019\"}";
		writer.println(message);
		//writer.flush();
		
		 String messageR = "";
	        try {
	            messageR = reader.readLine();
	        } catch(IOException e) {
	            System.err.println("Erreur lors de la lecture : " + e);
	            
	        }
	    	//JSONObject json = new JSONObject(message);
	        System.out.print(messageR);
	        return;
		
		
		
		
	}*/

	public String creerCompte(Scanner saisieUtilisateur) {

		System.out.print("login: ");
		String login = saisieUtilisateur.nextLine();
		System.out.print("password: ");
		String password = saisieUtilisateur.nextLine();
		System.out.print("passwordConfirm: ");
		String passwordConfirm = saisieUtilisateur.nextLine();

		JSONObject data = new JSONObject().put("action", 2).put("login", login).put("password", password)
				.put("passwordConfirm", passwordConfirm);

		try {
			return sendUDPWithResponse(data.toString());
		} catch (Exception e) {
			return e.getMessage();
		}

	}

	// Là je throws une exception car si il y a un probleme je veux pas juste
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

		JSONObject data = new JSONObject().put("action", 6).put("login", this.login);

		try {
			String res = sendUDPWithResponse(data.toString());
			this.clearActivity();
			return res;
		} catch (Exception e) {
			throw new Exception("Impossible de fermer l'activité: " + e.getMessage());
		}
	}

	public void sendGPSdata() throws Exception {
		JSONObject data = new JSONObject().put("action", 5).put("login", this.login).put("GPSdata",
				new JSONArray(this.GPSdataList));

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

		this.GPSdataList.add(new GPSdata(latitude, longitude));

		// tout les 5 nouveaux ajouts on tente d'envoyer
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
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        } catch(IOException e) {
            System.err.println("Association des flux impossible : " + e);
            System.exit(0);
        }
        writer.println(data);
        
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
	 
        String message = "";
        try {
            message = reader.readLine();
        } catch(IOException e) {
            System.err.println("Erreur lors de la lecture : " + e);
            System.exit(0);
        }
    	//JSONObject json = new JSONObject(message);
        
        return new String(message);
	}
	
	public void displayMenu(String r){

		System.out.println("\n\n\n\n"+r);
		
		System.out.print("\n----======= MENU =======-----");
		System.out.print( this.login != "" ?  "\n| (1) login" : "\n| Vous êtes connecté en tant que " +this.login);
		System.out.print( this.login != "" ?  "\n| (2) Créer un compte" : "");
		System.out.print( this.login == "" ?  "\n| (3) Commencer une activité" : "");
		System.out.print("\n| (8) quitter");  
		System.out.print( this.login == "" ?  "\n| (9) Se déconnecter" : "");
		System.out.print("\n----======= **** =======-----\n\n");

	}
	
	
	public void aurevoir() {
		String data = "{\"activity\":\"iojff\",\"action\":8,\"login\":\"toto\"}";
		try {
			sendUDP(data);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
            reader.close();
            writer.close();
            socket.close();
        } catch(IOException e) {
            System.err.println("Erreur lors de la fermeture des flux et des sockets : " + e);
            System.exit(0);
        }
		System.out.println(
				"-~~~~~.....~~~-~~~......^^._=*i;;++::::~.~-;+====+i*eeo*=i=*o*i**e!eee!!ee!!!!!eoiiioooooooooooo****\n" + 
				"-----~~~.~~~~~~~~~........;=i*i+++=:_:_~.~-;++=io*eeeeeoiii*o*i**eeee!???eee!!??*iiiioooooooooooo***\n" + 
				"__-----~~~~~~~~~~~~~~~~~.-**oi=++==__:-~.~-;io*i*e***!*ooioooooo*o**iio!?!!!??%%eiiiiiooooooooooooo*\n" + 
				"::___--------~~~~~~~~~~~~+!!oi=++;:__:~..~-;oo**!ee**ooooiiiii===;;___:=e%??!!??eoiiiiiiooooooooooo*\n" + 
				";::____------~~~~~~~-~~~-o?eoo_:_-~-::~..~-=io**e!eo+:++;+;++:_--~~~--__;e!??!??!eoiiiiiioooooooooo*\n" + 
				";;:::___-----~~~-------~:e!*o;-__---__...~-+=o**!eo;:____:::_~~~.~~~--__:+!ee!!!!eoiiiiioiioooooooo*\n" + 
				";;;;;:::___--------_---~+!!=:__::_--__..~-_+i*ee!e+_~-~~~----~~...~~---__:o!*o*e!*iiiiiiiiioooooooo*\n" + 
				";;++;;;;:::_::__:____-_-o!*::::;;:__:_..~;_-:oe*!i_~....~~-~~~.~~~~~---_::+!*o**!eoiiiiiiiioooooooo*\n" + 
				":;;;++++;;;;;;;::::___-_oeo-:+++;::;:_..::__:o*o*+-..~.~~~~~~~~~~~~~--__::;i!*o*eeoiiiiiiooooooooo**\n" + 
				":_::;++++++;+;;:::___-~:**;_+===++=;__.~+_;=i*i**:~..~~-----~~~~~-----__::;=****e*oiiiiiooooooooo***\n" + 
				"___::::::_____------~~~+i++===++++;--:.:++ioioo!i_~~-~~---__-------_____::;=oe**oo*iiiiiioooooooo***\n" + 
				"--____---~~~~~~~~.....~+oooi;:;;:;:_:;~==oeeoo*?i__-____-_____--_______:_:+=i*eeeooiiiiiioooooooo***\n" + 
				"--------~~~~~~~~~.....-ioii+::::;;:::;_;ie!e*o*!i:____:::_:;::::_-_____:::;=i*!!eeoiiiiiioooooooo***\n" + 
				"__------~~~~~~~~......_++;::__:;;::;:=io*!!!e*e*i::_::::::__:_-__-_--__::::+io!ee!o=iiiioiooooooo***\n" + 
				"_------~~~~~~~...~~.^~-__:::_::;:_:;;=ooo**!eeeoi:_:___:______-----~-_:::::;+o*o*eiiiiiiiioooooooo*o\n" + 
				"----~~~~~~~~~~...~~..~:;;;:__;;;:::;:;+io*=o*ee*=::;:___---------~--;+;:::::;i*oio=iiiiiiiiiiooooooo\n" + 
				"-~~~~~~~.............-:++;:__;;:::_:;:;=oi+iio*o+:_::__-~----___;+=eeo=;___::=**oi=iiiiiiiiiiooooooo\n" + 
				"~~~~~~~.......^^^....-;++:::_:::____;;+io+===o=i+:::_;:_:::__::+o!!*i++;:_:::=*e*+=iiiiiiiiiiioooooo\n" + 
				"~~~~~~..^^..^^^^^^^^._;++:::___:__:;;=i*==i==++=+-::i*ee!!o+;;+ie?%?e*eo+:::;+=*i+====iiiiiiiioooooo\n" + 
				"-~~~~~......^^^^^^^.~:+++;;____:;;;+=io==oii===oo_:;i*!$$$$!i++*e%%##$*=::::;;:=;=======iiiiiioooooo\n" + 
				"~~~~.....^^.^^^^^^..~_+==+:__::;++==io+=ioiii==oe;+=*eoe%OOOe;:;*??!%i;___::::;+;========iiiiioooooo\n" + 
				"~~~~.....^^^^^^^^^.^~:ioi;:__;;+==ioi__=iiiii=+=*+;i**%##!?Oe;-_+*e*i=;____:::;-:=========iiiiiooooo\n" + 
				"~~~~......^^^^^^^~..-;i*o+;;;+=iio*=~._=ioooi=+=i+:;__=ioo**+---_o!!ei+:____:_:::=========iiiiiioooo\n" + 
				"~~~~......^^^^^^~~~~_+*e*oiiiii*e*:..~:=iiii===+;+:__:+o*oo;:-~---:==+:_-___:_-_;=========iiiiiioooo\n" + 
				"~~~~......^^^^^.-~~~:oeeee*****e;~^^.-:=ioi====+:::_-:=ii+__:~~----~~--___:::___+=========iiiiiiiooo\n" + 
				"-~~~......^^^^^~-~~-+*ee!!eee!e:.....~:=oooiii=++;:___-----::-~-___---__::;:::o++=========iiiiiiiooo\n" + 
				"-~~~...........--~~:i*ie!e**e!eo=+ii=oo****oii=++__:::____:=:_-_:;+;:::;;++:::====+==========iiiiooo\n" + 
				"~~~~~........._-~~-+oo*ee***!!?%$O$$OOOOOOOOO$?e*;:+;;;;;+i*+_-_:;i=+;;++=;;;;====++========iiiiiooo\n" + 
				"~~~~.......-+.;-~_;i*ee!!!e!!?%%$#####&&&&&&&@&&&$o=i++++=o*:-_-__++i=++=+;;;+====++========iiiioooo\n" + 
				"~~~~......;e`:+-_;=oeeee!!ee?%%$$#&###&&&&&@@@@@@@%+==oiioi:-:__:::_;+==+;;;;+++=++++========iiiiiio\n" + 
				"~~~~.....i$~ ;+_:+i****e!!e?%$$$O#&##&&&&@@@@@@@@@&++=o*i=;:;++++=:_:;++;;;;+=+++++++========iiiiiii\n" + 
				"~~~~..~~e#*  :+_:=o****ee*!$O##OO#&&&&&&&@@@@@@@@@@o+==oo=+=ie!!e=;;;;;;;;;+=!+++++++++=======iiiiii\n" + 
				"---~~.~e#O~  .=;+io******o%$$OOOOO&&&&&&&@@@@@@@@@@!===i*i==o*?!=;;;;+;:;++==O!+++++++========iiiiii\n" + 
				"-__-~~*##e^   ;+=ioo***oio%$$$$$O#&&&&&&&@@@@@@@@@@O+iiii===ii=::::;+==+++=i_OO?=+++++========iiiiii\n" + 
				"::_--e#&#o^   ^+=ioooooi_%###O$OO#&&&&&&&&@@@@@@@@@&o=oo*e*ii==;=o*i==i===i=^OOO$%eo=++=======iiiiii\n" + 
				"::__e#&&#o.  `^.+==iii:~o$$$O#####&#&&&&&&@@@@@@@@@&$=i*ee*i=!$$%?=+=++++=i: $OO#OOO$?eoi======iiiii\n" + 
				":::+#&&&Oo~``^~~..~~-~~-$$OO##&&##&#&&&&&&@@@@@@@@@&&!=ooii*i++===ioi=++=i=` $OO#######O$%!e*oiiiiii\n" + 
				";;:+#&&&$*:^.~-__-^.~-~i#OOO#&&&&#&##&&&&&@@@@&@@@@@&&eioi=*!!*ooe*i++;+ii. `$OO##########OO$$%?eoii\n" + 
				";+:=&&&&$*=~~-;;:::~.~-O######&&&&&##&&&&&@@&&&@@@@@&&#*ooi=ooo%%o;:::;=i-  ^OOO#####&&&######OO$$%?\n" + 
				";;:e&&&&!e*;-:++::;;_~?&&######&&&&&#&&&&&&@&&&@@@@@&&&#*oo++;;==+;::;=i-   :OOO#####&&&&&&&&&####O$\n" + 
				";:+O&&&&%!eo;+=;:_;;;!&&&&&###&&&&&&#&&&&&&&&&&@&&&&&&&&#eoo+;=iii=+++=~    =OOO#####&&&&&&&&&&&&&##\n" + 
				";:e#&&&#$%?eo++=++++!&&&&&&####&&&&&##&&&&&&&&&&&&&&&&&&&$+*ooeeeee*o=.`    *#OOO####&&&&&&&&&&&&&&#\n" + 
				";;?&&&&OOOO%*i=i=+o$&&&&&&&&##&&&&@@&##&&&&&&&&&&&&&&&&&##:~oe!?%??!;``    `!O##O####&&&&&&&&&&&&&&&\n" + 
				";+O&&&&&&&&O%*i+;e#&&&&&&&&&&#&&&&@@&####&&&&&&&#####&&##&e^^;?O#OO$i      ^%O#######&&&&&&&&&&&&&&&\n" + 
				"+o#&&&&&&###!!!?O&&&@@@&&@&&&&&&&&@@@&###&&&&&&&######&&#&$_`.!O&#$!e`     ~$#########&&&&&&&&&&&&&&\n" + 
				"=!&&&&#&&&#&&&&&&&&&@@@&@@@&&&&&&@@@@&###&&&&&&&&&&&&&&&##!i`*!!e****+     _OO#####&&&&&&&&&&&&&&&&&\n" + 
				"i%&&&&#&&&&&&&&&&&&@@@@&@@@@&&&&@@@@@&####&&&&&&&&&&&&&&##___?%eooo***^   `+OO####&&&&&&&&&&&&&&&&&&\n" + 
				"i%#&&&&&&&&&&&&&&&@@@@@@@@@@&&&&@@@@@@&####&&&&&&&&&&&&&&#;~oe?!*oo**~-   ^iOO####&&&&&&&&&&&&&&&&&&\n" + 
				"i!#&&&&&&&&&&&&&&&&@@&@@@@@@&&&&@@@@@@&&###&&&&&&&&&&&&&&#i~._;eeooo* ~`  ^oO#####&&&&&&&&&&&&&&&&&&\n" + 
				"==!#&&&&&&&&&&&&&&&@&@&&&@&@@@@@@@@@@@@&####&&&&&&&&#&&&&#!^^~^;?eeee  . `^eO#####&&&&&&&&&&&&&&&&&&\n" + 
				"");
		
		
	}
	

}
