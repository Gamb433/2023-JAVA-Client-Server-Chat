import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient extends Thread {
    protected int serverPort = 1234;
    private String clientName; //dodano
	private DataOutputStream out; // dodano, povzročalo težave zato je sedaj definirano tukaj

    public static void main(String[] args) throws Exception {
        new ChatClient();
    }

    public ChatClient() throws Exception {
        Scanner sc = new Scanner(System.in); //dodano

        // Ask for client's name
        System.out.print("Vnesite svoje ime: "); //dodano
        clientName = sc.nextLine(); //dodano

        Socket socket = null;
        DataInputStream in = null;
        out = null; //spremenjeno, sedaj je definirano zgoraj v 8 vrstici

        // connect to the chat server
        try {
            System.out.println("[system] connecting to chat server ...");
            socket = new Socket("localhost", serverPort);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("[system] connected");

            // Pošlji ime klienta na server
            out.writeUTF(clientName); //dodano
            out.flush(); //dodano

            ChatClientMessageReceiver message_receiver = new ChatClientMessageReceiver(in);
            message_receiver.start();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // read from STDIN and send messages to the chat server
		/*
		 * Par zadeve spodaj spremenjenih
		 * Sedaj gleda, če se uporabnikov input začne s "private " in če je to true,
		 * kliče metodo sendPrivateMessage, drugače pa vse po starem.
		 */
        BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
		while ((userInput = std_in.readLine()) != null) {
            if (userInput.startsWith("private ")) {
                sendPrivateMessage(userInput); //PRIVATNA SPOROČIL
            } else {
                sendMessage(userInput); //JAVNA SPOROČILA
            }
        }

        // cleanup
        out.close();
        in.close();
        std_in.close();
        socket.close();
    }

    private void sendMessage(String message) {
        try {
            out.writeUTF(message); //spremenjeno
            out.flush();
        } catch (IOException e) {
            System.err.println("[system] could not send message");
            e.printStackTrace(System.err);
        }
    }

	/*
	 * Dodana nova metoda sendPrivateMessage, ki se kliče zgoraj,
	 * če se input začne s "private ".
	 * Sporočilo na server pošlje na isti način kot zgornja metoda sendMessage.
	 * 
	 */
	private void sendPrivateMessage(String message) {
        try {
            out.writeUTF(message); // Send the private message directly
            out.flush();
        } catch (IOException e) {
            System.err.println("[system] could not send private message");
            e.printStackTrace(System.err);
        }
    }
}

class ChatClientMessageReceiver extends Thread {
    private DataInputStream in;

    public ChatClientMessageReceiver(DataInputStream in) {
        this.in = in;
    }

    public void run() {
        try {
            String message;
            while ((message = this.in.readUTF()) != null) {
                System.out.println(message);
            }
        } catch (Exception e) {
            System.err.println("[system] could not read message");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
