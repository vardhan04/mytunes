
package com.mycompany.cecs544gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.FloatControl;

public class PlaylistWindow extends JFrame {
    private String playlistName;
    private List<Song> songs;
    private SongPlayer songPlayer;
    private MyTunesUI mainUI;
    private DefaultTableModel playlistTableModel;
    private JTable playlistTable;
    private JSlider volumeSlider;
    private FloatControl volumeControl;

    public PlaylistWindow(String playlistName, List<Song> songs, SongPlayer songPlayer, MyTunesUI mainUI) {
        this.playlistName = playlistName;
        this.songs = songs;
        this.songPlayer = new SongPlayer();
        this.mainUI = mainUI;

        initUI();

        // Set DropTarget for the entire window
        new DropTarget(this, new FileDropTargetListener());
    }

    public void removeSongById(int songId) {
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId() == songId) {
                songs.remove(i);
                playlistTableModel.removeRow(i);
                updatePlaylist();
                break;
            }
        }
    }

    private void initUI() {
        setTitle(playlistName);
        setSize(750, 400);
        setLayout(new BorderLayout());

        JPanel controlsPanel = new JPanel();
        JButton playSongButton = new JButton("Play");
        JButton stopSongButton = new JButton("Stop");
        JButton pauseSongButton = new JButton("Pause");
        JButton resumeSongButton = new JButton("Unpause");
        JButton nextSongButton = new JButton("Next");
        JButton previousSongButton = new JButton("Previous");

        controlsPanel.add(playSongButton);
        controlsPanel.add(stopSongButton);
        controlsPanel.add(pauseSongButton);
        controlsPanel.add(resumeSongButton);
        controlsPanel.add(nextSongButton);
        controlsPanel.add(previousSongButton);
//               volumeSlider = new JSlider(0, 100, 50); // Initial value is set to 50
//                volumeSlider.addChangeListener(e -> {
//                    int volumeValue = volumeSlider.getValue();
//                    float volumePercentage = volumeValue / 100.0f;
//                    songPlayer.setVolume(volumePercentage);
//                });


            volumeSlider = new JSlider(0, 100, (int) (songPlayer.getVolume() * 100));
            volumeSlider.addChangeListener(e -> {
                float volumeValue = (float) volumeSlider.getValue() / 100.0f;
                songPlayer.setVolume(volumeValue);
            });
    controlsPanel.add(new JLabel("Volume:"));
    controlsPanel.add(volumeSlider);

        add(controlsPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem addSongItem = new JMenuItem("Add a song");
        JMenuItem deleteSongItem = new JMenuItem("Delete a song");
        JMenuItem exitItem = new JMenuItem("Exit");

        fileMenu.add(addSongItem);
        fileMenu.add(deleteSongItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        String[] columnNames = {"ID", "File Name", "Title", "Artist", "Album", "Year", "Genre", "Comment"};
        playlistTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };
        playlistTable = new JTable(playlistTableModel);
        JScrollPane playlistScrollPane = new JScrollPane(playlistTable);
        add(playlistScrollPane, BorderLayout.CENTER);

        // Hide the ID column
        TableColumnModel columnModel = playlistTable.getColumnModel();
        columnModel.getColumn(0).setMinWidth(0);
        columnModel.getColumn(0).setMaxWidth(0);
        columnModel.getColumn(0).setPreferredWidth(0);

        for (Song song : songs) {
            playlistTableModel.addRow(new Object[]{ song.getId(), song.getFileName(), song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment()});
        }

//        addSongItem.addActionListener(e -> {
//            JFileChooser fileChooser = new JFileChooser();
//            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 files", "mp3"));
//            int returnValue = fileChooser.showOpenDialog(this);
//            if (returnValue == JFileChooser.APPROVE_OPTION) {
//                File selectedFile = fileChooser.getSelectedFile();
//                // Ensure the playlistId is set and the artist name is updated
//                int playlistId = DatabaseManager.getPlaylistIdByName(playlistName);
//                if (playlistId != -1) {
//                    Song newSong = DatabaseManager.addSong(selectedFile, playlistId);
//                    if (newSong != null) {
//                        newSong.setArtist(playlistName);
//                        newSong.setPlaylistId(playlistId);
//
//                        // Update the artist and playlist ID in the database
//                        try (Connection conn = DriverManager.getConnection(DatabaseManager.DB_URL);
//                             PreparedStatement updateSongStmt = conn.prepareStatement(
//                                     "UPDATE songs SET artist = ?, playlist_id = ? WHERE id = ?")) {
//                            updateSongStmt.setString(1, playlistName);
//                            updateSongStmt.setInt(2, playlistId);
//                            updateSongStmt.setInt(3, newSong.getId());
//                            updateSongStmt.executeUpdate();
//                        } catch (SQLException ex2) {
//                            System.out.println("SQL Error: " + ex2.getMessage());
//                            ex2.printStackTrace();
//                        }
//
//                        playlistTableModel.addRow(new Object[]{newSong.getId(), newSong.getTitle(), newSong.getArtist(), newSong.getAlbum(), newSong.getYear(), newSong.getGenre(), newSong.getComment()});
//                        songs.add(newSong);
//                        updatePlaylist();  // Refresh the playlist table
//                    }
//                } else {
//                    System.out.println("Playlist not found: " + playlistName);
//                }
//            }
//        });

addSongItem.addActionListener(e -> {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 files", "mp3"));
    int returnValue = fileChooser.showOpenDialog(this);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        int playlistId = DatabaseManager.getPlaylistIdByName(playlistName);
        if (playlistId != -1) {
            Song newSong = DatabaseManager.addSong(selectedFile, playlistId);
            if (newSong != null) {
                newSong.setPlaylistId(playlistId);  // Only set playlistId, no need to update artist

                playlistTableModel.addRow(new Object[]{newSong.getId(), newSong.getFileName(), newSong.getTitle(), newSong.getArtist(), newSong.getAlbum(), newSong.getYear(), newSong.getGenre(), newSong.getComment()});
                songs.add(newSong);
                updatePlaylist();  // Refresh the playlist table
                playlistTableModel.fireTableDataChanged();
            }
        } else {
            System.out.println("Playlist not found: " + playlistName);
        }
    }
});


        deleteSongItem.addActionListener(e -> {
            int selectedRow = playlistTable.getSelectedRow();
            if (selectedRow != -1) {
                int songId = (Integer) playlistTableModel.getValueAt(selectedRow, 0); // Ensure it's cast to Integer
                mainUI.deleteSongById(songId);
            } else {
                JOptionPane.showMessageDialog(this, "Select a song to delete.");
            }
        });

        exitItem.addActionListener(e -> dispose());

        playSongButton.addActionListener(e -> playSelectedSong());
        stopSongButton.addActionListener(e -> songPlayer.stop());
        pauseSongButton.addActionListener(e -> songPlayer.pause());
        resumeSongButton.addActionListener(e -> songPlayer.unpause());
//        nextSongButton.addActionListener(e -> playNextSong());
//        previousSongButton.addActionListener(e -> playPreviousSong());

nextSongButton.addActionListener(e -> {
    if (songPlayer.isPlaying()) {
        playNextSong();
    } else {
        selectNextSong();
    }
});
previousSongButton.addActionListener(e -> {
    if (songPlayer.isPlaying()) {
        playPreviousSong();
    } else {
        playPreviousSong();
    }
});

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                songPlayer.stop(); // Stop the song when the window is closing
                mainUI.updateCurrentTable(mainUI.getSongTable());
            }
        });
        
        new DropTarget(playlistTable, new DropTargetAdapter() {
        @Override
        public void drop(DropTargetDropEvent dtde) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable transferable = dtde.getTransferable();
                if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String songDetails = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                    String[] details = songDetails.split(";");
                    if (details.length == 8) {
                        int id = Integer.parseInt(details[0]);
                        String title = details[1];
                        //String artist = playlistName; // Set artist to playlist name
                        String artist = details[2];
                        String album = details[3];
                        int year = Integer.parseInt(details[4]);
                        String genre = details[5];
                        String comment = details[6];
                        String filePath = details[7];

                        Song song = new Song(id, title, artist, album, year, genre, comment, filePath);
                        DatabaseManager.addSongToPlaylist(song, DatabaseManager.getPlaylistIdByName(playlistName));
                        songs.add(song);
                        playlistTableModel.addRow(new Object[]{song.getId(),song.getFileName(), song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment()});
                        updatePlaylist();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    });
    }


    
    public String getPlaylistName() {
    return playlistName;
}
    
    private void playSelectedSong() {
    int selectedRow = playlistTable.getSelectedRow();
    if (selectedRow != -1) {
        int songId = (Integer) playlistTableModel.getValueAt(selectedRow, 0); // Ensure it's cast to Integer
        Song song = DatabaseManager.getSongById(songId);
        if (song != null) {
            songPlayer.stop();
            songPlayer.play(song.getFilePath());
            //songPlayer.setPlaying(true); // Update isPlaying to true
        } else {
            JOptionPane.showMessageDialog(this, "Song not found.");
        }
    } else {
        JOptionPane.showMessageDialog(this, "Select a song to play.");
    }
}


    public void stopSong() {
        songPlayer.stop();
    }

   
public void updatePlaylist() {
    int playlistId = DatabaseManager.getPlaylistIdByName(playlistName);  // Assuming playlistName is still used to get the ID
    if (playlistId != -1) {
        songs = DatabaseManager.getSongsByPlaylistId(playlistId);
        playlistTableModel.setRowCount(0);
        for (Song song : songs) {
            playlistTableModel.addRow(new Object[]{song.getId(), song.getFileName(), song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment()});
        }
    } else {
        System.out.println("Playlist ID not found for playlist: " + playlistName);
    }
}

    
    // Select the next song in the playlist without playing it
private void selectNextSong() {
    int selectedRow = playlistTable.getSelectedRow();
    if (selectedRow < songs.size() - 1) {
        playlistTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
    } else {
        playlistTable.setRowSelectionInterval(0, 0); // Wrap around to the first song
    }
}

// Select the previous song in the playlist without playing it
private void selectPreviousSong() {
    int selectedRow = playlistTable.getSelectedRow();
    if (selectedRow > 0) {
        playlistTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
    } else {
        playlistTable.setRowSelectionInterval(songs.size() - 1, songs.size() - 1); // Wrap around to the last song
    }
}

private void playNextSong() {
    int selectedRow = playlistTable.getSelectedRow();
    if (selectedRow < songs.size() - 1) {
        playlistTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
        if (songPlayer.isPlaying()) {
            songPlayer.stop(); // Stop current song before playing next
            playSelectedSong();
        }
    } else {
        JOptionPane.showMessageDialog(this, "No next song in the playlist.");
    }
}

//private void playPreviousSong() {
//    int selectedRow = playlistTable.getSelectedRow();
//    if (selectedRow > 0) {
//        playlistTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
//        if (songPlayer.isPlaying()) {
//            //songPlayer.stop(); // Stop current song before playing previous
//            playSelectedSong();
//        }
//    } else {
//        playSelectedSong();
//        //JOptionPane.showMessageDialog(this, "No previous song in the playlist.");
//    }
//}
//
private void playPreviousSong() {
    int selectedRow = playlistTable.getSelectedRow();
    if (selectedRow > 0) {
        // Move selection to the previous song
        playlistTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
    } else if (selectedRow == 0) {
        // If already at the first song, loop back to the last song
        playlistTable.setRowSelectionInterval(songs.size() - 1, songs.size() - 1);
    } else {
        JOptionPane.showMessageDialog(this, "No previous song in the playlist.");
        return;  // Exit the method if no song is selected or the table is empty
    }

    // Stop any currently playing song and play the newly selected song
    //songPlayer.stop();
    playSelectedSong();
}

public void addSongToPlaylistTable(Song song) {
    playlistTableModel.addRow(new Object[]{
        song.getId(),
        song.getFileName(),
        song.getTitle(),
        song.getArtist(),
        song.getAlbum(),
        song.getYear(),
        song.getGenre(),
        song.getComment()
    });
}   
//WORKING
//private class FileDropTargetListener extends DropTargetAdapter {
//    @Override
//    public void drop(DropTargetDropEvent evt) {
//        try {
//            evt.acceptDrop(DnDConstants.ACTION_COPY);
//            Transferable transferable = evt.getTransferable();
//            DataFlavor[] flavors = transferable.getTransferDataFlavors();
//
//            for (DataFlavor flavor : flavors) {
//                System.out.println("Available Data Flavor in playlist window: " + flavor);
//            }
//
//            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
//                List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
//
//                List<Song> addedSongs = mainUI.addSongsFromFiles(droppedFiles, playlistName);
//                for (Song song : addedSongs) {
//                    playlistTableModel.addRow(new Object[]{song.getId(), song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment()});
//                }
//                updatePlaylist();
//                playlistTableModel.fireTableDataChanged();
//                mainUI.updateCurrentTable(mainUI.getSongTable());
//                mainUI.refreshSongsAndTable();
//            } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
//                String data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
//                System.out.println("Dropped data: " + data);
//
//                // Split data into multiple songs if separated by new lines
//                String[] songsData = data.split("\n");
//                List<File> droppedFiles = new ArrayList<>();
//
//                for (String songData : songsData) {
//                    // Split each song data by semicolon
//                    String[] songFields = songData.split(";");
//                    if (songFields.length >= 8) {
//                        String filePath = songFields[7];
//                        File file = new File(filePath);
//                        if (file.exists()) {
//                            droppedFiles.add(file);
//                        }
//                    }
//                }
//
//                if (!droppedFiles.isEmpty()) {
//                    List<Song> addedSongs = mainUI.addSongsFromFiles(droppedFiles, playlistName);
//                    for (Song song : addedSongs) {
//                        playlistTableModel.addRow(new Object[]{song.getId(), song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment()});
//                    }
//                    playlistTableModel.fireTableDataChanged();
//                    mainUI.updateCurrentTable(mainUI.getSongTable());
//                    mainUI.refreshSongsAndTable();
//                }
//            } else {
//                System.out.println("Unsupported data flavor");
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//}


//private class FileDropTargetListener extends DropTargetAdapter {
//    @Override
//    public void drop(DropTargetDropEvent evt) {
//        try {
//            evt.acceptDrop(DnDConstants.ACTION_COPY);
//            Transferable transferable = evt.getTransferable();
//            DataFlavor[] flavors = transferable.getTransferDataFlavors();
//
//            for (DataFlavor flavor : flavors) {
//                System.out.println("Available Data Flavor in playlist window: " + flavor);
//            }
//
//            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
//                List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
//
//                for (File file : droppedFiles) {
//                    Song song = DatabaseManager.getSongByFilePath(file.getPath());
//
//                    if (song == null) {
//                        // Song does not exist in the library, add it to the library first
//                        song = DatabaseManager.addSong(file, null);
//                    }
//
//                    // Regardless of whether the song was already in the library, add it to the playlist
//                    if (song != null) {
//                        playlistTableModel.addRow(new Object[]{song.getId(), song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment()});
//                        DatabaseManager.addSongToPlaylist(song, DatabaseManager.getPlaylistIdByName(playlistName));
//                    }
//                }
//
//                updatePlaylist();
//                playlistTableModel.fireTableDataChanged();
//                mainUI.updateCurrentTable(mainUI.getSongTable());
//                mainUI.refreshSongsAndTable();
//            } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
//                String data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
//                System.out.println("Dropped data: " + data);
//
//                // Split data into multiple songs if separated by new lines
//                String[] songsData = data.split("\n");
//
//                for (String songData : songsData) {
//                    String[] songFields = songData.split(";");
//                    if (songFields.length >= 8) {
//                        String filePath = songFields[7];
//                        File file = new File(filePath);
//                        Song song = DatabaseManager.getSongByFilePath(file.getPath());
//
//                        if (song == null) {
//                            // Song does not exist in the library, add it to the library first
//                            song = DatabaseManager.addSong(file, null);
//                        }
//
//                        // Regardless of whether the song was already in the library, add it to the playlist
//                        if (song != null) {
//                            playlistTableModel.addRow(new Object[]{song.getId(), song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment()});
//                            DatabaseManager.addSongToPlaylist(song, DatabaseManager.getPlaylistIdByName(playlistName));
//                        }
//                    }
//                }
//
//                playlistTableModel.fireTableDataChanged();
//                mainUI.updateCurrentTable(mainUI.getSongTable());
//                mainUI.refreshSongsAndTable();
//            } else {
//                System.out.println("Unsupported data flavor");
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//}



private class FileDropTargetListener extends DropTargetAdapter {
    @Override
//    public void drop(DropTargetDropEvent evt) {
//        try {
//            evt.acceptDrop(DnDConstants.ACTION_COPY);
//            Transferable transferable = evt.getTransferable();
//            DataFlavor[] flavors = transferable.getTransferDataFlavors();
//
//            for (DataFlavor flavor : flavors) {
//                System.out.println("Available Data Flavor in playlist window: " + flavor);
//            }
//
//            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
//                List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
//
//                for (File file : droppedFiles) {
//                    Song song = DatabaseManager.getSongByFilePath(file.getPath());
//
//                    if (song == null) {
//                        // Song does not exist in the library, add it to the library first
//                        song = DatabaseManager.addSong(file, null);
//                    }
//
//                    // Regardless of whether the song was already in the library, add it to the playlist
//                    if (song != null) {
//                        // Add the song to the playlist in the database
//                        DatabaseManager.addSongToPlaylist(song, DatabaseManager.getPlaylistIdByName(playlistName));
//
//                        // Add the song to the playlistTableModel, even if it's the same song
//                        playlistTableModel.addRow(new Object[]{
//                            //song.getFileName(),
//                            song.getId(),
//                            song.getTitle(),
//                            song.getArtist(),
//                            song.getAlbum(),
//                            song.getYear(),
//                            song.getGenre(),
//                            song.getComment()
//                        });
//                    }
//                }
//                updatePlaylist();
//                playlistTableModel.fireTableDataChanged();
//                mainUI.updateCurrentTable(mainUI.getSongTable());
//                mainUI.refreshSongsAndTable();
//            } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
//                String data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
//                System.out.println("Dropped data: " + data);
//
//                // Split data into multiple songs if separated by new lines
//                String[] songsData = data.split("\n");
//
//                for (String songData : songsData) {
//                    String[] songFields = songData.split(";");
//                    if (songFields.length >= 8) {
//                        String filePath = songFields[7];
//                        File file = new File(filePath);
//                        Song song = DatabaseManager.getSongByFilePath(file.getPath());
//
//                        if (song == null) {
//                            // Song does not exist in the library, add it to the library first
//                            song = DatabaseManager.addSong(file, null);
//                        }
//
//                        // Regardless of whether the song was already in the library, add it to the playlist
//                        if (song != null) {
//                            // Add the song to the playlist in the database
//                            DatabaseManager.addSongToPlaylist(song, DatabaseManager.getPlaylistIdByName(playlistName));
//
//                            // Add the song to the playlistTableModel, even if it's the same song
//                            playlistTableModel.addRow(new Object[]{
//                                //song.getFileName(),
//                                song.getId(),
//                                song.getTitle(),
//                                song.getArtist(),
//                                song.getAlbum(),
//                                song.getYear(),
//                                song.getGenre(),
//                                song.getComment()
//                            });
//                        }
//                    }
//                }
//                updatePlaylist();
//                playlistTableModel.fireTableDataChanged();
//                mainUI.updateCurrentTable(mainUI.getSongTable());
//                mainUI.refreshSongsAndTable();
//            } else {
//                System.out.println("Unsupported data flavor");
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
    

public void drop(DropTargetDropEvent evt) {
    try {
        evt.acceptDrop(DnDConstants.ACTION_COPY);
        Transferable transferable = evt.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();

        for (DataFlavor flavor : flavors) {
            System.out.println("Available Data Flavor in playlist window: " + flavor);
        }

        String baseDirectory = "C:\\Users\\saiva\\Documents\\mp3files\\"; // Set your base directory here

        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

            for (File file : droppedFiles) {
                // Convert relative path to absolute path
                if (!file.isAbsolute()) {
                    file = new File(baseDirectory, file.getPath());
                }

                Song song = DatabaseManager.getSongByFilePath(file.getPath());

                if (song == null) {
                    // Song does not exist in the library, add it to the library first
                    song = DatabaseManager.addSong(file, null);
                }

                // Regardless of whether the song was already in the library, add it to the playlist
                if (song != null) {
                    // Add the song to the playlist in the database
                    DatabaseManager.addSongToPlaylist(song, DatabaseManager.getPlaylistIdByName(playlistName));

                    // Add the song to the playlistTableModel, even if it's the same song
                    playlistTableModel.addRow(new Object[]{
                        song.getId(),
                        song.getFileName(),
                        song.getTitle(),
                        song.getArtist(),
                        song.getAlbum(),
                        song.getYear(),
                        song.getGenre(),
                        song.getComment()
                    });
                }
            }
            updatePlaylist();
            playlistTableModel.fireTableDataChanged();
            mainUI.updateCurrentTable(mainUI.getSongTable());
            mainUI.refreshSongsAndTable();
        } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            String data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            System.out.println("Dropped data: " + data);

            // Split data into multiple songs if separated by new lines
            String[] songsData = data.split("\n");

            for (String songData : songsData) {
                String[] songFields = songData.split(";");
                if (songFields.length >= 8) {
                    String filePath = songFields[7];
                    File file = new File(filePath);

                    // Convert relative path to absolute path based on the base directory
                    if (!file.isAbsolute()) {
                        file = new File(baseDirectory, file.getPath());
                    }

                    Song song = DatabaseManager.getSongByFilePath(file.getPath());

                    if (song == null) {
                        // Song does not exist in the library, add it to the library first
                        song = DatabaseManager.addSong(file, null);
                    }

                    // Regardless of whether the song was already in the library, add it to the playlist
                    if (song != null) {
                        // Add the song to the playlist in the database
                        DatabaseManager.addSongToPlaylist(song, DatabaseManager.getPlaylistIdByName(playlistName));

                        // Add the song to the playlistTableModel, even if it's the same song
                        playlistTableModel.addRow(new Object[]{
                            song.getId(),
                            song.getFileName(),
                            song.getTitle(),
                            song.getArtist(),
                            song.getAlbum(),
                            song.getYear(),
                            song.getGenre(),
                            song.getComment()
                        });
                    }
                }
            }
            updatePlaylist();
            playlistTableModel.fireTableDataChanged();
            mainUI.updateCurrentTable(mainUI.getSongTable());
            mainUI.refreshSongsAndTable();
        } else {
            System.out.println("Unsupported data flavor");
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}

}



}
