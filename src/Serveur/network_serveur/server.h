#ifndef SERVER_H
#define SERVER_H

#include <stdbool.h>
#include <pthread.h>
#include <sys/socket.h>
#include <libpq-fe.h>
#include "../config.h"

typedef struct ClientInfo ClientInfo;
// Structure pour un client connecté
struct ClientInfo
{
    int socket_fd;
    int player_id;
    bool is_authenticated;
    bool is_bot;
    char username[50];
    pthread_t thread_id;
    ClientInfo *next;
};

// Structure principale du serveur
typedef struct
{
    int server_fd;
    ClientInfo *clients[MAX_PLAYERS];
    int client_count;
    pthread_mutex_t mutex;
    bool running;
} GameServer;

typedef struct PendingTrade
{
    int from_player_id;
    int to_player_id;
    int card_id;
    int trader_card_id;
    time_t timestamp;
    struct PendingTrade *next;
} PendingTrade;

extern PendingTrade *pending_trades;
extern pthread_mutex_t trades_mutex;

typedef struct Fight
{
    int fight_id;
    int player1_id;
    int player2_id;
    int card1_id;
    int card2_id;
    time_t timestamp;
    struct Fight *next;
} Fight;

typedef struct CardStats
{
    int card_id;
    int owner_id;
    int hp;
    int ap;
    int dp;
} CardStats;

typedef struct FightNode
{
    int fight_id;
    int player1_id;
    int player2_id;
    int card1_id;
    int card2_id;
    time_t timestamp;
    struct FightNode *next;
} FightNode;

extern int current_clients;
extern pthread_mutex_t clients_mutex;
extern ClientInfo *connected_clients;
extern pthread_mutex_t clients_list_mutex;

// server_main.c functions
int server_init(int port);
void server_run(int server_fd);

// socket_utils.c functions
int create_socket(int port);
int accept_client(int server_fd);
void send_response(int client_fd, const char *cmd, const char *data);
void send_error(int client_fd, const char *message);

// server.c functions
void *client_handler(void *arg);
void handle_auth(ClientInfo *client, const char *username);
void handle_create_card(ClientInfo *client, const char *json_data);
void player_connected(ClientInfo *client);
void player_disconnected(ClientInfo *client);
void handle_connected_players(ClientInfo *client);
void handle_connected_player_cards(ClientInfo *client);
int extract_int_from_json(const char *json, const char *key, int *value);
int extract_string_from_json(const char *json, const char *key, char *value, size_t max_len);

void add_client_to_list(ClientInfo *client);
void remove_client_from_list(int socket_fd);
ClientInfo *find_client_by_fd(int fd);
void broadcast_player_info_to_all();
void broadcast_to_all(const char *message);
void broadcast_to_all_except(int exclude_fd, const char *message);

// server_trade.c functions
void handle_trade_request(ClientInfo *client, const char *json_data);
void handle_trade_response(ClientInfo *client, const char *json_data);
void process_trade_accept(ClientInfo *trader, ClientInfo *requester, int from_player_id, int card_id, int trader_card_id);
ClientInfo *find_client_by_player_id(int player_id);
void send_trade_accept_notification_both(ClientInfo *requester, ClientInfo *trader, int card_id, int trader_card_id);
void process_trade_decline(ClientInfo *trader, ClientInfo *requester);
int execute_trade_exchange(ClientInfo *trader, ClientInfo *requester, int from_player_id, int card_id, int trader_card_id);
int verify_and_handle_card_ownership(PGconn *conn, int card_id, int expected_owner, ClientInfo *owner_client, ClientInfo *other_client, const char *error_msg);
int verify_card_ownership(PGconn *conn, int card_id, int expected_owner, const char *error_msg);
void send_trade_complete_messages(ClientInfo *trader, ClientInfo *requester, int card_id, int trader_card_id);
void handle_trade_card(ClientInfo *client, const char *json_data);

ClientInfo *find_client_by_player_id(int player_id);

// serve_fight.c functions
void handle_fight_request(ClientInfo *client, const char *json_data);
void handle_fight_response(ClientInfo *client, const char *json_data);
void handle_fight_card(ClientInfo *client, const char *json_data);
void start_fight(ClientInfo *challenger, ClientInfo *opponent, int card1_id, int card2_id);
int calculate_damage(int attacker_ap, int defender_dp, int defender_hp);
int get_card_stats(PGconn *conn, int card_id, CardStats *stats);
int add_pending_fight(int player1_id, int player2_id, int card1_id, int card2_id);
FightNode *find_and_remove_pending_fight(int player2_id);
int validate_card_ownership(PGconn *conn, int card_id, int expected_owner, CardStats *stats, const char *error_prefix);
void send_fight_request(ClientInfo *opponent, ClientInfo *challenger, int challenger_card_id, CardStats *challenger_card_stats, int opponent_card_id, CardStats *opponent_card_stats);
void send_fight_request_confirmation(ClientInfo *challenger, ClientInfo *opponent, int opponent_card_id);
void send_fight_result_simple(ClientInfo *winner, ClientInfo *loser, int winner_card_id, int winner_card_hp, int loser_card_id);
void send_fight_result_simple(ClientInfo *winner, ClientInfo *loser, int winner_card_id, int winner_card_hp, int loser_card_id);
void send_fight_draw_simple(ClientInfo *player1, ClientInfo *player2, int card1_id, int card2_id);
void send_fight_deny_messages(ClientInfo *decliner, ClientInfo *challenger);
void simulate_fight_round(CardStats *attacker, CardStats *defender, int *defender_new_hp, int *damage_dealt);
void update_database_after_fight(PGconn *conn, int winner_card_id, int winner_hp, int loser_player_id, int loser_card_id);
void process_fight_outcome(PGconn *conn, ClientInfo *player1, ClientInfo *player2, CardStats *card1, CardStats *card2);
void simulate_fight(CardStats *card1, CardStats *card2);
void start_fight(ClientInfo *challenger, ClientInfo *opponent, int card1_id, int card2_id);
void cleanup_abandoned_fights();

#endif