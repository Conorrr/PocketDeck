CREATE TABLE posts (
    id          TEXT PRIMARY KEY,
    title       TEXT NOT NULL,
    selftext    TEXT,
    created     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_posts_created ON posts(created DESC);

CREATE TABLE images (
    id          SERIAL PRIMARY KEY,
    post_id     TEXT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    url         TEXT NOT NULL
);

CREATE INDEX idx_images_post_id ON images(post_id);

CREATE TABLE decks (
    id          TEXT PRIMARY KEY,
    post_id     TEXT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created     TIMESTAMPTZ NOT NULL
)
