-- Players table
CREATE TABLE IF NOT EXISTS players (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    x INT DEFAULT 3222,
    y INT DEFAULT 3222,
    z INT DEFAULT 0,
    health INT DEFAULT 100,
    created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    last_login BIGINT NULL
);

-- Player skills table
CREATE TABLE IF NOT EXISTS player_skills (
    id SERIAL PRIMARY KEY,
    player_id INT REFERENCES players(id) ON DELETE CASCADE,
    skill_id INT NOT NULL,
    level INT DEFAULT 1,
    experience DOUBLE PRECISION DEFAULT 0.0,
    UNIQUE (player_id, skill_id)
);

-- Player inventory table
CREATE TABLE IF NOT EXISTS player_inventory (
    id SERIAL PRIMARY KEY,
    player_id INT REFERENCES players(id) ON DELETE CASCADE,
    slot INT NOT NULL,
    item_id INT NOT NULL,
    amount INT NOT NULL,
    UNIQUE (player_id, slot)
);

-- Items definition table
CREATE TABLE IF NOT EXISTS items (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    equipable BOOLEAN DEFAULT FALSE,
    stackable BOOLEAN DEFAULT FALSE,
    value INT DEFAULT 0,
    high_alch_value INT,
    low_alch_value INT,
    weight DOUBLE PRECISION DEFAULT 0.0
);

-- World regions table
CREATE TABLE IF NOT EXISTS world_regions (
    id SERIAL PRIMARY KEY,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT DEFAULT 0,
    name VARCHAR(100),
    is_wilderness BOOLEAN DEFAULT FALSE,
    is_pvp BOOLEAN DEFAULT FALSE,
    UNIQUE (x, y, z)
);

-- World objects table
CREATE TABLE IF NOT EXISTS world_objects (
    id SERIAL PRIMARY KEY,
    object_id INT NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT DEFAULT 0,
    rotation INT DEFAULT 0,
    region_id INT REFERENCES world_regions(id),
    respawn_time INT DEFAULT 0
);

-- Create indices for better performance
CREATE INDEX player_skills_player_id_idx ON player_skills(player_id);
CREATE INDEX player_inventory_player_id_idx ON player_inventory(player_id);
CREATE INDEX world_objects_region_id_idx ON world_objects(region_id);
CREATE INDEX world_objects_location_idx ON world_objects(x, y, z);

-- Insert some basic items
INSERT INTO items (id, name, description, equipable, stackable, value, high_alch_value, low_alch_value, weight)
VALUES 
    (995, 'Coins', 'Lovely money!', FALSE, TRUE, 1, NULL, NULL, 0.0),
    (1511, 'Logs', 'Some wooden logs.', FALSE, FALSE, 10, 6, 4, 2.0),
    (1265, 'Bronze pickaxe', 'Used for mining.', TRUE, FALSE, 25, 15, 10, 2.5),
    (1351, 'Bronze axe', 'Used for woodcutting.', TRUE, FALSE, 25, 15, 10, 2.5),
    (1205, 'Bronze dagger', 'A small bronze dagger.', TRUE, FALSE, 30, 18, 12, 0.5);

-- Insert some regions
INSERT INTO world_regions (id, x, y, z, name, is_wilderness, is_pvp)
VALUES
    (1, 3200, 3200, 0, 'Lumbridge', FALSE, FALSE),
    (2, 3100, 3400, 0, 'Varrock', FALSE, FALSE),
    (3, 3000, 3500, 0, 'Wilderness Level 1', TRUE, TRUE);

-- Insert some world objects
INSERT INTO world_objects (object_id, x, y, z, rotation, region_id, respawn_time)
VALUES
    (1278, 3221, 3221, 0, 0, 1, 100),  -- Tree in Lumbridge
    (1277, 3222, 3217, 0, 0, 1, 100),  -- Tree in Lumbridge
    (1265, 3230, 3245, 0, 0, 1, 150),  -- Rock in Lumbridge
    (1265, 3232, 3246, 0, 0, 1, 150),  -- Rock in Lumbridge
    (5959, 3210, 3220, 0, 0, 1, 0);    -- Lumbridge Castle door 