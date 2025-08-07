-- Schema del database per The Black Mountain
-- Versione migliorata con vincoli e indici per performance

-- ====== CREAZIONE TABELLE ======

-- Tabella delle stanze
CREATE TABLE IF NOT EXISTS rooms (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    look_description TEXT,
    north_room_id INTEGER,
    south_room_id INTEGER,
    east_room_id INTEGER,
    west_room_id INTEGER,
    is_visible BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Vincoli di chiave esterna per le connessioni tra stanze
    CONSTRAINT fk_room_north FOREIGN KEY (north_room_id) REFERENCES rooms(id),
    CONSTRAINT fk_room_south FOREIGN KEY (south_room_id) REFERENCES rooms(id),
    CONSTRAINT fk_room_east FOREIGN KEY (east_room_id) REFERENCES rooms(id),
    CONSTRAINT fk_room_west FOREIGN KEY (west_room_id) REFERENCES rooms(id),
    
    -- Vincoli di validazione
    CONSTRAINT chk_room_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_room_description_not_empty CHECK (LENGTH(TRIM(description)) > 0)
);

-- Tabella degli oggetti
CREATE TABLE IF NOT EXISTS objects (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    aliases TEXT, -- Separati da virgole
    is_openable BOOLEAN NOT NULL DEFAULT false,
    is_pickupable BOOLEAN NOT NULL DEFAULT true,
    is_pushable BOOLEAN NOT NULL DEFAULT false,
    is_open BOOLEAN NOT NULL DEFAULT false,
    is_pushed BOOLEAN NOT NULL DEFAULT false,
    object_type VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Vincoli di validazione
    CONSTRAINT chk_object_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_object_description_not_empty CHECK (LENGTH(TRIM(description)) > 0),
    CONSTRAINT chk_object_type CHECK (object_type IN ('NORMAL', 'WEAPON', 'CONTAINER', 'KEY', 'POTION', 'TREASURE'))
);

-- Tabella dei personaggi
CREATE TABLE IF NOT EXISTS characters (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    character_type VARCHAR(50) NOT NULL,
    max_hp INTEGER NOT NULL DEFAULT 100,
    current_hp INTEGER NOT NULL DEFAULT 100,
    attack INTEGER NOT NULL DEFAULT 10,
    defense INTEGER NOT NULL DEFAULT 5,
    is_alive BOOLEAN NOT NULL DEFAULT true,
    room_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Vincoli di chiave esterna
    CONSTRAINT fk_character_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    
    -- Vincoli di validazione
    CONSTRAINT chk_character_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_character_type CHECK (character_type IN ('PLAYER', 'ENEMY', 'NPC')),
    CONSTRAINT chk_character_hp_positive CHECK (max_hp > 0 AND current_hp >= 0),
    CONSTRAINT chk_character_hp_max CHECK (current_hp <= max_hp),
    CONSTRAINT chk_character_stats_positive CHECK (attack >= 0 AND defense >= 0)
);

-- Tabella delle armi (estende oggetti)
CREATE TABLE IF NOT EXISTS weapons (
    id INTEGER PRIMARY KEY,
    object_id INTEGER NOT NULL UNIQUE,
    weapon_type VARCHAR(50) NOT NULL,
    attack_bonus INTEGER NOT NULL DEFAULT 0,
    critical_chance INTEGER NOT NULL DEFAULT 5,
    critical_multiplier INTEGER NOT NULL DEFAULT 2,
    is_poisoned BOOLEAN NOT NULL DEFAULT false,
    poison_damage INTEGER NOT NULL DEFAULT 0,
    special_effect TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Vincoli di chiave esterna
    CONSTRAINT fk_weapon_object FOREIGN KEY (object_id) REFERENCES objects(id) ON DELETE CASCADE,
    
    -- Vincoli di validazione
    CONSTRAINT chk_weapon_type CHECK (weapon_type IN ('SWORD', 'AXE', 'DAGGER', 'BOW', 'STAFF', 'MACE')),
    CONSTRAINT chk_weapon_attack_bonus CHECK (attack_bonus >= 0),
    CONSTRAINT chk_weapon_critical_chance CHECK (critical_chance >= 0 AND critical_chance <= 100),
    CONSTRAINT chk_weapon_critical_multiplier CHECK (critical_multiplier >= 1),
    CONSTRAINT chk_weapon_poison_damage CHECK (poison_damage >= 0)
);

-- Tabella di associazione oggetti-stanze
CREATE TABLE IF NOT EXISTS room_objects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_id INTEGER NOT NULL,
    object_id INTEGER NOT NULL,
    container_id INTEGER, -- Se l'oggetto è dentro un container
    is_container_content BOOLEAN NOT NULL DEFAULT false,
    position_x INTEGER DEFAULT 0,
    position_y INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Vincoli di chiave esterna
    CONSTRAINT fk_room_objects_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_objects_object FOREIGN KEY (object_id) REFERENCES objects(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_objects_container FOREIGN KEY (container_id) REFERENCES objects(id) ON DELETE CASCADE,
    
    -- Vincolo unico: un oggetto può essere solo in una stanza (o container)
    CONSTRAINT uk_object_location UNIQUE (object_id, room_id)
);

-- Tabella per l'inventario del giocatore (separata per flessibilità)
CREATE TABLE IF NOT EXISTS inventory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL,
    object_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    slot_position INTEGER DEFAULT 0,
    is_equipped BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Vincoli di chiave esterna
    CONSTRAINT fk_inventory_character FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_object FOREIGN KEY (object_id) REFERENCES objects(id) ON DELETE CASCADE,
    
    -- Vincoli di validazione
    CONSTRAINT chk_inventory_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_inventory_slot_position CHECK (slot_position >= 0),
    
    -- Vincolo unico: un oggetto per personaggio
    CONSTRAINT uk_character_object UNIQUE (character_id, object_id)
);

-- Tabella per i trigger di eventi (per future espansioni)
CREATE TABLE IF NOT EXISTS event_triggers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    trigger_type VARCHAR(50) NOT NULL,
    room_id INTEGER,
    object_id INTEGER,
    character_id INTEGER,
    condition_script TEXT,
    action_script TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    trigger_count INTEGER NOT NULL DEFAULT 0,
    max_triggers INTEGER DEFAULT -1, -- -1 = infinito
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Vincoli di chiave esterna
    CONSTRAINT fk_trigger_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_trigger_object FOREIGN KEY (object_id) REFERENCES objects(id),
    CONSTRAINT fk_trigger_character FOREIGN KEY (character_id) REFERENCES characters(id),
    
    -- Vincoli di validazione
    CONSTRAINT chk_trigger_type CHECK (trigger_type IN ('ROOM_ENTER', 'OBJECT_USE', 'CHARACTER_DEFEAT', 'ITEM_PICKUP')),
    CONSTRAINT chk_trigger_count CHECK (trigger_count >= 0),
    CONSTRAINT chk_max_triggers CHECK (max_triggers >= -1)
);

-- Tabella per i salvataggi di gioco
CREATE TABLE IF NOT EXISTS game_saves (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    save_name VARCHAR(255) NOT NULL,
    player_id INTEGER NOT NULL,
    current_room_id INTEGER NOT NULL,
    game_state_json TEXT NOT NULL,
    save_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    play_time_seconds INTEGER DEFAULT 0,
    
    -- Vincoli di chiave esterna
    CONSTRAINT fk_save_player FOREIGN KEY (player_id) REFERENCES characters(id),
    CONSTRAINT fk_save_room FOREIGN KEY (current_room_id) REFERENCES rooms(id),
    
    -- Vincoli di validazione
    CONSTRAINT chk_save_name_not_empty CHECK (LENGTH(TRIM(save_name)) > 0),
    CONSTRAINT chk_play_time_positive CHECK (play_time_seconds >= 0)
);

-- ====== CREAZIONE INDICI PER PERFORMANCE ======

-- Indici per migliorare le performance delle query più comuni
CREATE INDEX IF NOT EXISTS idx_characters_room_id ON characters(room_id);
CREATE INDEX IF NOT EXISTS idx_characters_type ON characters(character_type);
CREATE INDEX IF NOT EXISTS idx_characters_alive ON characters(is_alive);
CREATE INDEX IF NOT EXISTS idx_objects_type ON objects(object_type);
CREATE INDEX IF NOT EXISTS idx_objects_pickupable ON objects(is_pickupable);
CREATE INDEX IF NOT EXISTS idx_room_objects_room_id ON room_objects(room_id);
CREATE INDEX IF NOT EXISTS idx_room_objects_object_id ON room_objects(object_id);
CREATE INDEX IF NOT EXISTS idx_inventory_character_id ON inventory(character_id);
CREATE INDEX IF NOT EXISTS idx_weapons_object_id ON weapons(object_id);
CREATE INDEX IF NOT EXISTS idx_weapons_type ON weapons(weapon_type);

-- Indici composti per query complesse
CREATE INDEX IF NOT EXISTS idx_characters_room_alive ON characters(room_id, is_alive, character_type);
CREATE INDEX IF NOT EXISTS idx_room_objects_room_container ON room_objects(room_id, is_container_content);

-- ====== TRIGGER PER AGGIORNAMENTO AUTOMATICO TIMESTAMP ======

-- Trigger per aggiornare updated_at nelle tabelle principali
CREATE TRIGGER IF NOT EXISTS tr_rooms_updated_at
    AFTER UPDATE ON rooms
    FOR EACH ROW
    BEGIN
        UPDATE rooms SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
    END;

CREATE TRIGGER IF NOT EXISTS tr_objects_updated_at
    AFTER UPDATE ON objects
    FOR EACH ROW
    BEGIN
        UPDATE objects SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
    END;

CREATE TRIGGER IF NOT EXISTS tr_characters_updated_at
    AFTER UPDATE ON characters
    FOR EACH ROW
    BEGIN
        UPDATE characters SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
    END;

CREATE TRIGGER IF NOT EXISTS tr_weapons_updated_at
    AFTER UPDATE ON weapons
    FOR EACH ROW
    BEGIN
        UPDATE weapons SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
    END;

-- Trigger per validazione coerenza HP dei personaggi
CREATE TRIGGER IF NOT EXISTS tr_characters_hp_validation
    BEFORE UPDATE OF current_hp ON characters
    FOR EACH ROW
    WHEN NEW.current_hp < 0 OR NEW.current_hp > NEW.max_hp
    BEGIN
        SELECT RAISE(ABORT, 'HP correnti non validi: devono essere tra 0 e max_hp');
    END;

-- Trigger per impostare automaticamente is_alive basato su current_hp
CREATE TRIGGER IF NOT EXISTS tr_characters_alive_status
    AFTER UPDATE OF current_hp ON characters
    FOR EACH ROW
    WHEN NEW.current_hp != OLD.current_hp
    BEGIN
        UPDATE characters 
        SET is_alive = (NEW.current_hp > 0)
        WHERE id = NEW.id AND is_alive != (NEW.current_hp > 0);
    END;

-- ====== VISTE PER QUERY COMPLESSE ======

-- Vista per caratteristiche complete delle stanze
CREATE VIEW IF NOT EXISTS v_room_details AS
SELECT 
    r.id,
    r.name,
    r.description,
    r.look_description,
    r.is_visible,
    rn.name as north_room_name,
    rs.name as south_room_name,
    re.name as east_room_name,
    rw.name as west_room_name,
    COUNT(DISTINCT c.id) as enemy_count,
    COUNT(DISTINCT CASE WHEN c.is_alive = true THEN c.id END) as alive_enemy_count,
    COUNT(DISTINCT o.id) as object_count
FROM rooms r
LEFT JOIN rooms rn ON r.north_room_id = rn.id
LEFT JOIN rooms rs ON r.south_room_id = rs.id
LEFT JOIN rooms re ON r.east_room_id = re.id
LEFT JOIN rooms rw ON r.west_room_id = rw.id
LEFT JOIN characters c ON r.id = c.room_id AND c.character_type != 'PLAYER'
LEFT JOIN room_objects ro ON r.id = ro.room_id
LEFT JOIN objects o ON ro.object_id = o.id
GROUP BY r.id, r.name, r.description, r.look_description, r.is_visible,
         rn.name, rs.name, re.name, rw.name;

-- Vista per personaggi con informazioni della stanza
CREATE VIEW IF NOT EXISTS v_character_details AS
SELECT 
    c.id,
    c.name,
    c.description,
    c.character_type,
    c.max_hp,
    c.current_hp,
    ROUND((CAST(c.current_hp AS FLOAT) / c.max_hp) * 100, 1) as hp_percentage,
    c.attack,
    c.defense,
    c.is_alive,
    r.name as room_name,
    r.id as room_id
FROM characters c
LEFT JOIN rooms r ON c.room_id = r.id;

-- Vista per armi con dettagli oggetto
CREATE VIEW IF NOT EXISTS v_weapon_details AS
SELECT 
    w.id,
    o.name,
    o.description,
    w.weapon_type,
    w.attack_bonus,
    w.critical_chance,
    w.critical_multiplier,
    w.is_poisoned,
    w.poison_damage,
    w.special_effect,
    o.is_pickupable,
    CASE 
        WHEN w.is_poisoned THEN w.attack_bonus + w.poison_damage
        ELSE w.attack_bonus
    END as total_damage_potential
FROM weapons w
JOIN objects o ON w.object_id = o.id;

-- Vista per inventario giocatore
CREATE VIEW IF NOT EXISTS v_player_inventory AS
SELECT 
    i.id,
    c.name as player_name,
    o.name as item_name,
    o.description as item_description,
    o.object_type,
    i.quantity,
    i.slot_position,
    i.is_equipped,
    CASE 
        WHEN w.id IS NOT NULL THEN 'WEAPON'
        ELSE o.object_type
    END as enhanced_type
FROM inventory i
JOIN characters c ON i.character_id = c.id
JOIN objects o ON i.object_id = o.id
LEFT JOIN weapons w ON o.id = w.object_id
WHERE c.character_type = 'PLAYER'
ORDER BY i.slot_position;

-- ====== PROCEDURE STORED (H2 supporta funzioni Java) ======

-- Funzione per calcolare danno di un'arma
CREATE ALIAS IF NOT EXISTS CALCULATE_WEAPON_DAMAGE AS $
int calculateWeaponDamage(int baseAttack, int weaponBonus, boolean isPoisoned, int poisonDamage, int criticalChance) {
    int totalDamage = baseAttack + weaponBonus;
    
    // Simula critico (semplificato)
    if (Math.random() * 100 < criticalChance) {
        totalDamage *= 2;
    }
    
    // Aggiunge danno veleno
    if (isPoisoned) {
        totalDamage += poisonDamage;
    }
    
    return Math.max(1, totalDamage);
}
$;

-- ====== FUNZIONI DI VALIDAZIONE ======

-- Verifica integrità connessioni stanze
CREATE VIEW IF NOT EXISTS v_room_connection_issues AS
SELECT 
    r.id,
    r.name,
    'North connection invalid' as issue_type,
    r.north_room_id as problematic_id
FROM rooms r
WHERE r.north_room_id IS NOT NULL 
  AND NOT EXISTS (SELECT 1 FROM rooms r2 WHERE r2.id = r.north_room_id)

UNION ALL

SELECT 
    r.id,
    r.name,
    'South connection invalid' as issue_type,
    r.south_room_id as problematic_id
FROM rooms r
WHERE r.south_room_id IS NOT NULL 
  AND NOT EXISTS (SELECT 1 FROM rooms r2 WHERE r2.id = r.south_room_id)

UNION ALL

SELECT 
    r.id,
    r.name,
    'East connection invalid' as issue_type,
    r.east_room_id as problematic_id
FROM rooms r
WHERE r.east_room_id IS NOT NULL 
  AND NOT EXISTS (SELECT 1 FROM rooms r2 WHERE r2.id = r.east_room_id)

UNION ALL

SELECT 
    r.id,
    r.name,
    'West connection invalid' as issue_type,
    r.west_room_id as problematic_id
FROM rooms r
WHERE r.west_room_id IS NOT NULL 
  AND NOT EXISTS (SELECT 1 FROM rooms r2 WHERE r2.id = r.west_room_id);

-- Vista per statistiche generali del gioco
CREATE VIEW IF NOT EXISTS v_game_statistics AS
SELECT 
    (SELECT COUNT(*) FROM rooms) as total_rooms,
    (SELECT COUNT(*) FROM objects) as total_objects,
    (SELECT COUNT(*) FROM characters) as total_characters,
    (SELECT COUNT(*) FROM characters WHERE character_type = 'PLAYER') as player_count,
    (SELECT COUNT(*) FROM characters WHERE character_type = 'ENEMY') as enemy_count,
    (SELECT COUNT(*) FROM characters WHERE character_type = 'ENEMY' AND is_alive = true) as alive_enemies,
    (SELECT COUNT(*) FROM characters WHERE character_type = 'ENEMY' AND is_alive = false) as dead_enemies,
    (SELECT COUNT(*) FROM weapons) as total_weapons,
    (SELECT COUNT(*) FROM weapons WHERE is_poisoned = true) as poisoned_weapons,
    (SELECT COUNT(*) FROM objects WHERE is_openable = true) as openable_objects,
    (SELECT COUNT(*) FROM objects WHERE is_openable = true AND is_open = true) as opened_objects,
    (SELECT AVG(CAST(current_hp AS FLOAT)) FROM characters WHERE is_alive = true) as avg_hp_alive,
    (SELECT MAX(attack_bonus) FROM weapons) as max_weapon_damage;

-- ====== CLEANUP E MANUTENZIONE ======

-- Procedure per pulizia dati orfani
CREATE VIEW IF NOT EXISTS v_orphaned_data AS
-- Oggetti senza posizione
SELECT 'Object without location' as issue_type, o.id, o.name, NULL as additional_info
FROM objects o
WHERE o.id NOT IN (SELECT DISTINCT object_id FROM room_objects WHERE object_id IS NOT NULL)
  AND o.id NOT IN (SELECT DISTINCT object_id FROM inventory WHERE object_id IS NOT NULL)

UNION ALL

-- Personaggi senza stanza (escluso giocatore che può essere ovunque)
SELECT 'Character without room' as issue_type, c.id, c.name, c.character_type as additional_info
FROM characters c
WHERE c.room_id IS NULL AND c.character_type != 'PLAYER'

UNION ALL

-- Armi senza oggetto corrispondente
SELECT 'Weapon without object' as issue_type, w.id, CAST(w.object_id AS VARCHAR), w.weapon_type as additional_info
FROM weapons w
WHERE w.object_id NOT IN (SELECT id FROM objects);

-- ====== CONFIGURAZIONI E PARAMETRI ======

-- Tabella per configurazioni di gioco
CREATE TABLE IF NOT EXISTS game_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value TEXT,
    config_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    description TEXT,
    is_user_configurable BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Vincoli di validazione
    CONSTRAINT chk_config_key_not_empty CHECK (LENGTH(TRIM(config_key)) > 0),
    CONSTRAINT chk_config_type CHECK (config_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'FLOAT', 'JSON'))
);

-- Inserimento configurazioni di default
INSERT OR IGNORE INTO game_config (config_key, config_value, config_type, description, is_user_configurable) VALUES
('game.version', '1.0.0', 'STRING', 'Versione del gioco', false),
('game.difficulty.default', 'NORMAL', 'STRING', 'Difficoltà di default', true),
('combat.base_damage', '10', 'INTEGER', 'Danno base per il combattimento', true),
('combat.critical_chance_base', '5', 'INTEGER', 'Probabilità critico base (%)', true),
('player.starting_hp', '100', 'INTEGER', 'HP iniziali del giocatore', true),
('player.starting_room', '0', 'INTEGER', 'Stanza iniziale del giocatore', false),
('inventory.max_slots', '20', 'INTEGER', 'Numero massimo slot inventario', true),
('save.auto_save_enabled', 'true', 'BOOLEAN', 'Salvataggio automatico abilitato', true),
('save.auto_save_interval', '300', 'INTEGER', 'Intervallo salvataggio automatico (secondi)', true),
('database.schema_version', '1.0', 'STRING', 'Versione schema database', false);

-- ====== TRIGGER PER CONFIGURAZIONI ======

CREATE TRIGGER IF NOT EXISTS tr_game_config_updated_at
    AFTER UPDATE ON game_config
    FOR EACH ROW
    BEGIN
        UPDATE game_config SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
    END;

-- ====== COMMENTI E DOCUMENTAZIONE ======

-- Le tabelle principali sono:
-- - rooms: Stanze del gioco con connessioni nord/sud/est/ovest
-- - objects: Oggetti generici con proprietà base
-- - characters: Personaggi (giocatore, nemici, NPC)
-- - weapons: Estensione degli oggetti per le armi
-- - room_objects: Associazione oggetti-stanze con supporto container
-- - inventory: Inventario dei personaggi
-- - event_triggers: Sistema di eventi per future espansioni
-- - game_saves: Salvataggi di gioco
-- - game_config: Configurazioni di sistema e utente

-- Le viste principali forniscono:
-- - v_room_details: Dettagli completi delle stanze
-- - v_character_details: Personaggi con informazioni stanza
-- - v_weapon_details: Armi con calcolo danno totale
-- - v_player_inventory: Inventario giocatore dettagliato
-- - v_game_statistics: Statistiche generali del gioco
-- - v_room_connection_issues: Problemi di integrità stanze
-- - v_orphaned_data: Dati orfani che necessitano pulizia

-- Gli indici ottimizzano:
-- - Ricerche per stanza dei personaggi e oggetti
-- - Filtri per tipo di personaggio/oggetto
-- - Query sull'inventario
-- - Associazioni armi-oggetti

-- I trigger garantiscono:
-- - Aggiornamento automatico timestamp
-- - Validazione integrità HP
-- - Coerenza stato vita/HP
-- - Validazione configurazioni