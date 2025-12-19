#include "database.h"
#include "../config.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

PGconn *db_connect()
{
    PGconn *conn = PQconnectdb(
        "host=" DB_HOST " "
        "dbname=" DB_NAME " "
        "user=" DB_USER " "
        "password=" DB_PASSWORD);

    if (PQstatus(conn) != CONNECTION_OK)
    {
        fprintf(stderr, "Connection to database failed: %s\n", PQerrorMessage(conn));
        PQfinish(conn);
        return NULL;
    }

    printf("Connected to database successfully\n");
    return conn;
}

void db_disconnect(PGconn *conn)
{
    if (conn)
        PQfinish(conn);
}

PGresult *db_get_user_id(PGconn *conn, const char *login)
{
    char query[256];
    PGresult *res;
    int user_id = -1;

    snprintf(query, sizeof(query),
             "SELECT id_player FROM Player WHERE user_name = '%s'",
             login);

    return PQexec(conn, query);
}

PGresult *get_player_cards(PGconn *conn, int id_player)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT id_card, name, ap, dp, image_url, hp "
             "FROM Card WHERE id_player = %d",
             id_player);

    return PQexec(conn, query);
}

PGresult *db_get_last_card_id(PGconn *conn, int id_player, const char *cardName)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT id_card FROM Card WHERE id_player = %d AND name = '%s' "
             "ORDER BY id_card DESC LIMIT 1",
             id_player, cardName);

    return PQexec(conn, query);
}

PGresult *db_get_player_by_id(PGconn *conn, int id_player)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT id_player, user_name, connection_state "
             "FROM Player WHERE id_player = %d",
             id_player);

    return PQexec(conn, query);
}

PGresult *db_get_player_connected(PGconn *conn)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT id_player, user_name "
             "FROM Player WHERE connection_state = TRUE");

    return PQexec(conn, query);
}

PGresult *db_get_player_connected_cards(PGconn *conn)
{
    char query[512];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT Card.id_card, Card.name, Card.ap, Card.dp, Card.image_url, Card.hp, Player.id_player, Player.user_name "
             "FROM Card JOIN Player ON Card.id_player = Player.id_player "
             "WHERE Player.connection_state = TRUE");

    return PQexec(conn, query);
}

PGresult *db_player_already_connected(PGconn *conn, const char *user_name)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT connection_state FROM Player WHERE user_name = '%s'",
             user_name);

    return PQexec(conn, query);
}

PGresult *db_get_card_owner(PGconn *conn, int card_id)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT id_player FROM Card WHERE id_card = %d",
             card_id);

    return PQexec(conn, query);
}

int insert_card(PGconn *conn, int id_player, const char *name, int ap, int dp, const char *image_url, int hp)
{
    char query[512];
    PGresult *res;

    if (image_url != NULL && image_url[0] != '\0')
    {
        // Avec image_url
        snprintf(query, sizeof(query),
                 "INSERT INTO Card (id_player, name, ap, dp, image_url, hp) "
                 "VALUES (%d, '%s', %d, %d, '%s', %d)",
                 id_player, name, ap, dp, image_url, hp);
    }
    else
    {
        // Sans image_url
        snprintf(query, sizeof(query),
                 "INSERT INTO Card (id_player, name, ap, dp, hp) "
                 "VALUES (%d, '%s', %d, %d, %d)",
                 id_player, name, ap, dp, hp);
    }
    res = PQexec(conn, query);
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "Insert card failed: %s\n", PQerrorMessage(conn));
        PQclear(res);
        return 0;
    }
    PQclear(res);
    return 1;
}

int insert_user(PGconn *conn, const char *user_name)
{
    char query[512];
    PGresult *res;

    snprintf(query, sizeof(query),
             "INSERT INTO Player (user_name) VALUES ('%s')",
             user_name);

    res = PQexec(conn, query);
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "Insert user failed: %s\n", PQerrorMessage(conn));
        PQclear(res);
        return 0;
    }
    PQclear(res);
    return 1;
}

int Player_connected(PGconn *conn, const char *user_name)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "UPDATE Player SET connection_state = TRUE WHERE user_name = '%s'",
             user_name);

    res = PQexec(conn, query);
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "Update connection state failed: %s\n", PQerrorMessage(conn));
        PQclear(res);
        return 0;
    }
    PQclear(res);
    return 1;
}

int Player_disconnected(PGconn *conn, const char *user_name)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "UPDATE Player SET connection_state = FALSE WHERE user_name = '%s'",
             user_name);

    res = PQexec(conn, query);
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "Update disconnection state failed: %s\n", PQerrorMessage(conn));
        PQclear(res);
        return 0;
    }
    PQclear(res);
    return 1;
}

int trade_card(PGconn *conn, int card_id, int new_owner_id)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "UPDATE Card SET id_player = %d WHERE id_card = %d",
             new_owner_id, card_id);

    res = PQexec(conn, query);
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "Trade card failed: %s\n", PQerrorMessage(conn));
        PQclear(res);
        return 0;
    }
    PQclear(res);
    return 1;
}

int Looser_delete_card(PGconn *conn, int player_id, int card_id)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "DELETE FROM Card WHERE id_player = %d AND id_card = %d",
             player_id, card_id);

    res = PQexec(conn, query);
    if (PQresultStatus(res) != PGRES_COMMAND_OK)
    {
        fprintf(stderr, "Delete cards failed: %s\n", PQerrorMessage(conn));
        PQclear(res);
        return 0;
    }
    PQclear(res);
    return 1;
}

int verify_card_ownership(PGconn *conn, int card_id, int expected_owner)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT id_player FROM Card WHERE id_card = %d",
             card_id);

    res = PQexec(conn, query);
    if (PQresultStatus(res) != PGRES_TUPLES_OK || PQntuples(res) == 0)
    {
        PQclear(res);
        return 0;
    }

    int actual_owner = atoi(PQgetvalue(res, 0, 0));
    PQclear(res);

    return (actual_owner == expected_owner);
}

int get_cards_stats(PGconn *conn, int card_id, CardStats *stats)
{
    char query[256];
    PGresult *res;

    snprintf(query, sizeof(query),
             "SELECT id_player, hp, ap, dp FROM Card WHERE id_card = %d",
             card_id);

    res = PQexec(conn, query);
    if (PQresultStatus(res) != PGRES_TUPLES_OK || PQntuples(res) == 0)
    {
        PQclear(res);
        return 0;
    }

    stats->card_id = card_id;
    stats->owner_id = atoi(PQgetvalue(res, 0, 0));
    stats->hp = atoi(PQgetvalue(res, 0, 1));
    stats->ap = atoi(PQgetvalue(res, 0, 2));
    stats->dp = atoi(PQgetvalue(res, 0, 3));

    PQclear(res);
    return 1;
}
