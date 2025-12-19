#ifndef CONFIG_H
#define CONFIG_H

// Configuration réseau
#define SERVER_PORT 8080
#define MAX_BACKLOG 10
#define BUFFER_SIZE 4096

// Configuration blockchain
#define BLOCKCHAIN_DIFFICULTY 4
#define HASH_SIZE 65 // 64 hex chars + null terminator

// Configuration base de données
#define DB_NAME "le403572"
#define DB_USER "le403572"
#define DB_PASSWORD "le403572"
#define DB_HOST "linserv-info-01.campus.unice.fr"

// Configuration jeu
#define MAX_PLAYERS 10
#define MAX_CARDS_PER_PLAYER 8
#define MAX_USERNAME_LEN 50

// Messages d'erreur
#define ERR_SERVER_FULL "Serveur plein"
#define ERR_INVALID_CMD "Commande invalide"
#define ERR_NOT_AUTHENTICATED "Non authentifié"

#endif