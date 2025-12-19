package Client.jsonUtils;

import Client.model.Card;
import Client.model.Player;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class jsonReader {


    public jsonReader() {

    }
    // Extrait la commande principale (ex: "AUTH_OK", "CARD_CREATED")
    public String getCommand(String json) {
        return extractValue(json, "cmd");
    }

    // Extrait l'ID pour l'authentification
    public int getAuthId(String json) {
        try {
            return Integer.parseInt(extractValue(json, "user_id"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Extrait le message d'erreur
    public String getErrorMessage(String json) {
        return extractValue(json, "error");
    }

    public ArrayList<Player> parseConnectedPlayers(String json) {
        ArrayList<Player> list = new ArrayList<>();
        String dataContent = extractArrayContent(json, "data"); // Extrait le contenu de [...]

        if (!dataContent.isEmpty()) {
            // On découpe chaque objet {...}
            ArrayList<String> objects = splitJsonObjects(dataContent);
            for (String obj : objects) {
                int id = Integer.parseInt(extractValue(obj, "user_id"));
                String name = extractValue(obj, "user_name");

                if (id != 0) {
                    Player p = new Player(id, name);
                    p.setConnected(true);
                    list.add(p);
                }
            }
        }
        return list;
    }

    public Card parseCard(String json) {
        try {
            int cardId = Integer.parseInt(extractValue(json, "card_id"));
            String name = extractValue(json, "cardName");
            int hp = Integer.parseInt(extractValue(json, "HP"));
            int ap = Integer.parseInt(extractValue(json, "AP"));
            int dp = Integer.parseInt(extractValue(json, "DP"));
            int ownerId = Integer.parseInt(extractValue(json, "user_id"));

            //Retourner la carte avec les bonnes infos
            return new Card(cardId, name, ap, dp, hp, ownerId);

        } catch (Exception e) {
            System.err.println("[JSON_Reader] Error parsing card: " + e.getMessage());
            return null;
        }
    }

    public ArrayList<Card> parseConnectedCards(String json) {
        ArrayList<Card> list = new ArrayList<>();
        String dataContent = extractArrayContent(json, "data");

        if (!dataContent.isEmpty()) {
            ArrayList<String> objects = splitJsonObjects(dataContent);
            for (String obj : objects) {
                Card c = parseCard(obj);

                if (c != null) {
                    // On utilise le constructeur avec ownerId et maxHealth (hp par défaut)
                    list.add(c);
                }
            }
        }
        return list;
    }


    // ID du joueur qui propose l'échange
    public int getTradeSenderId(String json) {
        // DOIT être "fromPlayerId" pour correspondre au paquet du serveur
        String val = extractValue(json, "fromPlayerId");
        if (val.isEmpty()) return -1; // Sécurité
        return Integer.parseInt(val);
    }

    // Nom du joueur qui propose
    public String getTradeSenderName(String json) {
        return extractValue(json, "fromPlayerName");
    }

    // La carte qu'il demande (VOTRE carte)
    public int getTradeOfferedCardId(String json) {
        return Integer.parseInt(extractValue(json, "cardId"));
    }

    // La carte qu'il propose (SA carte)
    public int getTradeRequestedCardId(String json) {
        return Integer.parseInt(extractValue(json, "traderCardId"));
    }

    //private functions

    private String extractValue(String json, String key) {
        //regex to find key and value
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    public int getTradePlayerId1(String json) {
        // ID de celui qui a accepté (ou le premier ID du paquet)
        String val = extractValue(json, "playerId");
        return val.isEmpty() ? -1 : Integer.parseInt(val);
    }

    public int getTradePlayerId2(String json) {
        // ID de celui qui a proposé (traderId)
        String val = extractValue(json, "traderId");
        return val.isEmpty() ? -1 : Integer.parseInt(val);
    }

    public int getTradeCardId1(String json) {
        String val = extractValue(json, "cardId");
        if (val.isEmpty()) {
            val = extractValue(json, "yourCardId");
        }

        return val.isEmpty() ? -1 : Integer.parseInt(val);
    }

    public int getTradeCardId2(String json) {
        // Carte appartenant à PlayerId2 (traderCardId)
        String val = extractValue(json, "traderCardId");
        return val.isEmpty() ? -1 : Integer.parseInt(val);
    }

    // split obj1},{obj2
    private ArrayList<String> splitJsonObjects(String t) {
        ArrayList<String> l = new ArrayList<>();
        int lvl = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : t.toCharArray()) {
            if (c == '{') lvl++;
            if (c == '}') lvl--;
            if (c == ',' && lvl == 0) {
                l.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) l.add(sb.toString());
        return l;
    }

    private String extractArrayContent(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return "";

        int startBracket = json.indexOf("[", keyIndex);
        if (startBracket == -1) return "";

        int endBracket = json.lastIndexOf("]"); // Version simplifiée (suppose que data est le dernier ou principal tableau)
        // Pour être plus robuste sur des JSON complexes imbriqués, il faudrait compter les crochets.
        // Mais vu vos logs, data est le gros bloc principal.

        if (endBracket > startBracket) {
            return json.substring(startBracket + 1, endBracket);
        }
        return "";
    }
}