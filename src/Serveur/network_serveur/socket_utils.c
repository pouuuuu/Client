#include "server.h"
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>

int create_socket(int port)
{
    int server_fd;
    struct sockaddr_in address;
    int opt = 1;

    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0)
    {
        return -1;
    }

    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &opt, sizeof(opt)))
    {
        close(server_fd);
        return -1;
    }

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(port);

    if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0)
    {
        close(server_fd);
        return -1;
    }

    if (listen(server_fd, MAX_BACKLOG) < 0)
    {
        close(server_fd);
        return -1;
    }

    return server_fd;
}

int accept_client(int server_fd)
{
    struct sockaddr_in client_addr;
    socklen_t addr_len = sizeof(client_addr);
    int client_fd;

    client_fd = accept(server_fd, (struct sockaddr *)&client_addr, &addr_len);
    if (client_fd < 0)
    {
        return -1;
    }

    return client_fd;
}

// Envoi d'une réponse JSON
void send_response(int socket_fd, const char *cmd, const char *message)
{
    char buffer[BUFFER_SIZE];
    snprintf(buffer, sizeof(buffer),
             "{\"cmd\":\"%s\",\"data\":{\"message\":\"%s\"}}\n",
             cmd, message);
    send(socket_fd, buffer, strlen(buffer), 0);
}

// Envoi d'une erreur
void send_error(int socket_fd, const char *message)
{
    send_response(socket_fd, "ERROR", message);
}