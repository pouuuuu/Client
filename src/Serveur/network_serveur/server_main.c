#include "server.h"
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <string.h>
#include "../config.h"
#include "../data/database.h"

// Compteur global des clients connectés et mutex pour protection
int current_clients = 0;
pthread_mutex_t clients_mutex = PTHREAD_MUTEX_INITIALIZER;

extern ClientInfo *connected_clients;
extern pthread_mutex_t clients_list_mutex;

// Initialisation du serveur
int server_init(int port)
{
    return create_socket(port);
}

void server_run(int server_fd)
{
    while (1)
    {
        // Acceptation d'un nouveau client
        int client_fd = accept_client(server_fd);
        if (client_fd < 0)
        {
            continue;
        }

        // Vérifier la limite de joueurs (atomique)
        pthread_mutex_lock(&clients_mutex);
        if (current_clients >= MAX_PLAYERS)
        {
            pthread_mutex_unlock(&clients_mutex);
            // Informer le client que le serveur est plein puis fermer la socket
            send_error(client_fd, ERR_SERVER_FULL);
            close(client_fd);
            continue;
        }
        // Réserver une place pour ce client
        current_clients++;
        pthread_mutex_unlock(&clients_mutex);

        // Création de la structure client
        ClientInfo *client = malloc(sizeof(ClientInfo));
        if (!client)
        {
            // Décrémenter si échec d'allocation
            pthread_mutex_lock(&clients_mutex);
            current_clients--;
            pthread_mutex_unlock(&clients_mutex);

            close(client_fd);
            continue;
        }

        memset(client, 0, sizeof(ClientInfo));
        client->socket_fd = client_fd;
        client->player_id = -1;
        client->is_authenticated = false;
        client->is_bot = false;
        client->username[0] = '\0';

        // Création du thread pour le client
        pthread_t thread_id;
        if (pthread_create(&thread_id, NULL, client_handler, client) != 0)
        {
            // Annuler la réservation de place
            pthread_mutex_lock(&clients_mutex);
            current_clients--;
            pthread_mutex_unlock(&clients_mutex);

            free(client);
            close(client_fd);
            continue;
        }

        client->thread_id = thread_id;
        pthread_detach(thread_id);
    }
}

// Point d'entrée principal
int main()
{
    connected_clients = NULL;
    int server_fd = server_init(SERVER_PORT);
    if (server_fd < 0)
    {
        return 1;
    }

    server_run(server_fd);

    close(server_fd);
    return 0;
}