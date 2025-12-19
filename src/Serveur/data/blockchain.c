#include "blockchain.h"
#include "database.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <openssl/sha.h>

extern PGconn *db_conn;

void calculate_sha256(const char *input, char *output)
{
    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256((unsigned char *)input, strlen(input), hash);

    for (int i = 0; i < SHA256_DIGEST_LENGTH; i++)
    {
        sprintf(output + (i * 2), "%02x", hash[i]);
    }
    output[64] = 0;
}

void compute_hash(Block *block)
{
    char input[1024];
    snprintf(input, sizeof(input), "%d%s%d%ld%d%s",
             block->Blockid,
             block->PreviousHash,
             block->nonce,
             block->timestamp,
             block->gameItemType,
             block->gameItem);
    calculate_sha256(input, block->hash);
}

Block *create_genesis_block(int difficulty)
{
    Block *genesis = (Block *)malloc(sizeof(Block));
    if (!genesis)
        return NULL;

    genesis->Blockid = 0;
    genesis->timestamp = time(NULL);
    strcpy(genesis->PreviousHash, "0");
    genesis->nonce = 0;
    genesis->gameItemType = user_hand;
    genesis->gameItem = strdup("{\"info\": \"Genesis block\"}");
    genesis->nextBlock = NULL;
    genesis->previousBlock = NULL;

    proof_of_work(genesis, difficulty);

    return genesis;
}

int proof_of_work(Block *block, int difficulty)
{
    char target[65];
    memset(target, '0', difficulty);
    target[difficulty] = '\0';

    do
    {
        block->nonce++;
        compute_hash(block);
    } while (strncmp(block->hash, target, difficulty) != 0);

    return block->nonce;
}

Blockchain *create_blockchain()
{
    Blockchain *blockchain = (Blockchain *)malloc(sizeof(Blockchain));
    if (!blockchain)
    {
        return NULL;
    }

    blockchain->genesisBlock = NULL;
    blockchain->lastBlock = NULL;
    blockchain->length = 0;
    blockchain->difficulty = 4;

    blockchain->genesisBlock = create_genesis_block(blockchain->difficulty);
    if (!blockchain->genesisBlock)
    {
        free(blockchain);
        return NULL;
    }

    blockchain->lastBlock = blockchain->genesisBlock;
    blockchain->length = 1;
    return blockchain;
}

void free_blockchain(Blockchain *blockchain)
{
    Block *current = blockchain->genesisBlock;
    while (current)
    {
        Block *next = current->nextBlock;
        free(current->gameItem);
        free(current);
        current = next;
    }
    free(blockchain);
}

void add_block(Blockchain *blockchain, GameItemType type, const char *gameItem)
{
    if (!blockchain || !gameItem)
    {
        return false;
    }

    if (!blockchain->genesisBlock)
    {
        blockchain->genesisBlock = create_genesis_block(blockchain->difficulty);
        blockchain->lastBlock = blockchain->genesisBlock;
        blockchain->length = 1;
    }

    Block *newBlock = create_block(type, gameItem, blockchain->lastBlock->hash);
    if (!newBlock)
    {
        return false;
    }

    newBlock->Blockid = blockchain->length;
    proof_of_work(newBlock, blockchain->difficulty);

    if (blockchain->lastBlock)
    {
        blockchain->lastBlock->nextBlock = newBlock;
        newBlock->previousBlock = blockchain->lastBlock;
        blockchain->lastBlock = newBlock;
    }
    else
    {
        blockchain->genesisBlock = newBlock;
        blockchain->lastBlock = newBlock;
    }

    blockchain->length++;

    // Save to database
    if (db_conn)
    {
        db_save_block(db_conn, newBlock);
    }
}

bool is_chain_valid(Blockchain *blockchain)
{
    if (!blockchain || !blockchain->genesisBlock)
    {
        return false;
    }

    Block *current = blockchain->genesisBlock;
    Block *previous = NULL;

    while (current)
    {
        // Verify current block's hash
        char calculated_hash[65];
        char input[1024];
        snprintf(input, sizeof(input), "%d%s%d%ld%d%s",
                 current->Blockid,
                 current->PreviousHash,
                 current->nonce,
                 current->timestamp,
                 current->gameItemType,
                 current->gameItem);
        calculate_sha256(input, calculated_hash);

        if (strcmp(current->hash, calculated_hash) != 0)
        {
            printf("Invalid hash for block %d\n", current->Blockid);
            return false;
        }

        // Verify previous hash link
        if (previous && strcmp(current->PreviousHash, previous->hash) != 0)
        {
            printf("Invalid previous hash for block %d\n", current->Blockid);
            return false;
        }

        // Verify proof of work
        char target[65];
        memset(target, '0', blockchain->difficulty);
        target[blockchain->difficulty] = '\0';

        if (strncmp(current->hash, target, blockchain->difficulty) != 0)
        {
            printf("Proof of work invalid for block %d\n", current->Blockid);
            return false;
        }

        previous = current;
        current = current->nextBlock;
    }

    return true;
}

Block *create_block(GameItemType type, const char *gameItem, const char *previousHash)
{
    Block *newBlock = (Block *)malloc(sizeof(Block));
    if (!newBlock)
        return NULL;

    newBlock->Blockid = 0;
    newBlock->timestamp = time(NULL);
    strncpy(newBlock->PreviousHash, previousHash, 64);
    newBlock->PreviousHash[64] = '\0';
    newBlock->nonce = 0;
    newBlock->gameItemType = type;
    newBlock->gameItem = strdup(gameItem);
    newBlock->nextBlock = NULL;
    newBlock->previousBlock = NULL;
    memset(newBlock->hash, 0, 65);

    return newBlock;
}

void print_blockchain(Blockchain *blockchain)
{
    Block *current = blockchain->genesisBlock;
    printf("Blockchain (length: %d):\n", blockchain->length);
    printf("=======================\n");

    while (current)
    {
        printf("Block ID: %d\n", current->Blockid);
        printf("Previous Hash: %s\n", current->PreviousHash);
        printf("Hash: %s\n", current->hash);
        printf("Nonce: %d\n", current->nonce);
        printf("Timestamp: %ld\n", current->timestamp);
        printf("Type: %d\n", current->gameItemType);
        printf("Data: %s\n", current->gameItem);
        printf("---------------------------\n");
        current = current->nextBlock;
    }
}

Block *get_block_by_id(Blockchain *blockchain, int blockId)
{
    Block *current = blockchain->genesisBlock;
    while (current)
    {
        if (current->Blockid == blockId)
            return current;
        current = current->nextBlock;
    }
    return NULL;
}

Bsslock *get_last_block(Blockchain *blockchain)
{
    return blockchain->lastBlock;
}

int get_blockchain_length(Blockchain *blockchain)
{
    return blockchain->length;
}