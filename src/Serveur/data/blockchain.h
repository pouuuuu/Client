#ifndef BLOCKCHAIN_H
#define BLOCKCHAIN_H

#include <time.h>
#include <stdbool.h>

typedef enum
{
    card_description,
    user_hand,
    new_card_event,
    card_trade_event,
    card_battle_event
} GameItemType;

typedef struct Block
{
    int Blockid;
    char PreviousHash[65];
    char hash[65];
    int nonce;
    time_t timestamp;
    GameItemType gameItemType;
    char *gameItem;
    struct Block *nextBlock;
    struct Block *previousBlock;
} Block;

typedef struct Blockchain
{
    Block *genesisBlock;
    Block *lastBlock;
    int length;
    int difficulty;
} Blockchain;

void calculate_sha256(const char *input, char *output);
void compute_hash(Block *block);

Block *create_genesis_block();
Block *create_block(GameItemType type, const char *gameItem, const char *previousHash);
int proof_of_work(Block *block, int difficulty);

Blockchain *create_blockchain();
void free_blockchain(Blockchain *blockchain);
void add_block(Blockchain *blockchain, GameItemType type, const char *gameItem);
bool is_chain_valid(Blockchain *blockchain);
void print_blockchain(Blockchain *blockchain);
Block *get_block_by_id(Blockchain *blockchain, int blockId);
Block *get_last_block(Blockchain *blockchain);
int get_blockchain_length(Blockchain *blockchain);

#endif