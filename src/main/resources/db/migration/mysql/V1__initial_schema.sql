-- HyperFactions initial schema for MySQL/MariaDB
-- All tables use the ${prefix} placeholder for table prefix

-- Core faction data
CREATE TABLE IF NOT EXISTS ${prefix}_factions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    tag VARCHAR(5),
    color VARCHAR(7) NOT NULL DEFAULT '#55FFFF',
    created_at BIGINT NOT NULL,
    open BOOLEAN NOT NULL DEFAULT FALSE,
    home_world VARCHAR(255),
    home_x DOUBLE,
    home_y DOUBLE,
    home_z DOUBLE,
    home_yaw FLOAT,
    home_pitch FLOAT,
    home_set_at BIGINT,
    home_set_by VARCHAR(36),
    hardcore_power DOUBLE,
    permissions_json TEXT,
    UNIQUE KEY uq_faction_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Faction members
CREATE TABLE IF NOT EXISTS ${prefix}_faction_members (
    faction_id VARCHAR(36) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    joined_at BIGINT NOT NULL,
    last_online BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (faction_id, player_uuid),
    INDEX idx_member_player (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Territory claims (one chunk = one row)
CREATE TABLE IF NOT EXISTS ${prefix}_faction_claims (
    world VARCHAR(255) NOT NULL,
    chunk_x INT NOT NULL,
    chunk_z INT NOT NULL,
    faction_id VARCHAR(36) NOT NULL,
    claimed_at BIGINT NOT NULL,
    claimed_by VARCHAR(36),
    PRIMARY KEY (world, chunk_x, chunk_z),
    INDEX idx_claim_faction (faction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Inter-faction relations
CREATE TABLE IF NOT EXISTS ${prefix}_faction_relations (
    faction_id VARCHAR(36) NOT NULL,
    target_faction_id VARCHAR(36) NOT NULL,
    type VARCHAR(16) NOT NULL,
    since BIGINT NOT NULL,
    PRIMARY KEY (faction_id, target_faction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Activity audit log
CREATE TABLE IF NOT EXISTS ${prefix}_faction_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    faction_id VARCHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    actor_uuid VARCHAR(36),
    message_key VARCHAR(128),
    message_args TEXT,
    INDEX idx_log_faction_time (faction_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Player data (power, stats, preferences)
CREATE TABLE IF NOT EXISTS ${prefix}_players (
    uuid VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    power DOUBLE NOT NULL DEFAULT 0,
    max_power DOUBLE NOT NULL DEFAULT 0,
    last_death BIGINT NOT NULL DEFAULT 0,
    last_regen BIGINT NOT NULL DEFAULT 0,
    kills INT NOT NULL DEFAULT 0,
    deaths INT NOT NULL DEFAULT 0,
    first_joined BIGINT NOT NULL DEFAULT 0,
    last_online BIGINT NOT NULL DEFAULT 0,
    max_power_override DOUBLE,
    power_loss_disabled BOOLEAN NOT NULL DEFAULT FALSE,
    claim_decay_exempt BOOLEAN NOT NULL DEFAULT FALSE,
    admin_bypass_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    language_preference VARCHAR(10),
    territory_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    death_announcements_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    power_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_player_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Player faction membership history
CREATE TABLE IF NOT EXISTS ${prefix}_player_membership_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    faction_id VARCHAR(36) NOT NULL,
    faction_name VARCHAR(64) NOT NULL,
    faction_tag VARCHAR(5),
    highest_role VARCHAR(16) NOT NULL,
    joined_at BIGINT NOT NULL,
    left_at BIGINT NOT NULL DEFAULT 0,
    reason VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    INDEX idx_membership_player (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Zones (SafeZone / WarZone)
CREATE TABLE IF NOT EXISTS ${prefix}_zones (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    type VARCHAR(16) NOT NULL,
    world VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(36),
    flags TEXT,
    settings TEXT,
    notify_on_entry BOOLEAN NOT NULL DEFAULT TRUE,
    notify_title_upper VARCHAR(255),
    notify_title_lower VARCHAR(255),
    INDEX idx_zone_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Zone territory chunks
CREATE TABLE IF NOT EXISTS ${prefix}_zone_chunks (
    world VARCHAR(255) NOT NULL,
    chunk_x INT NOT NULL,
    chunk_z INT NOT NULL,
    zone_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (world, chunk_x, chunk_z),
    INDEX idx_zone_chunk_zone (zone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Faction treasury / economy
CREATE TABLE IF NOT EXISTS ${prefix}_faction_economy (
    faction_id VARCHAR(36) NOT NULL PRIMARY KEY,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    limits_json TEXT,
    last_upkeep_timestamp BIGINT NOT NULL DEFAULT 0,
    upkeep_auto_pay BOOLEAN NOT NULL DEFAULT TRUE,
    upkeep_grace_start_timestamp BIGINT NOT NULL DEFAULT 0,
    consecutive_missed_payments INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Economy transaction history
CREATE TABLE IF NOT EXISTS ${prefix}_faction_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    faction_id VARCHAR(36) NOT NULL,
    actor_id VARCHAR(36),
    type VARCHAR(32) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    balance_after DECIMAL(19, 4) NOT NULL,
    timestamp BIGINT NOT NULL,
    description TEXT,
    INDEX idx_tx_faction_time (faction_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Faction chat message history
CREATE TABLE IF NOT EXISTS ${prefix}_chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    faction_id VARCHAR(36) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(64) NOT NULL,
    player_faction_tag VARCHAR(5),
    channel VARCHAR(16) NOT NULL DEFAULT 'FACTION',
    message TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    INDEX idx_chat_faction_time (faction_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pending faction invites
CREATE TABLE IF NOT EXISTS ${prefix}_invites (
    faction_id VARCHAR(36) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    invited_by VARCHAR(36) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    PRIMARY KEY (faction_id, player_uuid),
    INDEX idx_invite_player (player_uuid),
    INDEX idx_invite_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pending join requests
CREATE TABLE IF NOT EXISTS ${prefix}_join_requests (
    faction_id VARCHAR(36) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(64) NOT NULL,
    message TEXT,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    PRIMARY KEY (faction_id, player_uuid),
    INDEX idx_request_player (player_uuid),
    INDEX idx_request_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
