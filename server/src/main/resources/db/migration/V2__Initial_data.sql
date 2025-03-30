-- Insert some basic items
INSERT INTO items (id, name, description, equipable, stackable, value, high_alch_value, low_alch_value, weight)
VALUES 
    (995, 'Coins', 'Lovely money!', FALSE, TRUE, 1, NULL, NULL, 0.0),
    (1511, 'Logs', 'Some wooden logs.', FALSE, FALSE, 10, 6, 4, 2.0),
    (1265, 'Bronze pickaxe', 'Used for mining.', TRUE, FALSE, 25, 15, 10, 2.5),
    (1351, 'Bronze axe', 'Used for woodcutting.', TRUE, FALSE, 25, 15, 10, 2.5),
    (1205, 'Bronze dagger', 'A small bronze dagger.', TRUE, FALSE, 30, 18, 12, 0.5)
ON CONFLICT (id) DO NOTHING;

-- Insert some regions
INSERT INTO world_regions (id, x, y, z, name, is_wilderness, is_pvp)
VALUES
    (1, 3200, 3200, 0, 'Lumbridge', FALSE, FALSE),
    (2, 3100, 3400, 0, 'Varrock', FALSE, FALSE),
    (3, 3000, 3500, 0, 'Wilderness Level 1', TRUE, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Insert some world objects
INSERT INTO world_objects (object_id, x, y, z, rotation, region_id, respawn_time)
VALUES
    (1278, 3221, 3221, 0, 0, 1, 100),  -- Tree in Lumbridge
    (1277, 3222, 3217, 0, 0, 1, 100),  -- Tree in Lumbridge
    (1265, 3230, 3245, 0, 0, 1, 150),  -- Rock in Lumbridge
    (1265, 3232, 3246, 0, 0, 1, 150),  -- Rock in Lumbridge
    (5959, 3210, 3220, 0, 0, 1, 0)     -- Lumbridge Castle door
ON CONFLICT (id) DO NOTHING; 