#ifndef DATABASE_H
#define DATABASE_H

#include <libpq-fe.h>
#include <stdbool.h>

PGconn *db_connect();
void db_disconnect(PGconn *conn);

PGresult *db_get_user_id(PGconn *conn, const char *login);
PGresult *get_player_cards(PGconn *conn, int id_joueur);
PGresult *db_get_last_card_id(PGconn *conn, int id_joueur, const char *cardName);
PGresult *db_get_player_by_id(PGconn *conn, int id_joueur);
PGresult *db_get_player_connected(PGconn *conn);
PGresult *db_get_player_connected_cards(PGconn *conn);
PGresult *db_player_already_connected(PGconn *conn, const char *user_name);
PGresult *db_get_card_owner(PGconn *conn, int card_id);

int insert_card(PGconn *conn, int id_joueur, const char *nom, int attaque, int defense, const char *image_url, int hp);
int Player_connected(PGconn *conn, const char *user_name);
int Player_disconnected(PGconn *conn, const char *user_name);
int insert_user(PGconn *conn, const char *nom_utilisateur);
int trade_card(PGconn *conn, int card_id, int new_owner_id);
int Looser_delete_card(PGconn *conn, int player_id, int card_id);
int verify_card_ownership(PGconn *conn, int card_id, int expected_owner);

#endif