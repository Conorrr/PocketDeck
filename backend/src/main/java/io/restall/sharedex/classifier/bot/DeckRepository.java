package io.restall.sharedex.classifier.bot;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeckRepository {

    private final String url;
    private final String user;
    private final String password;

    public DeckRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void insertDeck(String deckId) {
        insertDecks(Collections.singletonList(deckId), null);
    }

    @SneakyThrows
    public void insertDecks(List<String> deckIds, String postId) {
        String insertDeckSql = """
                INSERT INTO decks (id, post_id, created)
                VALUES (?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;

        try (var conn = getConnection();
             var deckStmt = conn.prepareStatement(insertDeckSql)) {

            for (var deckId : deckIds) {
                deckStmt.setString(1, deckId);
                deckStmt.setString(2, postId);
                deckStmt.setTimestamp(3, Timestamp.from(Instant.now()));
                deckStmt.addBatch();
            }
            deckStmt.executeBatch();
        }
    }

    @SneakyThrows
    public void insertPost(RedditPost post, List<String> imageUrls) {
        String insertPostSql = """
                INSERT INTO posts (id, title, selftext, created)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;

        String insertImageSql = """
                INSERT INTO images (post_id, url)
                VALUES (?, ?)
                """;

        try (var conn = getConnection()) {
            conn.setAutoCommit(false);

            try (var postStmt = conn.prepareStatement(insertPostSql);
                 var imgStmt = conn.prepareStatement(insertImageSql)) {

                postStmt.setString(1, post.id());
                postStmt.setString(2, post.title());
                postStmt.setString(3, post.selftext());
                postStmt.setTimestamp(4, Timestamp.from(post.created()));

                postStmt.executeUpdate();

                for (var img : imageUrls) {
                    imgStmt.setString(1, post.id());
                    imgStmt.setString(2, img);
                    imgStmt.addBatch();
                }
                imgStmt.executeBatch();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @SneakyThrows
    public List<String> getLatestPostsIds() {
        String sql = """
                SELECT id
                FROM posts
                ORDER BY created DESC
                LIMIT 25
                """;

        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            var posts = new ArrayList<String>();

            while (rs.next()) {
                posts.add(rs.getString("id"));
            }
            return posts;
        }
    }

    @SneakyThrows
    public List<String> getLatestDecks() {
        String sql = """
                SELECT id
                FROM decks
                ORDER BY created DESC
                LIMIT 10
                """;

        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            var posts = new ArrayList<String>();

            while (rs.next()) {
                posts.add(rs.getString("id"));
            }
            return posts;
        }
    }

}
