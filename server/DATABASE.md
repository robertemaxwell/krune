# Database Documentation

## Overview

The project uses PostgreSQL as the database, Exposed ORM for database access, and Flyway for database migrations. The database is containerized using Docker.

### Components

1. **PostgreSQL**: Relational database for storing game data
2. **Exposed ORM**: Kotlin SQL framework used for type-safe SQL queries
3. **HikariCP**: Connection pooling for optimal database performance
4. **Flyway**: Database migration tool for schema evolution

## Database Schema

### Players (`players` table)

Stores player account information and basic character data.

| Column         | Type      | Description                           |
|----------------|-----------|---------------------------------------|
| id             | SERIAL    | Primary key                           |
| username       | VARCHAR   | Unique username (max 50 chars)        |
| password_hash  | VARCHAR   | Hashed password                       |
| x              | INT       | X coordinate position                 |
| y              | INT       | Y coordinate position                 |
| z              | INT       | Z coordinate (height level)           |
| health         | INT       | Current health points                 |
| created_at     | BIGINT    | Account creation timestamp (ms)       |
| last_login     | BIGINT    | Last login timestamp (ms)             |

### Player Skills (`player_skills` table)

Tracks player skill levels and experience points.

| Column         | Type      | Description                           |
|----------------|-----------|---------------------------------------|
| id             | SERIAL    | Primary key                           |
| player_id      | INT       | Reference to players.id               |
| skill_id       | INT       | Skill identifier                      |
| level          | INT       | Current skill level                   |
| experience     | DOUBLE    | Total experience points               |

### Player Inventory (`player_inventory` table)

Stores player inventory items.

| Column         | Type      | Description                           |
|----------------|-----------|---------------------------------------|
| id             | SERIAL    | Primary key                           |
| player_id      | INT       | Reference to players.id               |
| slot           | INT       | Inventory slot number                 |
| item_id        | INT       | Item identifier                       |
| amount         | INT       | Quantity of items                     |

### Items (`items` table)

Defines all item types in the game.

| Column          | Type      | Description                          |
|-----------------|-----------|--------------------------------------|
| id              | SERIAL    | Primary key                          |
| name            | VARCHAR   | Item name                            |
| description     | TEXT      | Item description                     |
| equipable       | BOOLEAN   | Whether item can be equipped         |
| stackable       | BOOLEAN   | Whether item can stack in inventory  |
| value           | INT       | Base shop value                      |
| high_alch_value | INT       | High alchemy value                   |
| low_alch_value  | INT       | Low alchemy value                    |
| weight          | DOUBLE    | Item weight                          |

### World Regions (`world_regions` table)

Defines geographic regions in the game world.

| Column         | Type      | Description                          |
|----------------|-----------|--------------------------------------|
| id             | SERIAL    | Primary key                          |
| x              | INT       | Base X coordinate of region          |
| y              | INT       | Base Y coordinate of region          |
| z              | INT       | Base Z coordinate of region          |
| name           | VARCHAR   | Region name                          |
| is_wilderness  | BOOLEAN   | Whether region is in wilderness      |
| is_pvp         | BOOLEAN   | Whether PVP is enabled in region     |

### World Objects (`world_objects` table)

Defines static objects in the game world.

| Column         | Type      | Description                          |
|----------------|-----------|--------------------------------------|
| id             | SERIAL    | Primary key                          |
| object_id      | INT       | Object type identifier               |
| x              | INT       | X coordinate                         |
| y              | INT       | Y coordinate                         |
| z              | INT       | Z coordinate (height level)          |
| rotation       | INT       | Rotation (0-3)                       |
| region_id      | INT       | Reference to world_regions.id        |
| respawn_time   | INT       | Respawn time in ticks (if resource)  |

## Database Migration

The project uses Flyway for database migrations. Migration scripts are stored in the `server/src/main/resources/db/migration` directory.

Migration files follow the naming convention `V{version}__{description}.sql` (e.g., `V1__Initial_schema.sql`).

## Connection Pooling

HikariCP is used for connection pooling with the following configuration:

- **Maximum Pool Size**: 10 connections
- **Auto Commit**: Disabled
- **Transaction Isolation**: REPEATABLE_READ

## Repository Pattern

The project uses the repository pattern to abstract database access. Each entity type has a dedicated repository class providing CRUD operations:

- `PlayerRepository`: Operations for player accounts and data
- `ItemRepository`: Operations for item definitions
- `WorldRepository`: Operations for world state (regions and objects)

## Database Initialization

The database is automatically initialized when the server starts. The initialization process:

1. Creates a HikariCP connection pool
2. Runs Flyway migrations
3. Connects Exposed ORM to the database
4. Creates any missing tables that aren't handled by migrations 