#include "server.h"
#include "../data/database.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>
#include <arpa/inet.h>

// Compteur global déclaré dans server_main.c
extern int current_clients;
extern pthread_mutex_t clients_mutex;

ClientInfo *connected_clients = NULL;
pthread_mutex_t clients_list_mutex = PTHREAD_MUTEX_INITIALIZER;

void *client_handler(void *arg)
{
    ClientInfo *client = (ClientInfo *)arg;
    int client_fd = client->socket_fd;
    char buffer[BUFFER_SIZE];
    ssize_t bytes_read;

    // Boucle principale de lecture
    while (1)
    {
        bytes_read = recv(client_fd, buffer, BUFFER_SIZE - 1, 0);

        if (bytes_read <= 0)
        {
            if (bytes_read == 0)
            {
                printf("[CLIENT %d] Déconnecté\n", client_fd);
            }
            break;
        }

        buffer[bytes_read] = '\0';

        // Traitement du message
        if (strstr(buffer, "\"cmd\":\"AUTH\""))
        {
            // Extraire le nom d'utilisateur
            char username[100] = "";
            char *user_start = strstr(buffer, "\"user\":\"");
            if (user_start)
            {
                user_start += 8; // Taille de "\"user\":\""
                char *user_end = strchr(user_start, '"');
                if (user_end)
                {
                    size_t len = user_end - user_start;
                    strncpy(username, user_start, len);
                    username[len] = '\0';
                    handle_auth(client, username);
                }
                else
                {
                    send_error(client_fd, "Format JSON invalide pour user");
                }
            }
            else
            {
                send_error(client_fd, "Champ user manquant dans JSON");
            }
        }
        else if (strstr(buffer, "\"cmd\":\"CREATE_CARD\""))
        {
            handle_create_card(client, buffer);
        }
        else if (strstr(buffer, "\"cmd\":\"GET_CONNECTED_PLAYERS\""))
        {
            if (!client->is_authenticated)
            {
                send_error(client_fd, "Not authenticated");
            }
            else
            {
                handle_connected_players(client);
            }
        }
        else if (strstr(buffer, "\"cmd\":\"GET_CONNECTED_PLAYERS_CARDS\""))
        {
            if (!client->is_authenticated)
            {
                send_error(client_fd, "Not authenticated");
            }
            else
            {
                handle_connected_player_cards(client);
            }
        }
        else if (strstr(buffer, "\"cmd\":\"TRADE_REQUEST\""))
        {
            if (!client->is_authenticated)
            {
                send_error(client_fd, "Not authenticated");
            }
            else
            {
                handle_trade_request(client, buffer);
            }
        }
        else if (strstr(buffer, "\"cmd\":\"TRADE_ACCEPT\""))
        {
            if (!client->is_authenticated)
            {
                send_error(client_fd, "Not authenticated");
            }
            else
            {
                handle_trade_response(client, buffer);
            }
        }
        else if (strstr(buffer, "\"cmd\":\"FIGHT_REQUEST\""))
        {
            if (!client->is_authenticated)
            {
                send_error(client_fd, "Not authenticated");
            }
            else
            {
                handle_fight_request(client, buffer);
            }
        }
        else if (strstr(buffer, "\"cmd\":\"FIGHT_ACCEPT\"") ||
                 strstr(buffer, "\"cmd\":\"FIGHT_DENY\""))
        {
            if (!client->is_authenticated)
            {
                send_error(client->socket_fd, "Not authenticated");
            }
            else
            {
                printf("[DEBUG] Réponse combat reçue: %s\n", buffer);
                handle_fight_response(client, buffer);
            }
        }
        else
        {
            send_error(client_fd, "Unknown command");
        }
    }
    // Nettoyage — appeler player_disconnected si l'utilisateur était identifié
    if (client->is_authenticated || client->username[0] != '\0')
    {
        player_disconnected(client);
        remove_client_from_list(client_fd);
        broadcast_player_info_to_all();
    }
    // Décrémenter le compteur global de clients
    pthread_mutex_lock(&clients_mutex);
    current_clients--;
    pthread_mutex_unlock(&clients_mutex);

    close(client_fd);
    free(client);
    return NULL;
}

char *get_connected_players_json(PGconn *conn)
{
    PGresult *res = db_get_player_connected(conn);
    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        PQclear(res);
        return NULL;
    }

    char *json = malloc(BUFFER_SIZE);
    if (!json)
    {
        PQclear(res);
        return NULL;
    }

    strcpy(json, "{\"cmd\":\"CONNECTED_PLAYERS\",\"data\":[");

    int num_players = PQntuples(res);
    for (int i = 0; i < num_players; i++)
    {
        int player_id = atoi(PQgetvalue(res, i, 0));
        char *user_name = PQgetvalue(res, i, 1);

        char player_entry[200];
        snprintf(player_entry, sizeof(player_entry),
                 "{\"user_id\":%d,\"user_name\":\"%s\"}%s",
                 player_id, user_name, (i < num_players - 1) ? "," : "");

        strcat(json, player_entry);
    }
    strcat(json, "]}\n");

    PQclear(res);
    return json;
}

char *get_connected_players_cards_json(PGconn *conn)
{
    PGresult *res = db_get_player_connected_cards(conn);
    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        PQclear(res);
        return NULL;
    }

    char *json = malloc(BUFFER_SIZE * 2);
    if (!json)
    {
        PQclear(res);
        return NULL;
    }

    strcpy(json, "{\"cmd\":\"CONNECTED_PLAYERS_CARDS\",\"data\":[");

    int num_entries = PQntuples(res);
    for (int i = 0; i < num_entries; i++)
    {
        int card_id = atoi(PQgetvalue(res, i, 0));
        char *card_name = PQgetvalue(res, i, 1);
        int ap = atoi(PQgetvalue(res, i, 2));
        int dp = atoi(PQgetvalue(res, i, 3));
        char *image_url = PQgetvalue(res, i, 4);
        int hp = atoi(PQgetvalue(res, i, 5));
        int player_id = atoi(PQgetvalue(res, i, 6));
        char *user_name = PQgetvalue(res, i, 7);

        char card_entry[400];
        snprintf(card_entry, sizeof(card_entry),
                 "{\"card_id\":%d,\"cardName\":\"%s\",\"AP\":%d,\"DP\":%d,\"image_url\":\"%s\",\"HP\":%d,\"user_id\":%d,\"user_name\":\"%s\"}%s",
                 card_id, card_name, ap, dp, image_url ? image_url : "", hp, player_id, user_name,
                 (i < num_entries - 1) ? "," : "");

        strcat(json, card_entry);
    }
    strcat(json, "]}\n");

    PQclear(res);
    return json;
}

void handle_create_card(ClientInfo *client, const char *json_data)
{
    if (!client->is_authenticated)
    {
        send_error(client->socket_fd, "Not authenticated");
        return;
    }

    int id;
    char cardName[100] = "";
    int HP = 0, AP = 0, DP = 0;

    // Extraction des données du JSON
    if (!extract_int_from_json(json_data, "id", &id))
    {
        send_error(client->socket_fd, "ID not found in JSON");
        return;
    }

    // Chercher le data.cardName
    char *data_start = strstr(json_data, "\"data\":{");
    if (data_start)
    {
        extract_string_from_json(data_start, "cardName", cardName, sizeof(cardName));
        extract_int_from_json(data_start, "HP", &HP);
        extract_int_from_json(data_start, "AP", &AP);
        extract_int_from_json(data_start, "DP", &DP);
    }

    // Connexion à la base de données
    PGconn *conn = db_connect();
    if (!conn)
    {
        send_error(client->socket_fd, "Database connection failed");
        return;
    }

    // Vérifier si l'utilisateur existe
    PGresult *res = db_get_player_by_id(conn, id);
    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        send_error(client->socket_fd, "Error checking user");
        PQclear(res);
        db_disconnect(conn);
        return;
    }

    int user_id = atoi(PQgetvalue(res, 0, 0));
    PQclear(res);

    // Insérer la carte dans la base de données
    if (insert_card(conn, user_id, cardName, AP, DP, NULL, HP))
    {
        // Récupérer l'ID de la carte créée
        PGresult *card_res = db_get_last_card_id(conn, user_id, cardName);
        int card_id = -1;

        if (PQresultStatus(card_res) == PGRES_TUPLES_OK && PQntuples(card_res) > 0)
        {
            card_id = atoi(PQgetvalue(card_res, 0, 0));
        }
        PQclear(card_res);

        // Envoyer une confirmation au client avec les info de la carte
        char buffer[BUFFER_SIZE];
        snprintf(buffer, sizeof(buffer),
                 "{\"cmd\":\"CREATE_CARD_OK\",\"data\":{\"id_player\":%d,\"card_id\":%d,\"cardName\":\"%s\",\"HP\":%d,\"AP\":%d,\"DP\":%d}}\n",
                 user_id, card_id, cardName, HP, AP, DP);
        send(client->socket_fd, buffer, strlen(buffer), 0);
        broadcast_player_info_to_all();
    }
    else
    {
        send_error(client->socket_fd, "Failed to insert card into database");
    }

    db_disconnect(conn);
}

void player_connected(ClientInfo *client)
{
    PGconn *conn = db_connect();
    if (!conn)
        return;

    Player_connected(conn, client->username);
    db_disconnect(conn);
}

void player_disconnected(ClientInfo *client)
{
    PGconn *conn = db_connect();
    if (!conn)
        return;

    Player_disconnected(conn, client->username);
    db_disconnect(conn);
}

void handle_auth(ClientInfo *client, const char *username)
{
    PGconn *conn = db_connect();
    if (!conn)
    {
        send_error(client->socket_fd, "Database connection failed");
        return;
    }

    pthread_mutex_lock(&clients_list_mutex);
    ClientInfo *current = connected_clients;
    while (current)
    {
        if (current->is_authenticated && strcmp(current->username, username) == 0)
        {
            pthread_mutex_unlock(&clients_list_mutex);
            send_error(client->socket_fd, "User already connected from another session");
            db_disconnect(conn);
            return;
        }
        current = current->next;
    }
    pthread_mutex_unlock(&clients_list_mutex);

    // Vérifier aussi dans la base de données
    PGresult *connected_res = db_player_already_connected(conn, username);
    if (PQresultStatus(connected_res) != PGRES_TUPLES_OK)
    {
        send_error(client->socket_fd, "Error checking connection state");
        PQclear(connected_res);
        db_disconnect(conn);
        return;
    }

    // Vérifier si la ligne existe
    if (PQntuples(connected_res) > 0)
    {
        char *connection_state_str = PQgetvalue(connected_res, 0, 0);
        if (connection_state_str && strcmp(connection_state_str, "t") == 0)
        {
            // Vérifier doublement avec la liste des clients connectés
            pthread_mutex_lock(&clients_list_mutex);
            current = connected_clients;
            int already_connected_in_list = 0;
            while (current)
            {
                if (current->is_authenticated && strcmp(current->username, username) == 0)
                {
                    already_connected_in_list = 1;
                    break;
                }
                current = current->next;
            }
            pthread_mutex_unlock(&clients_list_mutex);

            if (already_connected_in_list)
            {
                PQclear(connected_res);
                send_error(client->socket_fd, "User already connected from another session");
                db_disconnect(conn);
                return;
            }
            else
            {
                // Cas d'un crash précédent : l'état dans la BDD n'a pas été mis à jour
                // On force la déconnexion dans la BDD
                Player_disconnected(conn, username);
            }
        }
    }
    PQclear(connected_res);

    // Vérifier si l'utilisateur existe déjà
    PGresult *res = db_get_user_id(conn, username);

    int user_id;

    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        send_error(client->socket_fd, "Error checking user");
        PQclear(res);
        db_disconnect(conn);
        return;
    }

    if (PQntuples(res) == 0)
    {
        // L'utilisateur n'existe pas, on le crée
        PQclear(res);

        if (!insert_user(conn, username))
        {
            send_error(client->socket_fd, "Failed to create user");
            db_disconnect(conn);
            return;
        }

        // Récupérer l'ID après création
        res = db_get_user_id(conn, username);
        if (PQntuples(res) == 0)
        {
            send_error(client->socket_fd, "Failed to get user ID after creation");
            PQclear(res);
            db_disconnect(conn);
            return;
        }

        user_id = atoi(PQgetvalue(res, 0, 0));
    }
    else
    {
        // Utilisateur existant, récupérer son ID
        user_id = atoi(PQgetvalue(res, 0, 0));
    }

    // Mettre à jour l'état du client
    client->is_authenticated = true;
    client->player_id = user_id;
    strncpy(client->username, username, sizeof(client->username) - 1);
    client->username[sizeof(client->username) - 1] = '\0';

    add_client_to_list(client);

    // Envoyer la réponse avec l'ID du joueur uniquement
    char response[BUFFER_SIZE];
    snprintf(response, sizeof(response),
             "{\"cmd\":\"AUTH_OK\",\"data\":{\"user_id\":%d}}\n", user_id);

    send(client->socket_fd, response, strlen(response), 0);

    PQclear(res);
    db_disconnect(conn);

    // Mettre à vrai dans la base de données
    player_connected(client);
    broadcast_player_info_to_all();
}

void handle_connected_players(ClientInfo *client)
{
    PGconn *conn = db_connect();
    if (!conn)
    {
        send_error(client->socket_fd, "Database connection failed");
        return;
    }

    PGresult *res = db_get_player_connected(conn);
    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        send_error(client->socket_fd, "Error retrieving connected players");
        PQclear(res);
        db_disconnect(conn);
        return;
    }

    char response[BUFFER_SIZE];
    strcpy(response, "{\"cmd\":\"CONNECTED_PLAYERS\",\"data\":[");

    int num_players = PQntuples(res);
    for (int i = 0; i < num_players; i++)
    {
        int player_id = atoi(PQgetvalue(res, i, 0));
        char *user_name = PQgetvalue(res, i, 1);

        char player_entry[200];
        snprintf(player_entry, sizeof(player_entry),
                 "{\"user_id\":%d,\"user_name\":\"%s\"}%s",
                 player_id, user_name, (i < num_players - 1) ? "," : "");

        strcat(response, player_entry);
    }
    strcat(response, "]}\n");

    send(client->socket_fd, response, strlen(response), 0);

    PQclear(res);
    db_disconnect(conn);
}

void handle_connected_player_cards(ClientInfo *client)
{
    PGconn *conn = db_connect();
    if (!conn)
    {
        send_error(client->socket_fd, "Database connection failed");
        return;
    }

    PGresult *res = db_get_player_connected_cards(conn);
    if (PQresultStatus(res) != PGRES_TUPLES_OK)
    {
        send_error(client->socket_fd, "Error retrieving connected players' cards");
        PQclear(res);
        db_disconnect(conn);
        return;
    }

    char response[BUFFER_SIZE];
    strcpy(response, "{\"cmd\":\"CONNECTED_PLAYERS_CARDS\",\"data\":[");

    int num_entries = PQntuples(res);
    for (int i = 0; i < num_entries; i++)
    {
        int card_id = atoi(PQgetvalue(res, i, 0));
        char *card_name = PQgetvalue(res, i, 1);
        int ap = atoi(PQgetvalue(res, i, 2));
        int dp = atoi(PQgetvalue(res, i, 3));
        char *image_url = PQgetvalue(res, i, 4);
        int hp = atoi(PQgetvalue(res, i, 5));
        int player_id = atoi(PQgetvalue(res, i, 6));
        char *user_name = PQgetvalue(res, i, 7);

        char card_entry[400];
        snprintf(card_entry, sizeof(card_entry),
                 "{\"card_id\":%d,\"cardName\":\"%s\",\"AP\":%d,\"DP\":%d,\"image_url\":\"%s\",\"HP\":%d,\"user_id\":%d,\"user_name\":\"%s\"}%s",
                 card_id, card_name, ap, dp, image_url, hp, player_id, user_name,
                 (i < num_entries - 1) ? "," : "");

        strcat(response, card_entry);
    }
    strcat(response, "]}\n");

    send(client->socket_fd, response, strlen(response), 0);

    PQclear(res);
    db_disconnect(conn);
}

void add_client_to_list(ClientInfo *client)
{
    pthread_mutex_lock(&clients_list_mutex);

    client->next = connected_clients;
    connected_clients = client;

    printf("[LIST] Client %d ajouté à la liste (username: %s)\n",
           client->socket_fd, client->username);

    pthread_mutex_unlock(&clients_list_mutex);
}

void remove_client_from_list(int socket_fd)
{
    pthread_mutex_lock(&clients_list_mutex);

    ClientInfo **ptr = &connected_clients;
    while (*ptr)
    {
        if ((*ptr)->socket_fd == socket_fd)
        {
            ClientInfo *to_remove = *ptr;
            *ptr = (*ptr)->next;

            printf("[LIST] Client %d retiré de la liste (username: %s)\n",
                   socket_fd, to_remove->username);

            // Note: la structure ClientInfo sera libérée dans client_handler
            break;
        }
        ptr = &(*ptr)->next;
    }

    pthread_mutex_unlock(&clients_list_mutex);
}

ClientInfo *find_client_by_fd(int fd)
{
    pthread_mutex_lock(&clients_list_mutex);

    ClientInfo *current = connected_clients;
    while (current)
    {
        if (current->socket_fd == fd)
        {
            pthread_mutex_unlock(&clients_list_mutex);
            return current;
        }
        current = current->next;
    }

    pthread_mutex_unlock(&clients_list_mutex);
    return NULL;
}

void broadcast_to_all(const char *message)
{
    pthread_mutex_lock(&clients_list_mutex);

    ClientInfo *current = connected_clients;
    int count = 0;

    while (current)
    {
        if (current->is_authenticated)
        {
            send(current->socket_fd, message, strlen(message), 0);
            count++;
        }
        current = current->next;
    }

    pthread_mutex_unlock(&clients_list_mutex);

    if (count > 0)
    {
        printf("[BROADCAST] Message envoyé à %d clients\n", count);
    }
}

void broadcast_to_all_except(int exclude_fd, const char *message)
{
    pthread_mutex_lock(&clients_list_mutex);

    ClientInfo *current = connected_clients;
    int count = 0;

    while (current)
    {
        if (current->is_authenticated && current->socket_fd != exclude_fd)
        {
            send(current->socket_fd, message, strlen(message), 0);
            count++;
        }
        current = current->next;
    }

    pthread_mutex_unlock(&clients_list_mutex);

    if (count > 0)
    {
        printf("[BROADCAST] Message envoyé à %d clients (excluant %d)\n", count, exclude_fd);
    }
}

void broadcast_player_info_to_all()
{
    PGconn *conn = db_connect();
    if (!conn)
    {
        printf("[BROADCAST] ERROR: impossible de se connecter à la base de données\n");
        return;
    }

    // Récupérer les infos au format JSON
    char *players_json = get_connected_players_json(conn);
    char *cards_json = get_connected_players_cards_json(conn);

    if (!players_json || !cards_json)
    {
        printf("[BROADCAST] ERROR: impossible de générer les JSON\n");
        if (players_json)
            free(players_json);
        if (cards_json)
            free(cards_json);
        db_disconnect(conn);
        return;
    }

    printf("[BROADCAST] Envoi des infos à tous les clients...\n");
    printf("  - Players JSON: %s\n", players_json);
    printf("  - Cards JSON: %s\n", cards_json);

    pthread_mutex_lock(&clients_list_mutex);

    // Envoyer à tous les clients authentifiés
    ClientInfo *current = connected_clients;
    int count = 0;

    while (current)
    {
        if (current->is_authenticated)
        {
            // Envoyer la liste des joueurs
            send(current->socket_fd, players_json, strlen(players_json), 0);
            usleep(1000); // Petit délai pour éviter le mélange des messages

            // Envoyer la liste des cartes
            send(current->socket_fd, cards_json, strlen(cards_json), 0);

            count++;
            printf("[BROADCAST] Infos envoyées au client %d (%s)\n",
                   current->socket_fd, current->username);
        }
        current = current->next;
    }

    pthread_mutex_unlock(&clients_list_mutex);

    printf("[BROADCAST] Terminé - %d clients mis à jour\n", count);

    // Nettoyer
    free(players_json);
    free(cards_json);
    db_disconnect(conn);
}
