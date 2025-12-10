package Client;

import java.io.*;
import java.net.Socket;

public class Client {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 7777;


        try {
            // --- Étape 1 : Création du fichier data.json directement ---
            File file = new File("Data_Files/data.json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("{\"message\": \"AUTH\"}");
            }
            System.out.println("Fichier data.json créé et rempli avec un message JSON.");

            // --- Étape 2 : Connexion au serveur ---
            Socket socket = new Socket(host, port);
            System.out.println("Connecté au serveur " + host + ":" + port);

            // Flux d’envoi
            OutputStream outputStream = socket.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            // Flux de réception
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // --- Étape 3 : Lecture du fichier JSON ---
            StringBuilder sb = new StringBuilder();
            try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String message = sb.toString();

            // --- Étape 4 : Envoi du message ---
            System.out.println("Envoi du message JSON au serveur...");
            writer.write(message);
            writer.newLine(); // marque la fin du message
            writer.flush();

            // --- Étape 5 : Réception de la réponse ---
            String response = reader.readLine();
            System.out.println("Réponse du serveur : " + response);

            // --- Étape 6 : Fermeture ---
            socket.close();
            System.out.println("Connexion fermée.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
