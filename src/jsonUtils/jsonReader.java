package jsonUtils;

import model.Card;
import model.Player;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class jsonReader {

    // --- PUBLIC API ---

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

    // Parse un message CARD_CREATED pour renvoyer une Carte
    public Card parseCardCreated(String json) {
        return parseSingleCard(json);
    }

    // Parse la liste complète des joueurs (PLAYERS_UPDATE)
    public ArrayList<Player> parsePlayersList(String json) {
        ArrayList<Player> list = new ArrayList<>();
        try {
            // On cherche le contenu du tableau principal "data": [ ... ] ou juste [ ... ]
            // Recherche basique du premier crochet ouvrant et dernier fermant
            int start = json.indexOf("[");
            int end = json.lastIndexOf("]");

            if (start == -1 || end == -1) {
                return list;
            }

            String content = json.substring(start + 1, end);
            ArrayList<String> pJsons = splitJsonObjects(content);

            for (String pj : pJsons) {
                // Création du Joueur
                String idStr = extractValue(pj, "id");
                if (idStr.isEmpty()) continue;

                int id = Integer.parseInt(idStr);
                String name = extractValue(pj, "user");

                Player p = new Player(id, name);
                p.setConnected(true);

                // Parsing des cartes de ce joueur
                int cS = pj.indexOf("[");
                int cE = pj.lastIndexOf("]");
                if (cS != -1 && cE != -1) {
                    String cContent = pj.substring(cS + 1, cE);
                    for (String cj : splitJsonObjects(cContent)) {
                        if (!cj.trim().isEmpty()) {
                            Card c = parseSingleCard(cj);
                            if (c != null) p.getHand().addCard(c);
                        }
                    }
                }
                list.add(p);
            }
        } catch (Exception e) {
            System.err.println("[JSON_Reader] Error parsing players list: " + e.getMessage());
        }
        return list;
    }

    public ArrayList<Player> parseConnectedPlayers(String json) {
        ArrayList<Player> list = new ArrayList<>();
        String dataContent = extractArrayContent(json, "data"); // Extrait le contenu de [...]

        if (!dataContent.isEmpty()) {
            // On découpe chaque objet {...}
            ArrayList<String> objects = splitJsonObjects(dataContent);
            for (String obj : objects) {
                int id = parseIntSafe(extractValue(obj, "user_id"));
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

    public Player parsePlayer(String json) {
        try {
            int id = parseIntSafe(extractValue(json, "id"));
            if (id == 0) id = parseIntSafe(extractValue(json, "playerId")); // Fallback

            String name = extractValue(json, "user");
            if (name.isEmpty()) name = extractValue(json, "name");

            if (id != 0 && !name.isEmpty()) {
                Player p = new Player(id, name);
                p.setConnected(true);
                return p;
            }
        } catch (Exception e) {
            System.err.println("[JSON] Erreur parsing joueur: " + e.getMessage());
        }
        return null;
    }

    // --- 2. PARSING D'UNE CARTE SEULE (Avec Owner ID) ---
    public Card parseCard(String json) {
        try {
            int id = parseIntSafe(extractValue(json, "id"));
            if (id == 0) id = parseIntSafe(extractValue(json, "cardId"));

            String name = extractValue(json, "name");
            if (name.isEmpty()) name = extractValue(json, "cardName");

            int hp = parseIntSafe(extractValue(json, "HP"));
            int ap = parseIntSafe(extractValue(json, "AP")); // ou attack
            int dp = parseIntSafe(extractValue(json, "DP")); // ou defense

            // IMPORTANT : On récupère l'ID du propriétaire
            int ownerId = parseIntSafe(extractValue(json, "ownerId"));
            if (ownerId == 0) ownerId = parseIntSafe(extractValue(json, "id_player"));

            if (id != 0) {
                // On utilise le constructeur avec ownerId
                return new Card(id, name, ap, dp, hp, ownerId);
            }
        } catch (Exception e) {
            System.err.println("[JSON] Erreur parsing carte: " + e.getMessage());
        }
        return null;
    }

    public ArrayList<Card> parseConnectedCards(String json) {
        ArrayList<Card> list = new ArrayList<>();
        String dataContent = extractArrayContent(json, "data");

        if (!dataContent.isEmpty()) {
            ArrayList<String> objects = splitJsonObjects(dataContent);
            for (String obj : objects) {
                int id = parseIntSafe(extractValue(obj, "card_id"));
                String name = extractValue(obj, "cardName");
                int ap = parseIntSafe(extractValue(obj, "AP"));
                int dp = parseIntSafe(extractValue(obj, "DP"));
                int hp = parseIntSafe(extractValue(obj, "HP"));
                int ownerId = parseIntSafe(extractValue(obj, "user_id")); // Important : le user_id du JSON devient l'ownerId

                if (id != 0) {
                    // On utilise le constructeur avec ownerId et maxHealth (hp par défaut)
                    list.add(new Card(id, name, ap, dp, hp, ownerId));
                }
            }
        }
        return list;
    }

    // --- PRIVATE HELPERS ---

    private String extractValue(String json, String key) {
        // Regex simple pour chercher "key":"value" ou "key":value
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private Card parseSingleCard(String json) {
        try {
            int cardId = parseIntSafe(extractValue(json, "card_id"));
            String name = extractValue(json, "cardName");
            int hp = parseIntSafe(extractValue(json, "HP"));
            int ap = parseIntSafe(extractValue(json, "AP"));
            int dp = parseIntSafe(extractValue(json, "DP"));
            int ownerId = parseIntSafe(extractValue(json, "id_player"));

            //Retourner la carte avec les bonnes infos
                return new Card(cardId, name, ap, dp, hp, ownerId);

        } catch (Exception e) {
            System.err.println("[JSON_Reader] Error parsing card: " + e.getMessage());
            return null;
        }
    }

    // Utilitaire pour découper "objet1},{objet2" proprement
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

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}