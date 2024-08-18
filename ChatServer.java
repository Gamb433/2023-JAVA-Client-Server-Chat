import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat; //dodano
import java.util.Date; //dodano


public class ChatServer {

    protected int serverPort = 1234;
    protected List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new ChatServer();
    }

    public ChatServer() {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(this.serverPort);
        } catch (Exception e) {
            System.err.println("[system] could not create socket on port " + this.serverPort);
            e.printStackTrace(System.err);
            System.exit(1);
        }

        System.out.println("[system] listening ...");
        try {
            while (true) {
                Socket newClientSocket = serverSocket.accept();
                ClientHandler newClient = new ClientHandler(this, newClientSocket);
                clients.add(newClient);
                newClient.start();
            }
        } catch (Exception e) {
            System.err.println("[error] Accept failed.");
            e.printStackTrace(System.err);
            System.exit(1);
        }

        System.out.println("[system] closing server socket ...");
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public synchronized void sendToAllClients(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

	/*
	 * Nova metoda, ki pošlje privatno sporočilo navedenemu uporabniku. Kliče se iz handlePrivateMessage
	 * pogleda če se vnešeno ime recipienta ujema z bazo klientov v seznamu "client" in če se pošlje,
	 * drugače pa vrne error.
	 */
    public synchronized void sendPrivateMessage(String senderName, String recipientName, String message) {
        for (ClientHandler client : clients) {
            if (client.getClientName().equals(recipientName)) {
                client.sendMessage("[private from " + senderName + "] : " + message); //sporočilo prikazno v konzoli clienta
                return;
            }
        }
        // Če recipient ni najden vrne error
        for (ClientHandler client : clients) {
            if (client.getClientName().equals(senderName)) {
                client.sendMessage("[system] Error: Prejemnik " + recipientName + " ni bil najden.");
                return;
            }
        }
    }


	/*
	 * Nova oz. spremenjena metoda, ki v primeru, da se kliče removeClient
	 * prikaže sporočilo Uporabnik {ime} nas je zapustil.
	 */
    public synchronized void removeClient(ClientHandler client) {
		// Pridobi ime clienta, ki nas je zapustil
		String disconnectedClientName = client.getClientName();
		
		// Ga odstrani
		clients.remove(client);
		
		// Obvesti vse ostale kliente
		String disconnectionMessage = "[system] Uporabnik " + disconnectedClientName + " nas je zapustil.";
		for (ClientHandler klient : clients) {
			klient.sendMessage(disconnectionMessage);
		}
	}
}

class ClientHandler extends Thread {
    private ChatServer server;
    private Socket socket;
    private String clientName; //dodano

    public ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

	//dodano
    public String getClientName() {
        return clientName;
    }


	/*
	 * Metoda precej spremenjena
	 * glej komentarje spodaj
	 */
    public void run() {
        System.out.println(getCurrentTime() + " [system] connected with localhost:" + this.socket.getPort());

        try {
            DataInputStream in = new DataInputStream(this.socket.getInputStream());
            DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());

            // Server sedaj najprej pridobi klientovo ime
            this.clientName = in.readUTF();
            // in pošlje vsem drugim klientom obvestilo
            server.sendToAllClients("[system] " + clientName + " se je pridružil klepetu.");
			
            while (true) {
				String receivedMessage;
				try {
					receivedMessage = in.readUTF();
				} catch (EOFException e) {
					// Če daš CTRL+C sedaj izpiše v server konzolo:
					System.out.println(getCurrentTime() + " [system] disconnected with " + clientName + ":" + this.socket.getPort());
					server.removeClient(this);
					break;
				}
				
				/*
				//PRIVATNA SPOROČILA
				 * pogleda, če se začne s "private " in v tem primeru
				 * kliče metodo handlePrivateMessage
				 */
				if (receivedMessage.startsWith("private ")) {
					handlePrivateMessage(receivedMessage);
				} else {
					String publicMessageForServer = getCurrentTime() + " " + clientName + "[" + this.socket.getPort() + "] : " + receivedMessage;
					String publicMessageForClients = clientName + " : " + receivedMessage;
					System.out.println(publicMessageForServer); // Sporočilo v server konzoli
					server.sendToAllClients(publicMessageForClients); //Sporočilo za vse ostale kliente
				}
			}
			
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			server.removeClient(this);
		}
    }


	/*
	 * Se kliče zgoraj v metodi run. 
	 * Metoda razdeli sporočilo na tri dele, saj je tak regex klica privatnega sporočila.
	 * Ker besede "private" ne rabimo vzame samo drugi in tretji element niza.
	 * Torej naslovnika in sporočilo ter ju shrani v posebni spremenljivki.
	 */
    private void handlePrivateMessage(String message) {
        String[] parts = message.split(" ", 3); // Razdeli na ["private", prejemnik, privatno_sporocilo]
        if (parts.length < 3) {
            sendMessage("[system] Error: Napačen format privatnega sporočila.");
            return;
        }
        String recipient = parts[1];
        String privateMessage = parts[2];
        String formattedPrivateMessage = getCurrentTime() + " [private from " + clientName + " to " + recipient + "] : " + privateMessage;
        System.out.println(formattedPrivateMessage); // Privatno sporocilo skupaj s prejemniki in naslovniki v server konzoli
        server.sendPrivateMessage(clientName, recipient, privateMessage); //tole vrže v drugo metodo, ki je zadolžena za dejansko pošiljanje
    }
	

    /*
     * predloga malo spremenjena, dodan čas ipd
     */
    public void sendMessage(String message) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            String timestampedMessage = getCurrentTime() + " " + message;
            out.writeUTF(timestampedMessage);
            out.flush();
        } catch (IOException e) {
            System.err.println("[system] could not send message to client");
            e.printStackTrace(System.err);
        }
    }
    
    /*
     * Nova metoda, ki pridobi trenutni čas in ga formatira
     */
    private String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date currentTime = new Date(System.currentTimeMillis());
        return formatter.format(currentTime);
    }
    
}
