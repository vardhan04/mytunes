package com.mycompany.cecs544gui;



import com.mpatric.mp3agic.*;
import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    static final String DB_URL = "jdbc:sqlite:mytunes.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            if (conn != null) {
                System.out.println("Connection to SQLite has been established.");
            }

            String createTableOfSongs = "CREATE TABLE IF NOT EXISTS songs ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "title TEXT NOT NULL,"
                            + "artist TEXT NOT NULL,"
                            + "album TEXT,"
                            + "year INTEGER,"
                            + "genre TEXT,"
                            + "comment TEXT,"
                            + "file_path TEXT NOT NULL,"
                            + "playlist_id INTEGER,"
                            + "FOREIGN KEY (playlist_id) REFERENCES playlists(id))";
            stmt.execute(createTableOfSongs);

            String createTableOfPlaylists = "CREATE TABLE IF NOT EXISTS playlists ("
                                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                            + "name TEXT NOT NULL)";
            stmt.execute(createTableOfPlaylists);

            String createTableOfPlaylistSongs = "CREATE TABLE IF NOT EXISTS playlist_songs ("
                                                + "playlist_id INTEGER NOT NULL,"
                                                + "song_id INTEGER NOT NULL,"
                                                + "FOREIGN KEY (playlist_id) REFERENCES playlists(id),"
                                                + "FOREIGN KEY (song_id) REFERENCES songs(id))";
            stmt.execute(createTableOfPlaylistSongs);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public static Song getSongByFilePath(String filePath) {
    Song song = null;
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM songs WHERE file_path = ?")) {
        stmt.setString(1, filePath.replace("\\", "\\\\"));
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            song = new Song(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("artist"),
                rs.getString("album"),
                rs.getInt("year"),
                rs.getString("genre"),
                rs.getString("comment"),
                rs.getString("file_path")
            );
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return song;
}


    public static Song addSong(File file, Integer playlistId) {
    Song song = null;
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement insertSongStmt = conn.prepareStatement(
                 "INSERT INTO songs (title, artist, album, year, genre, comment, file_path, playlist_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {

        
         System.out.println("File path: " + file.getPath());

        // Check if the file exists
        if (!file.exists()) {
            System.out.println("File not found: " + file.getPath());
            return null; // Exit early if the file does not exist
        }
        Mp3File mp3file = new Mp3File(file.getPath());
        String title = "";
        String artist = "";
        String album = "";
        int year = 0;
        String genre = "";
        String comment = "";

        if (mp3file.hasId3v1Tag()) {
            ID3v1 id3v1Tag = mp3file.getId3v1Tag();
            title = id3v1Tag.getTitle() != null ? id3v1Tag.getTitle() : "";
            artist = id3v1Tag.getArtist() != null ? id3v1Tag.getArtist() : "";
            album = id3v1Tag.getAlbum() != null ? id3v1Tag.getAlbum() : "";
            year = id3v1Tag.getYear() != null && !id3v1Tag.getYear().isEmpty() ? Integer.parseInt(id3v1Tag.getYear()) : 0;
            genre = id3v1Tag.getGenreDescription() != null ? id3v1Tag.getGenreDescription() : "";
            comment = id3v1Tag.getComment() != null ? id3v1Tag.getComment() : "";
        } else if (mp3file.hasId3v2Tag()) {
            ID3v2 id3v2Tag = mp3file.getId3v2Tag();
            title = id3v2Tag.getTitle() != null ? id3v2Tag.getTitle() : "";
            artist = id3v2Tag.getArtist() != null ? id3v2Tag.getArtist() : "";
            album = id3v2Tag.getAlbum() != null ? id3v2Tag.getAlbum() : "";
            year = id3v2Tag.getYear() != null && !id3v2Tag.getYear().isEmpty() ? Integer.parseInt(id3v2Tag.getYear()) : 0;
            genre = id3v2Tag.getGenreDescription() != null ? id3v2Tag.getGenreDescription() : "";
            comment = id3v2Tag.getComment() != null ? id3v2Tag.getComment() : "";
        }

        insertSongStmt.setString(1, title);
        insertSongStmt.setString(2, artist);
        insertSongStmt.setString(3, album);
        insertSongStmt.setInt(4, year);
        insertSongStmt.setString(5, genre);
        insertSongStmt.setString(6, comment);
        insertSongStmt.setString(7, file.getPath().replace("\\", "\\\\"));
        if (playlistId != null) {
            insertSongStmt.setInt(8, playlistId);
        } else {
            insertSongStmt.setNull(8, Types.INTEGER);
        }

        insertSongStmt.executeUpdate();
        ResultSet generatedKeys = insertSongStmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            int songId = generatedKeys.getInt(1);
            song = new Song(songId, title, artist, album, year, genre, comment, file.getPath());
            song.setPlaylistId(playlistId != null ? playlistId : -1);
        }
        System.out.println("Added song with name: " + title);

    } catch (Exception e) {
        System.out.println("Error processing file(not mp3): " + file.getName());
        e.printStackTrace();
    }
    return song;
}


    
    public static boolean isSongInLibrary(String title, String artist, String filePath) {
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM songs WHERE title = ? AND artist = ? AND file_path = ?")) {
        stmt.setString(1, title);
        stmt.setString(2, artist);
        stmt.setString(3, filePath.replace("\\", "\\\\"));
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;  // Returns true if the song exists in the library
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}
    
    
    public static Song addSongByTitleAndArtist(String title, String artist, String album, int year, String genre, String comment, String filePath) {
        Song song = null;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement insertSongStmt = conn.prepareStatement(
                     "INSERT INTO songs (title, artist, album, year, genre, comment, file_path) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            insertSongStmt.setString(1, title);
            insertSongStmt.setString(2, artist);
            insertSongStmt.setString(3, album);
            insertSongStmt.setInt(4, year);
            insertSongStmt.setString(5, genre);
            insertSongStmt.setString(6, comment);
            insertSongStmt.setString(7, filePath.replace("\\", "\\\\"));

            insertSongStmt.executeUpdate();
            ResultSet generatedKeys = insertSongStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int songId = generatedKeys.getInt(1);
                song = new Song(songId, title, artist, album, year, genre, comment, filePath);
            }
            System.out.println("Added song with title: " + title + " and artist: " + artist);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return song;
    }
    
    

    
    public static void deleteSong(int songId) {
    try (Connection conn = DriverManager.getConnection(DB_URL)) {
        // Start transaction
        conn.setAutoCommit(false);

        // Delete from playlist_songs table
        try (PreparedStatement deletePlaylistSongStmt = conn.prepareStatement("DELETE FROM playlist_songs WHERE song_id = ?")) {
            deletePlaylistSongStmt.setInt(1, songId);
            deletePlaylistSongStmt.executeUpdate();
            System.out.println("Deleted song from playlist_songs where song_id is: " + songId);
        }

        // Delete from songs table
        try (PreparedStatement deleteSongStmt = conn.prepareStatement("DELETE FROM songs WHERE id = ?")) {
            deleteSongStmt.setInt(1, songId);
            deleteSongStmt.executeUpdate();
            System.out.println("Deleted song where ID is: " + songId);
        }

        // Commit transaction
        conn.commit();

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
    public static void deletePlaylistAndSongs(String playlistName) {
    try (Connection conn = DriverManager.getConnection(DB_URL)) {
        conn.setAutoCommit(false);

        int playlistId = getPlaylistIdByName(playlistName);

        if (playlistId != -1) {
            //deleteSongsByPlaylistId(playlistId);
            
            PreparedStatement deletePlaylistSongsStmt = conn.prepareStatement("DELETE FROM playlist_songs WHERE playlist_id = ?");
            deletePlaylistSongsStmt.setInt(1, playlistId);
            deletePlaylistSongsStmt.executeUpdate();
            
            PreparedStatement updateSongsStmt = conn.prepareStatement("UPDATE songs SET playlist_id = NULL WHERE playlist_id = ?");
                updateSongsStmt.setInt(1, playlistId);
                updateSongsStmt.executeUpdate();

            try (PreparedStatement deletePlaylistStmt = conn.prepareStatement("DELETE FROM playlists WHERE id = ?")) {
                deletePlaylistStmt.setInt(1, playlistId);
                deletePlaylistStmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Playlist and related songs deleted: " + playlistName);
        } else {
            System.out.println("Playlist not found: " + playlistName);
        }
    } catch (SQLException e) {
        System.out.println("SQL Error: " + e.getMessage());
        e.printStackTrace();
    }
}

//    public static List<Song> getSongsByPlaylistId(int playlistId) {
//    List<Song> songs = new ArrayList<>();
//    try (Connection conn = DriverManager.getConnection(DB_URL);
//         PreparedStatement ps = conn.prepareStatement("SELECT * FROM songs WHERE playlist_id = ?")) {
//        ps.setInt(1, playlistId);
//        try (ResultSet rs = ps.executeQuery()) {
//            while (rs.next()) {
//                int id = rs.getInt("id");
//                String title = rs.getString("title");
//                String artist = rs.getString("artist");
//                String album = rs.getString("album");
//                int year = rs.getInt("year");
//                String genre = rs.getString("genre");
//                String comment = rs.getString("comment");
//                String filePath = rs.getString("file_path");
//                songs.add(new Song(id, title, artist, album, year, genre, comment, filePath));
//            }
//        }
//    } catch (SQLException e) {
//        e.printStackTrace();
//    }
//    return songs;
//}

    

    public static List<Song> getSongsByPlaylistId(int playlistId) {
    List<Song> songs = new ArrayList<>();
    String query = "SELECT s.* FROM songs s " +
                   "JOIN playlist_songs ps ON s.id = ps.song_id " +
                   "WHERE ps.playlist_id = ?";
    
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement ps = conn.prepareStatement(query)) {
        ps.setInt(1, playlistId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String artist = rs.getString("artist");
                String album = rs.getString("album");
                int year = rs.getInt("year");
                String genre = rs.getString("genre");
                String comment = rs.getString("comment");
                String filePath = rs.getString("file_path");
                songs.add(new Song(id, title, artist, album, year, genre, comment, filePath));
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return songs;
}

    
    
    
    
    public static void deleteSongsByPlaylistId(int playlistId) {
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement deleteSongsStmt = conn.prepareStatement("DELETE FROM songs WHERE playlist_id = ?")) {
        deleteSongsStmt.setInt(1, playlistId);
        deleteSongsStmt.executeUpdate();
    } catch (SQLException e) {
        System.out.println("SQL Error: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    
    private static int getPlaylistId(Connection conn, String playlistName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM playlists WHERE name = ?")) {
            stmt.setString(1, playlistName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }


    
    
    public static List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet songsResultSet = stmt.executeQuery("SELECT * FROM songs")) {

            while (songsResultSet.next()) {
                Song song = new Song(
                    songsResultSet.getInt("id"),
                    songsResultSet.getString("title"),
                    songsResultSet.getString("artist"),
                    songsResultSet.getString("album"),
                    songsResultSet.getInt("year"),
                    songsResultSet.getString("genre"),
                    songsResultSet.getString("comment"),
                    songsResultSet.getString("file_path")
                );
                songs.add(song);
            }
            System.out.println("Retrieved all songs from database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return songs;
    }

    
    
    public static void updateComment(int songId, String comment) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement updateCommentStmt = conn.prepareStatement("UPDATE songs SET comment = ? WHERE id = ?")) {

            updateCommentStmt.setString(1, comment);
            updateCommentStmt.setInt(2, songId);
            updateCommentStmt.executeUpdate();
            System.out.println("Updated comment having song ID: " + songId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    
    public static void createPlaylist(String playlistName) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement createPlaylistStmt = conn.prepareStatement("INSERT INTO playlists (name) VALUES (?)")) {

            createPlaylistStmt.setString(1, playlistName);
            createPlaylistStmt.executeUpdate();
            System.out.println("Playlist has been created: " + playlistName);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAllPlaylists() {
        List<String> playlists = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet playlistsResultSet = stmt.executeQuery("SELECT * FROM playlists")) {

            while (playlistsResultSet.next()) {
                playlists.add(playlistsResultSet.getString("name"));
            }
            System.out.println("Retrieved all playlists from database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playlists;
    }

    public static void deletePlaylist(String playlistName) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement playlistDeleteStmt = conn.prepareStatement("DELETE FROM playlists WHERE name = ?")) {

            playlistDeleteStmt.setString(1, playlistName);
            playlistDeleteStmt.executeUpdate();
            System.out.println("Playlist deleted: " + playlistName);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
public static Song getSongDetails(String title, String album, int year, String genre, String comment, String filePath) {
    Song song = null;
    String sql = "SELECT * FROM songs WHERE title = ? AND album = ? AND year = ? AND genre = ? AND comment = ? AND file_path = ?";
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, title);
        stmt.setString(2, album);
        stmt.setInt(3, year);
        stmt.setString(4, genre);
        stmt.setString(5, comment);
        stmt.setString(6, filePath);

        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            song = new Song(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("artist"),
                rs.getString("album"),
                rs.getInt("year"),
                rs.getString("genre"),
                rs.getString("comment"),
                rs.getString("file_path")
            );
            song.setPlaylistId(rs.getInt("playlist_id"));
        }
    } catch (SQLException e) {
        System.out.println("SQL Error: " + e.getMessage());
        e.printStackTrace();
    }
    return song;
}



public static void addSongToPlaylist(String title, String album, int year, String genre, String comment, String filePath, String playlistName) {
    System.out.println("Attempting to update song in the database:");
    System.out.println("Title: " + title);
    System.out.println("Album: " + album);
    System.out.println("Year: " + year);
    System.out.println("Genre: " + genre);
    System.out.println("Comment: " + comment);
    System.out.println("File Path: " + filePath);
    System.out.println("New Artist (Playlist Name): " + playlistName);

    // Fetch the song details before updating
    Song song = getSongDetails(title, album, year, genre, comment, filePath);
    if (song == null) {
        System.out.println("Song not found in the database. Cannot update artist.");
        return;
    }

    int playlistId = getPlaylistIdByName(playlistName);
    if (playlistId != -1) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
//             PreparedStatement updateSongStmt = conn.prepareStatement(
//                     "UPDATE songs SET artist = ?, playlist_id = ? WHERE id = ?")) {
//
//            updateSongStmt.setString(1, playlistName);
//            updateSongStmt.setInt(2, playlistId);
//            updateSongStmt.setInt(3, song.getId());
//            updateSongStmt.executeUpdate();
                
                PreparedStatement updateSongStmt = conn.prepareStatement(
                 "UPDATE songs SET playlist_id = ? WHERE id = ?")) {

                updateSongStmt.setInt(1, playlistId);
                updateSongStmt.setInt(2, song.getId());
                updateSongStmt.executeUpdate();

            // Insert into playlist_songs table
            try (PreparedStatement insertPlaylistSongStmt = conn.prepareStatement(
                    "INSERT INTO playlist_songs (playlist_id, song_id) VALUES (?, ?)")) {
                insertPlaylistSongStmt.setInt(1, playlistId);
                insertPlaylistSongStmt.setInt(2, song.getId());
                insertPlaylistSongStmt.executeUpdate();
            }

            System.out.println("Updated song " + title + " with new artist " + playlistName + " and playlist ID: " + playlistId);
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
    } else {
        System.out.println("Playlist not found: " + playlistName);
    }
}


public static Song getSongById(int songId) {
    Song song = null;
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM songs WHERE id = ?")) {
        stmt.setInt(1, songId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            song = new Song(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("artist"),
                rs.getString("album"),
                rs.getInt("year"),
                rs.getString("genre"),
                rs.getString("comment"),
                rs.getString("file_path")
            );
        }
    } catch (SQLException e) {
        System.out.println("SQL Error: " + e.getMessage());
        e.printStackTrace();
    }
    return song;
}

public static int getPlaylistIdByName(String playlistName) {
    int playlistId = -1;
    String sql = "SELECT id FROM playlists WHERE name = ?";
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, playlistName);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            playlistId = rs.getInt("id");
        }
    } catch (SQLException e) {
        System.out.println("SQL Error: " + e.getMessage());
        e.printStackTrace();
    }
    return playlistId;
}

public static void updateSongArtist(int songId, String newArtist) {
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement updateStmt = conn.prepareStatement(
                 "UPDATE songs SET artist = ? WHERE id = ?")) {

        updateStmt.setString(1, newArtist);
        updateStmt.setInt(2, songId);

        int rowsAffected = updateStmt.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("Updated song ID " + songId + " with new artist " + newArtist);
        } else {
            System.out.println("No song updated. Please check the provided details.");
        }
    } catch (SQLException e) {
        System.out.println("SQL Error: " + e.getMessage());
        e.printStackTrace();
    }
}




public static void printSongDetails(String title, String album, int year, String genre, String comment, String filePath) {
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM songs WHERE title = ? AND album = ? AND year = ? AND genre = ? AND comment = ? AND file_path = ?")) {

        stmt.setString(1, title);
        stmt.setString(2, album);
        stmt.setInt(3, year);
        stmt.setString(4, genre);
        stmt.setString(5, comment);
        stmt.setString(6, filePath.replace("\\", "\\\\"));

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            System.out.println("Current song details in the database:");
            System.out.println("ID: " + rs.getInt("id"));
            System.out.println("Title: " + rs.getString("title"));
            System.out.println("Artist: " + rs.getString("artist"));
            System.out.println("Album: " + rs.getString("album"));
            System.out.println("Year: " + rs.getInt("year"));
            System.out.println("Genre: " + rs.getString("genre"));
            System.out.println("Comment: " + rs.getString("comment"));
            System.out.println("File Path: " + rs.getString("file_path"));
        }
    } catch (SQLException e) {
        System.out.println("SQL Error: " + e.getMessage());
        e.printStackTrace();
    }
}















    static Song getSongByTitleAndArtist(String title, String artist) {
        Song song = null;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM songs WHERE title = ? AND artist = ?")) {
            stmt.setString(1, title);
            stmt.setString(2, artist);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                song = new Song(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("artist"),
                    rs.getString("album"),
                    rs.getInt("year"),
                    rs.getString("genre"),
                    rs.getString("comment"),
                    rs.getString("file_path")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return song;
    }

    public static List<Song> getSongsForPlaylist(String playlistName) {
        List<Song> songs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement playlistSongsStmt = conn.prepareStatement(
                     "SELECT s.* FROM songs s " +
                             "JOIN playlist_songs ps ON s.id = ps.song_id " +
                             "JOIN playlists p ON ps.playlist_id = p.id " +
                             "WHERE p.name = ?")) {

            playlistSongsStmt.setString(1, playlistName);
            ResultSet songsResultSet = playlistSongsStmt.executeQuery();

            while (songsResultSet.next()) {
                Song song = new Song(
                        songsResultSet.getInt("id"),
                        songsResultSet.getString("title"),
                        songsResultSet.getString("artist"),
                        songsResultSet.getString("album"),
                        songsResultSet.getInt("year"),
                        songsResultSet.getString("genre"),
                        songsResultSet.getString("comment"),
                        songsResultSet.getString("file_path")
                );
                songs.add(song);
            }
            System.out.println("Songs retrieved for the playlist: " + playlistName);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return songs;
    }

    public static List<Song> getSongsByArtist(String artist) {
    List<Song> songs = new ArrayList<>();
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement artistSongsStmt = conn.prepareStatement("SELECT * FROM songs WHERE artist = ?")) {
        artistSongsStmt.setString(1, artist);
        ResultSet artistSongsResultSet = artistSongsStmt.executeQuery();
        while (artistSongsResultSet.next()) {
            Song song = new Song(
                artistSongsResultSet.getInt("id"),
                artistSongsResultSet.getString("title"),
                artistSongsResultSet.getString("artist"),
                artistSongsResultSet.getString("album"),
                artistSongsResultSet.getInt("year"),
                artistSongsResultSet.getString("genre"),
                artistSongsResultSet.getString("comment"),
                artistSongsResultSet.getString("file_path")
            );
            songs.add(song);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return songs;
}
    
    
    
//    public static void addSongToPlaylist(Song song, int playlistId) {
//    try (Connection conn = DriverManager.getConnection(DB_URL);
////         PreparedStatement ps = conn.prepareStatement("UPDATE songs SET artist = ?, playlist_id = ? WHERE id = ?")) {
////        ps.setString(1, song.getArtist());
////        ps.setInt(2, playlistId);
////        ps.setInt(3, song.getId());
////        ps.executeUpdate();
//            
//            
//            
//            PreparedStatement ps = conn.prepareStatement("UPDATE songs SET playlist_id = ? WHERE id = ?")){
//                ps.setInt(1, playlistId);
//                ps.setInt(2, song.getId());
//                ps.executeUpdate();
//    } catch (SQLException e) {
//        e.printStackTrace();
//    }
//}
    
    public static void addSongToPlaylist(Song song, int playlistId) {
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement updateSongStmt = conn.prepareStatement("UPDATE songs SET playlist_id = ? WHERE id = ?");
         PreparedStatement insertPlaylistSongStmt = conn.prepareStatement(
                 "INSERT INTO playlist_songs (playlist_id, song_id) VALUES (?, ?)")) {
        
        // Update the songs table
        updateSongStmt.setInt(1, playlistId);
        updateSongStmt.setInt(2, song.getId());
        updateSongStmt.executeUpdate();
        
        // Insert into playlist_songs table
        insertPlaylistSongStmt.setInt(1, playlistId);
        insertPlaylistSongStmt.setInt(2, song.getId());
        insertPlaylistSongStmt.executeUpdate();
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


    public static void main(String[] args) {
        initializeDatabase();
    }
}
