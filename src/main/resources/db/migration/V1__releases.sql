CREATE TABLE releases (
    id VARCHAR(36) PRIMARY KEY,
    version VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(800) NOT NULL,
    notes TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at VARCHAR(40),
    created_at VARCHAR(40) NOT NULL,
    source_url VARCHAR(500)
);
CREATE TABLE assets (
    id VARCHAR(36) PRIMARY KEY,
    release_id VARCHAR(36) NOT NULL REFERENCES releases(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64),
    sha512 VARCHAR(100),
    external_url VARCHAR(700),
    storage_key VARCHAR(200),
    UNIQUE(release_id, name)
);
CREATE INDEX releases_public ON releases(published);
CREATE INDEX assets_release ON assets(release_id);
