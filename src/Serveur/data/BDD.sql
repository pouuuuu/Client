CREATE TABLE Card (
    id_card INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY ,
    id_player INT, 
    name VARCHAR(100) NOT NULL,
    hp INT NOT NULL,
    ap INT NOT NULL,
    dp INT NOT NULL,
    image_url VARCHAR(255),
    FOREIGN KEY (id_player) REFERENCES Player(id_player)
);


CREATE TABLE Player (
    id_player INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_name VARCHAR(50) UNIQUE NOT NULL,
    connection_state BOOLEAN DEFAULT FALSE
);