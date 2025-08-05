-- Schema per The Black Mountain Database
-- Disabilita autocommit per eseguire tutto in una transazione
SET AUTOCOMMIT FALSE;

-- Drop tables se esistono (in ordine inverso per rispettare le foreign key)
DROP TABLE IF EXISTS room_objects CASCADE;
DROP TABLE IF EXISTS weapons CASCADE;
DROP TABLE IF EXISTS objects CASCADE;
DROP TABLE IF EXISTS characters CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;

-- Tabella delle stanze
CREATE TABLE rooms (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    look_description TEXT,
    north_room_id INTEGER,
    south_room_id INTEGER,
    east_room_id INTEGER,
    west_room_id INTEGER,
    is_visible BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys per le connessioni tra stanze
    FOREIGN KEY (north_room_id) REFERENCES rooms(id),
    FOREIGN KEY (south_room_id) REFERENCES rooms(id),
    FOREIGN KEY (east_room_id) REFERENCES rooms(id),
    FOREIGN KEY (west_room_id) REFERENCES rooms(id)
);

-- Tabella dei personaggi
CREATE TABLE characters (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    character_type VARCHAR(50) NOT NULL,
    max_hp INTEGER NOT NULL DEFAULT 100,
    current_hp INTEGER NOT NULL DEFAULT 100,
    attack INTEGER NOT NULL DEFAULT 10,
    defense INTEGER NOT NULL DEFAULT 5,
    is_alive BOOLEAN DEFAULT TRUE,
    room_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- Tabella degli oggetti base
CREATE TABLE objects (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    aliases TEXT, -- Separati da virgola
    is_openable BOOLEAN DEFAULT FALSE,
    is_pickupable BOOLEAN DEFAULT TRUE,
    is_pushable BOOLEAN DEFAULT FALSE,
    is_open BOOLEAN DEFAULT FALSE,
    is_pushed BOOLEAN DEFAULT FALSE,
    object_type VARCHAR(50) DEFAULT 'NORMAL', -- NORMAL, CONTAINER, WEAPON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella delle armi (estende objects)
CREATE TABLE weapons (
    id INTEGER PRIMARY KEY,
    object_id INTEGER NOT NULL,
    weapon_type VARCHAR(50) NOT NULL,
    attack_bonus INTEGER NOT NULL DEFAULT 0,
    critical_chance INTEGER DEFAULT 5,
    critical_multiplier INTEGER DEFAULT 2,
    is_poisoned BOOLEAN DEFAULT FALSE,
    poison_damage INTEGER DEFAULT 0,
    special_effect TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (object_id) REFERENCES objects(id) ON DELETE CASCADE
);

-- Tabella di relazione tra stanze e oggetti
CREATE TABLE room_objects (
    room_id INTEGER NOT NULL,
    object_id INTEGER NOT NULL,
    quantity INTEGER DEFAULT 1,
    is_container_content BOOLEAN DEFAULT FALSE,
    container_id INTEGER, -- Se questo oggetto è dentro un container
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (room_id, object_id),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (object_id) REFERENCES objects(id) ON DELETE CASCADE,
    FOREIGN KEY (container_id) REFERENCES objects(id) ON DELETE CASCADE
);

-- Indici per migliorare le performance
CREATE INDEX idx_rooms_name ON rooms(name);
CREATE INDEX idx_characters_room ON characters(room_id);
CREATE INDEX idx_characters_type ON characters(character_type);
CREATE INDEX idx_objects_name ON objects(name);
CREATE INDEX idx_objects_type ON objects(object_type);
CREATE INDEX idx_weapons_type ON weapons(weapon_type);
CREATE INDEX idx_room_objects_room ON room_objects(room_id);

-- Commit delle modifiche
COMMIT;