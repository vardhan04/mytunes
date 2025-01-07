package com.mycompany.cecs544gui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.sound.sampled.FloatControl;
import static javax.swing.TransferHandler.COPY;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class MyTunesUI extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MyTunesUI.class.getName());
    private JPanel controlsPanel;
    private JButton playSongButton, stopSongButton, pauseSongButton, resumeSongButton, nextSongButton, previousSongButton;
    private JTable songTable;
    private JTree directoryTree;
    private JScrollPane songScrollPane;
    private SongPlayer songPlayer;
    private DefaultTableModel songsTableModel;
    private List<Song> songs;
    private int currentSongPosition = -1;
    private DefaultMutableTreeNode libraryTreeNode;
    private DefaultMutableTreeNode playlistTreeNode;
    private JTable currentTable;
    private PlaylistWindow playlistWindow;
    private boolean isPlaying = false;
    private FloatControl volumeControl;
    private boolean shuffleEnabled = false;
    private JPopupMenu libraryPopupMenu;
    private JMenuItem addSongPopupItem;
    private JMenuItem deleteSongPopupItem;
    private JPopupMenu songPopupMenu;
    private JMenu addToPlaylistMenu;
    private JSlider volumeSlider;
    private JLabel elapsedTimeLabel;
    private JLabel remainingTimeLabel;
    private JProgressBar progressBar;
    private Timer songTimer;
    private int songDuration;
    private int elapsed;
    private boolean repeatEnabled = false;

    // Added for column selection popup menu and configuration persistence
    private JPopupMenu columnPopupMenu;
    private Map<String, Boolean> columnVisibilityMap;
    private static final String CONFIG_FILE = "column_config.txt";
    //private static final String CONFIG_FILE = System.getProperty("user.home") + "/column_config.txt";

    private List<Song> recentSongs = new ArrayList<>();
    private static final String RECENT_SONGS_FILE = "recent_songs.txt";

    public MyTunesUI() {
        songPlayer = new SongPlayer();
        DatabaseManager.initializeDatabase();
        songs = DatabaseManager.getAllSongs();
        loadRecentSongs();
        // Initialize column visibility map
        columnVisibilityMap = new LinkedHashMap<>();
        loadColumnConfiguration();
        initUI();
        // Apply the visibility settings
        applyColumnVisibility();
        updateCurrentTable(songTable);  // Initialize currentTable with songTable
        loadSongsIntoTable();
        new DropTarget(this, new FileDropTargetListener());

        

        
    }

    private void initUI() {
        setTitle("MyTunes");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem addSongItem = new JMenuItem("Add a song");
        JMenuItem deleteSongItem = new JMenuItem("Delete a song");
        JMenuItem createPlaylistItem = new JMenuItem("Create Playlist");
        JMenuItem openSongItem = new JMenuItem("Open a song");
        JMenuItem exitItem = new JMenuItem("Exit");
        currentTable = songTable;

        fileMenu.add(openSongItem);
        fileMenu.add(addSongItem);
        fileMenu.add(deleteSongItem);
        fileMenu.add(createPlaylistItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        
        JMenu controlsMenu = new JMenu("Controls");

    JMenuItem playItem = new JMenuItem("Play");
    playItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
    playItem.addActionListener(e -> playSelectedSong(currentTable));

    JMenuItem nextItem = new JMenuItem("Next");
    nextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK));
    nextItem.addActionListener(e -> playNextSong(songTable, true));

    JMenuItem previousItem = new JMenuItem("Previous");
    previousItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK));
    previousItem.addActionListener(e -> playPreviousSong(songTable));

    JMenu playRecentMenu = new JMenu("Play Recent");

    updatePlayRecentMenu(playRecentMenu);
    JCheckBoxMenuItem shuffleItem = new JCheckBoxMenuItem("Shuffle");
//    shuffleItem.addActionListener(e -> shuffleEnabled = shuffleItem.isSelected());
shuffleItem.addActionListener(e -> {
    shuffleEnabled = shuffleItem.isSelected();
    if (shuffleEnabled && !songPlayer.isPlaying()) {
        playNextShuffledSong(); // Start shuffling immediately if no song is playing
    }
});

    
     JMenuItem goToCurrentSongItem = new JMenuItem("Go to Current Song");
    goToCurrentSongItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
    goToCurrentSongItem.addActionListener(e -> goToCurrentSong());
    
    JMenuItem increaseVolumeItem = new JMenuItem("Increase Volume");
    increaseVolumeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK));
    increaseVolumeItem.addActionListener(e -> adjustVolume(5));
    
    
    JMenuItem decreaseVolumeItem = new JMenuItem("Decrease Volume");
    decreaseVolumeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
    decreaseVolumeItem.addActionListener(e -> adjustVolume(-5));
    
    
    JCheckBoxMenuItem repeatItem = new JCheckBoxMenuItem("Repeat");
    repeatItem.addActionListener(e -> repeatEnabled = repeatItem.isSelected());
    
    controlsMenu.add(playItem);
    controlsMenu.add(nextItem);
    controlsMenu.add(previousItem);
    controlsMenu.add(playRecentMenu);
    
    controlsMenu.add(goToCurrentSongItem);
    controlsMenu.addSeparator();
    controlsMenu.add(increaseVolumeItem);
    controlsMenu.add(decreaseVolumeItem);
    controlsMenu.addSeparator();
    controlsMenu.add(shuffleItem);
    controlsMenu.add(repeatItem);
    menuBar.add(controlsMenu);
        // Panel for controls, timers, and progress bar
        JPanel timerPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        elapsedTimeLabel = new JLabel("0:00:00", SwingConstants.LEFT);
        remainingTimeLabel = new JLabel("0:00:00", SwingConstants.RIGHT);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        timerPanel.add(elapsedTimeLabel);
        timerPanel.add(progressBar);
        timerPanel.add(remainingTimeLabel);

        add(timerPanel, BorderLayout.NORTH);

        controlsPanel = new JPanel();
        playSongButton = new JButton("Play");
        stopSongButton = new JButton("Stop");
        pauseSongButton = new JButton("Pause");
        resumeSongButton = new JButton("Unpause");
        nextSongButton = new JButton("Next");
        previousSongButton = new JButton("Previous");

        controlsPanel.add(playSongButton);
        controlsPanel.add(stopSongButton);
        controlsPanel.add(pauseSongButton);
        controlsPanel.add(resumeSongButton);
        controlsPanel.add(nextSongButton);
        controlsPanel.add(previousSongButton);

        volumeSlider = new JSlider(0, 100, (int) (songPlayer.getVolume() * 100));
        volumeSlider.addChangeListener(e -> {
            float volumeValue = (float) volumeSlider.getValue() / 100.0f;
            songPlayer.setVolume(volumeValue);
        });

        controlsPanel.add(new JLabel("Volume:"));
        controlsPanel.add(volumeSlider);

        add(controlsPanel, BorderLayout.SOUTH);

        String[] columnNames = {"File Name", "Title", "Artist", "Album", "Year", "Genre", "Comment"};
        songsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Updated to the new "Comment" column index
            }
        };
        songTable = new JTable(songsTableModel);
        
        songTable.setAutoCreateRowSorter(true);
        songTable.getRowSorter().toggleSortOrder(1); // Default sorting on "Title" column
        songScrollPane = new JScrollPane(songTable);
        add(songScrollPane, BorderLayout.CENTER);

        // Hide the "File Name" column by setting its width to 0
        songTable.getColumnModel().getColumn(0).setMinWidth(0);
        songTable.getColumnModel().getColumn(0).setMaxWidth(0);
        songTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        songsTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    if (column == 6) { // Updated column index for "Comment"
                        String newComment = (String) songsTableModel.getValueAt(row, column);
                        int songId = songs.get(row).getId();
                        DatabaseManager.updateComment(songId, newComment);
                    }
                }
            }
        });

        libraryTreeNode = new DefaultMutableTreeNode("Library");
        playlistTreeNode = new DefaultMutableTreeNode("Playlist");

        DefaultMutableTreeNode[] nodes = {libraryTreeNode, playlistTreeNode};
        DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Root") {
            {
                for (DefaultMutableTreeNode node : nodes) {
                    add(node);
                }
            }
        });

        directoryTree = new JTree(treeModel);
        directoryTree.setRootVisible(false);
        JScrollPane treeScrollPane = new JScrollPane(directoryTree);
        add(treeScrollPane, BorderLayout.WEST);

        // Initialize the popup menu
        libraryPopupMenu = new JPopupMenu();
        addSongPopupItem = new JMenuItem("Add a song");
        deleteSongPopupItem = new JMenuItem("Delete a song");

        libraryPopupMenu.add(addSongPopupItem);
        libraryPopupMenu.add(deleteSongPopupItem);

        addSongPopupItem.addActionListener(e -> addSong());
        deleteSongPopupItem.addActionListener(e -> deleteSong());

        addSongItem.addActionListener(e -> addSong());
        deleteSongItem.addActionListener(e -> deleteSong());
        openSongItem.addActionListener(e -> openSong());
        createPlaylistItem.addActionListener(e -> createPlaylist());
        exitItem.addActionListener(e -> System.exit(0));

        playSongButton.addActionListener(e -> playSelectedSong(currentTable));
        stopSongButton.addActionListener(e -> {
    songPlayer.stop(); // Stop the song
    if (songTimer != null) {
        songTimer.stop(); // Stop the timer controlling the progress bar
    }
    progressBar.setValue(0); // Reset the progress bar
    elapsedTimeLabel.setText("0:00:00"); // Reset the elapsed time label
    remainingTimeLabel.setText("0:00:00"); // Reset the remaining time label to 0:00:00
    isPlaying = false; // Update the playing state
});
        pauseSongButton.addActionListener(e -> {
    if (songTimer != null) {
        songTimer.stop(); // Stop the timer controlling the progress bar
    }
    songPlayer.pause(); // Pause the song
    isPlaying = false;  // Update the playing state
});
        resumeSongButton.addActionListener(e -> {
    if (songTimer != null) {
        songTimer.start(); // Restart the timer controlling the progress bar
    }
    songPlayer.unpause(); // Resume the song
    isPlaying = true;     // Update the playing state
});

        nextSongButton.addActionListener(e -> {
            if (songPlayer.isPlaying()) {
                playNextSong(songTable, true);
            } else {
                selectNextSong();
            }
        });
        previousSongButton.addActionListener(e -> {
            if (songPlayer.isPlaying()) {
                playPreviousSong(songTable);
            } else {
                selectPreviousSong();
            }
        });

        // Initialize song table popup menu
        songPopupMenu = new JPopupMenu();
        JMenuItem playSongPopupItem = new JMenuItem("Play");
        JMenuItem deleteSongPopupItem = new JMenuItem("Delete");
        addToPlaylistMenu = new JMenu("Add to playlist");

        songPopupMenu.add(playSongPopupItem);
        songPopupMenu.add(deleteSongPopupItem);
        songPopupMenu.add(addToPlaylistMenu);

        playSongPopupItem.addActionListener(e -> playSelectedSong(songTable));
        deleteSongPopupItem.addActionListener(e -> deleteSong());

        songTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = songTable.rowAtPoint(e.getPoint());
                    songTable.setRowSelectionInterval(row, row);
                    updateAddToPlaylistMenu();
                    songPopupMenu.show(songTable, e.getX(), e.getY());
                }
            }
        });

        // Directory tree mouse listener for right-click and double-click actions
        directoryTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = directoryTree.getClosestRowForLocation(e.getX(), e.getY());
                    directoryTree.setSelectionRow(row);
                    TreePath path = directoryTree.getPathForRow(row);
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node != null) {
                        if (node == libraryTreeNode || node == playlistTreeNode) {
                            libraryPopupMenu.show(directoryTree, e.getX(), e.getY());
                        } else {
                            JPopupMenu popupMenu = new JPopupMenu();
                            JMenuItem openInNewWindowMenuItem = new JMenuItem("Open in new window");
                            JMenuItem deletePlaylistMenuItem = new JMenuItem("Delete Playlist");

                            openInNewWindowMenuItem.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    Object userObject = node.getUserObject();
                                    if (userObject instanceof String) {
                                        openInNewWindow((String) userObject);
                                    } else {
                                        JOptionPane.showMessageDialog(null, "Cannot open: Invalid node type.");
                                    }
                                }
                            });

                            deletePlaylistMenuItem.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    Object userObject = node.getUserObject();
                                    if (userObject instanceof String) {
                                        deletePlaylist((String) userObject);
                                    } else {
                                        JOptionPane.showMessageDialog(null, "Cannot delete: Invalid node type.");
                                    }
                                }
                            });

                            popupMenu.add(openInNewWindowMenuItem);
                            popupMenu.add(deletePlaylistMenuItem);
                            popupMenu.show(directoryTree, e.getX(), e.getY());
                        }
                    }
                } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    TreePath path = directoryTree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node == libraryTreeNode) {
                        loadSongsIntoTable();
                    } else if (node == playlistTreeNode) {
                        if (directoryTree.isExpanded(path)) {
                            directoryTree.collapsePath(path);
                        } else {
                            directoryTree.expandPath(path);
                        }
                    } else {
                        String playlistName = node.getUserObject().toString();
                        int playlistId = DatabaseManager.getPlaylistIdByName(playlistName);
                        if (playlistId != -1) {
                            loadSongsByPlaylist(playlistId);
                        } else {
                            System.out.println("Playlist ID not found for playlist: " + playlistName);
                        }
                    }
                }
            }
        });

        // Initialize column selection popup menu
        initializeColumnPopupMenu();

        // Add mouse listener to the table header to show column selection popup
        songTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    columnPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        updateTree();
        songTable.setDragEnabled(true);
        songTable.setTransferHandler(new SongTransferHandler());
        new DropTarget(this, new FileDropTargetListener());
    }

    // Initialize the popup menu for column selection
//    private void initializeColumnPopupMenu() {
//        columnPopupMenu = new JPopupMenu();
//        String[] columns = {"Artist", "Album", "Year", "Genre", "Comment"};
//
//        // Create a checkbox menu item for each column
//        for (String column : columns) {
//            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(column, true);
//            columnVisibilityMap.put(column, true);
//            menuItem.addActionListener(e -> toggleColumnVisibility(column));
//            columnPopupMenu.add(menuItem);
//        }
//    }
    
    private void initializeColumnPopupMenu() {
    columnPopupMenu = new JPopupMenu();
    String[] columns = {"Artist", "Album", "Year", "Genre", "Comment"};

    // Create a checkbox menu item for each column
    for (String column : columns) {
        // Get the visibility state from the columnVisibilityMap or default to true if not found
        boolean isVisible = columnVisibilityMap.getOrDefault(column, true);
        
        // Create the checkbox menu item with the initial state
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(column, isVisible);
        
        // Add an action listener to handle toggling visibility
        menuItem.addActionListener(e -> {
            boolean newVisibility = menuItem.isSelected();
            toggleColumnVisibilityDirect(column, newVisibility); // Apply the visibility
        });
        
        // Add the menu item to the popup menu
        columnPopupMenu.add(menuItem);
    }
}

    // Toggle the visibility of the selected column
//    private void toggleColumnVisibility(String column) {
//        int index = getColumnIndex(column);
//        if (index != -1) {
//            boolean visible = columnVisibilityMap.get(column);
//            if (visible) {
//                // Hide the column
//                songTable.getColumnModel().getColumn(index).setMinWidth(0);
//                songTable.getColumnModel().getColumn(index).setMaxWidth(0);
//                songTable.getColumnModel().getColumn(index).setPreferredWidth(0);
//                System.out.println("Hiding column: " + column);
//            } else {
//                // Show the column with default settings
//                songTable.getColumnModel().getColumn(index).setMinWidth(75);
//                songTable.getColumnModel().getColumn(index).setMaxWidth(Integer.MAX_VALUE);
//                songTable.getColumnModel().getColumn(index).setPreferredWidth(150);
//                System.out.println("Showing column: " + column); // Debugging line
//            }
//            columnVisibilityMap.put(column, !visible);
//            saveColumnConfiguration(); // Save configuration whenever it is changed
//        }
//    }
    
    private void toggleColumnVisibilityDirect(String column, boolean isVisible) {
    int index = getColumnIndex(column);
    if (index != -1) {
        if (isVisible) {
            // Show the column with default settings
            songTable.getColumnModel().getColumn(index).setMinWidth(75);
            songTable.getColumnModel().getColumn(index).setMaxWidth(Integer.MAX_VALUE);
            songTable.getColumnModel().getColumn(index).setPreferredWidth(150);
            //System.out.println("Showing column: " + column); // Debugging line
        } else {
            // Hide the column
            songTable.getColumnModel().getColumn(index).setMinWidth(0);
            songTable.getColumnModel().getColumn(index).setMaxWidth(0);
            songTable.getColumnModel().getColumn(index).setPreferredWidth(0);
            System.out.println("Hiding column: " + column); // Debugging line
        }
        // Update and save the visibility state
        columnVisibilityMap.put(column, isVisible);
        saveColumnConfiguration(); // Save configuration whenever it is changed
        //System.out.println("Saved new visibility state for column: " + column);
    } 
}
    
//    private void goToCurrentSong() {
//    int rowToSelect;
//    
//    if (songPlayer.isPlaying() && currentSongPosition >= 0) {
//        rowToSelect = currentSongPosition;
//    } else if (songTable.getSelectedRow() >= 0) {
//        rowToSelect = songTable.getSelectedRow();
//    } else {
//        return; // No song is playing or selected, do nothing
//    }
//
//    songTable.setRowSelectionInterval(rowToSelect, rowToSelect);
//    songTable.scrollRectToVisible(new Rectangle(songTable.getCellRect(rowToSelect, 0, true)));
//}
    
    
    private void goToCurrentSong() {
    if (currentSongPosition >= 0 && currentSongPosition < songs.size()) {
        int viewRow = songTable.convertRowIndexToView(currentSongPosition);
        songTable.setRowSelectionInterval(viewRow, viewRow);
        songTable.scrollRectToVisible(new Rectangle(songTable.getCellRect(viewRow, 0, true)));
    } else {
        JOptionPane.showMessageDialog(this, "No song is currently playing.");
    }
}

    private void adjustVolume(int change) {
    int newVolume = volumeSlider.getValue() + change;
    if (newVolume > 100) {
        newVolume = 100;
    } else if (newVolume < 0) {
        newVolume = 0;
    }

    volumeSlider.setValue(newVolume);
    float volumeValue = newVolume / 100.0f;
    songPlayer.setVolume(volumeValue);
}


    // Get the index of the column by its name
    private int getColumnIndex(String columnName) {
        for (int i = 0; i < songTable.getColumnModel().getColumnCount(); i++) {
            if (songTable.getColumnModel().getColumn(i).getHeaderValue().equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    // Load the column visibility configuration from a file
    private void loadColumnConfiguration() {
        System.out.println("Loading column configuration..."); // Debugging line
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    columnVisibilityMap.put(parts[0], Boolean.parseBoolean(parts[1]));
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load column configuration: " + e.getMessage());
        }
    }

    // Save the current column visibility configuration to a file
    private void saveColumnConfiguration() {
        //System.out.println("Saving column configuration...");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            for (Map.Entry<String, Boolean> entry : columnVisibilityMap.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Could not save column configuration: " + e.getMessage());
        }
    }

    // Apply the saved column visibility configuration
//    private void applyColumnVisibility() {
//    System.out.println("Applying column visibility settings...");
//    for (Map.Entry<String, Boolean> entry : columnVisibilityMap.entrySet()) {
//        System.out.println("Column: " + entry.getKey() + ", Visible: " + entry.getValue());
//        if (!entry.getValue()) {
//            toggleColumnVisibility(entry.getKey());  // Apply visibility settings
//        }
//    }
//}
    
    private void applyColumnVisibility() {
    //System.out.println("Applying column visibility settings...");
    try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length == 2) {
                String column = parts[0];
                boolean isVisible = Boolean.parseBoolean(parts[1]);
                //System.out.println("Applying visibility for " + column + ": " + isVisible);
                toggleColumnVisibilityDirect(column, isVisible);
            }
        }
    } catch (IOException e) {
        System.out.println("Could not apply column visibility: " + e.getMessage());
    }
}

    
    private void loadRecentSongs() {
    recentSongs.clear();
    try (BufferedReader reader = new BufferedReader(new FileReader(RECENT_SONGS_FILE))) {
        String line;
        while ((line = reader.readLine()) != null) {
            int songId = Integer.parseInt(line);
            Song song = DatabaseManager.getSongById(songId);
            if (song != null) {
                recentSongs.add(song);
            }
        }
    } catch (IOException e) {
        System.out.println("Could not load recent songs: " + e.getMessage());
    }
}
    
    private void addToRecentSongs(Song song) {
    if (!recentSongs.contains(song)) {
        if (recentSongs.size() == 10) {
            recentSongs.remove(0); // Remove the oldest entry
        }
    } else {
        recentSongs.remove(song); // Remove and re-add to move it to the end
    }
    recentSongs.add(song);
    saveRecentSongs(); // Save the updated list
}

private void saveRecentSongs() {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECENT_SONGS_FILE))) {
        for (Song song : recentSongs) {
            writer.write(String.valueOf(song.getId()));
            writer.newLine();
        }
    } catch (IOException e) {
        System.out.println("Could not save recent songs: " + e.getMessage());
    }
}

private void updatePlayRecentMenu(JMenu playRecentMenu) {
    playRecentMenu.removeAll();
    for (Song song : recentSongs) {
        JMenuItem recentItem = new JMenuItem(song.getTitle() + " - " + song.getArtist());
        recentItem.addActionListener(e -> {
            playSelectedSong(song);
            addToRecentSongs(song); // Move the song to the end of the recent list
        });
        playRecentMenu.add(recentItem);
    }
}


    
 
    
        private class SongTransferHandler extends TransferHandler {
    @Override
    protected Transferable createTransferable(JComponent c) {
        int[] selectedRows = songTable.getSelectedRows();
        StringBuilder sb = new StringBuilder();
        for (int row : selectedRows) {
            Song song = songs.get(row);
            sb.append(song.getId()).append(";")
              .append(song.getTitle()).append(";")
              .append(song.getArtist()).append(";")
              .append(song.getAlbum()).append(";")
              .append(song.getYear()).append(";")
              .append(song.getGenre()).append(";")
              .append(song.getComment()).append(";")
//              .append(song.getFilePath()).append("\n");
                    .append(song.getFileName())  // Use getFileName() here
              .append("\n");
        }
        return new StringSelection(sb.toString());
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }
        }
            private class FileDropTargetListener extends DropTargetAdapter {
    @Override
    public void drop(DropTargetDropEvent evt) {
        try {
            evt.acceptDrop(DnDConstants.ACTION_COPY);
            Transferable transferable = evt.getTransferable();
            DataFlavor[] flavors = transferable.getTransferDataFlavors();

            // Log all available data flavors
            for (DataFlavor flavor : flavors) {
                System.out.println("Available Data Flavor in mytunesui: " + flavor);
            }

            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                // Check if the drop occurred in the directory tree
                Point dropPoint = evt.getLocation();
                TreePath path = directoryTree.getPathForLocation(dropPoint.x, dropPoint.y);

                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.getParent() == playlistTreeNode) {
                        String playlistName = node.getUserObject().toString();
                        List<Song> addedSongs = addSongsFromFiles(droppedFiles, playlistName);
                        for (Song song : addedSongs) {
                            System.out.println("Added song: " + song.getTitle() + " - Artist: " + song.getArtist());
                        }
                        refreshSongsAndTable();
                    } else {
                        addSongsFromFiles(droppedFiles);
                        refreshSongsAndTable();
                    }
                } else {
                    addSongsFromFiles(droppedFiles);
                    refreshSongsAndTable();
                }
            } else {
                System.out.println("Unsupported data flavor");
            }
        } catch (UnsupportedFlavorException e) {
            System.out.println("Unsupported flavor: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error processing drop event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}





        private class SongTransferable implements Transferable {
            private final List<Song> songs;
            private final DataFlavor songFlavor = new DataFlavor(Song.class, "Song");

            public SongTransferable(List<Song> songs) {
                this.songs = songs;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { songFlavor };
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.equals(songFlavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                if (!isDataFlavorSupported(flavor)) {
                    return null;
                }
                return songs;
            }
    }

    private void loadSongsIntoTable() {
        songsTableModel.setRowCount(0);
        for (Song song : songs) {
            songsTableModel.addRow(new Object[]{
                song.getFileName(),  // Hidden
                song.getTitle(),
                song.getArtist(),
                song.getAlbum(),
                song.getYear(),
                song.getGenre(),
                song.getComment()
            });
        }
    }

    private void loadSongsByPlaylist(int playlistId) {
        songsTableModel.setRowCount(0);
        List<Song> playlistSongs = DatabaseManager.getSongsByPlaylistId(playlistId);

        for (Song song : playlistSongs) {
            songsTableModel.addRow(new Object[]{
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

    public void refreshSongsAndTable() {
        songs = DatabaseManager.getAllSongs();
        SwingUtilities.invokeLater(() -> {
            loadSongsIntoTable();
            System.out.println("Table refreshed with " + songs.size() + " songs");
        });
    }

    private void updateAddToPlaylistMenu() {
        addToPlaylistMenu.removeAll();
        List<String> playlists = DatabaseManager.getAllPlaylists();
        for (String playlist : playlists) {
            JMenuItem playlistMenuItem = new JMenuItem(playlist);
            playlistMenuItem.addActionListener(e -> addSongToPlaylist(playlist));
            addToPlaylistMenu.add(playlistMenuItem);
        }
    }

    private void addSongToPlaylist(String playlistName) {
        int selectedRow = songTable.getSelectedRow();
        if (selectedRow != -1) {
            Song song = songs.get(selectedRow);

            System.out.println("Adding song to playlist with parameters:");
            System.out.println("Title: " + song.getTitle());
            System.out.println("Original Artist: " + song.getArtist());
            System.out.println("Album: " + song.getAlbum());
            System.out.println("Year: " + song.getYear());
            System.out.println("Genre: " + song.getGenre());
            System.out.println("Comment: " + song.getComment());
            System.out.println("File Path: " + song.getFilePath());

            // Fetch song details from the database
            Song dbSong = DatabaseManager.getSongDetails(
                song.getTitle(),
                song.getAlbum(),
                song.getYear(),
                song.getGenre(),
                song.getComment(),
                song.getFilePath()
            );

            if (dbSong != null) {
                int playlistId = DatabaseManager.getPlaylistIdByName(playlistName);
                if (playlistId != -1) {
                    // Only update the playlist ID in the database
                    try (Connection conn = DriverManager.getConnection(DatabaseManager.DB_URL);
                         PreparedStatement updateSongStmt = conn.prepareStatement("UPDATE songs SET playlist_id = ? WHERE id = ?")) {

                        updateSongStmt.setInt(1, playlistId);
                        updateSongStmt.setInt(2, dbSong.getId());
                        updateSongStmt.executeUpdate();

                        song.setPlaylistId(playlistId);

                        // Insert into playlist_songs table
                        try (PreparedStatement insertPlaylistSongStmt = conn.prepareStatement(
                                "INSERT INTO playlist_songs (playlist_id, song_id) VALUES (?, ?)")) {
                            insertPlaylistSongStmt.setInt(1, playlistId);
                            insertPlaylistSongStmt.setInt(2, dbSong.getId());
                            insertPlaylistSongStmt.executeUpdate();
                        }

                        // Update the song object in the UI
                        songsTableModel.setValueAt(song.getArtist(), selectedRow, 1);  // Keep the original artist in the table

                        // Reload the songs from the database to refresh the table
                        songs = DatabaseManager.getAllSongs();
                        loadSongsIntoTable();

                        if (playlistWindow != null && playlistWindow.getPlaylistName().equals(playlistName)) {
                            playlistWindow.updatePlaylist();
                        }

                        JOptionPane.showMessageDialog(this, "Added " + song.getTitle() + " to " + playlistName + " playlist.");
                    } catch (SQLException e) {
                        System.out.println("SQL Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Playlist not found in the database. Please check the details.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Song not found in the database. Please check the details.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a song to add to playlist.");
        }
    }

    public void stopPlaylistWindowSong() {
        if (playlistWindow != null) {
            playlistWindow.stopSong();
        }
    }

//private void playSelectedSong(JTable table) {
//    int selectedRow = table.getSelectedRow();
//    if (selectedRow != -1 && selectedRow < songs.size()) {
//        int modelRow = table.convertRowIndexToModel(selectedRow);
//        Song song = songs.get(modelRow);
//        songPlayer.stop();
//        songPlayer.play(song.getFilePath());
//        currentSongPosition = modelRow;
//        setupTimers(song.getFilePath());
//    } else {
//        JOptionPane.showMessageDialog(this, "Select a song to play.");
//    }
//}
    
//    private void playSelectedSong(JTable table) {
//    if (shuffleEnabled) {
//        int randomIndex = new Random().nextInt(songs.size());
//        Song song = songs.get(randomIndex);
//        songPlayer.stop();
//        songPlayer.play(song.getFilePath());
//        currentSongPosition = randomIndex;
//        setupTimers(song.getFilePath());
//    } else {
//        int selectedRow = table.getSelectedRow();
//        if (selectedRow != -1) {
//            int modelRow = table.convertRowIndexToModel(selectedRow);
//            Song song = songs.get(modelRow);
//            songPlayer.stop();
//            songPlayer.play(song.getFilePath());
//            currentSongPosition = modelRow;
//            setupTimers(song.getFilePath());
//            addToRecentSongs(song);
//        } else if (!songs.isEmpty()) {
//            playFirstSong();
//        } else {
//            JOptionPane.showMessageDialog(this, "No songs available to play.");
//        }
//    }
//}
    
//    private void playSelectedSong(JTable table) {
//    if (shuffleEnabled) {
//        int randomIndex = new Random().nextInt(songs.size());
//        Song song = songs.get(randomIndex);
//        songPlayer.stop();
//        songPlayer.play(song.getFilePath());
//        currentSongPosition = randomIndex;
//        setupTimers(song.getFilePath());
//        addToRecentSongs(song);
//        table.setRowSelectionInterval(randomIndex, randomIndex);
//        table.scrollRectToVisible(new Rectangle(table.getCellRect(randomIndex, 0, true)));
//    } else {
//        int selectedRow = table.getSelectedRow();
//        if (selectedRow != -1) {
//            int modelRow = table.convertRowIndexToModel(selectedRow);
//            Song song = songs.get(modelRow);
//            songPlayer.stop();
//            songPlayer.play(song.getFilePath());
//            currentSongPosition = modelRow;
//            setupTimers(song.getFilePath());
//            addToRecentSongs(song);
//        } else if (!songs.isEmpty()) {
//            playFirstSong();
//        } else {
//            JOptionPane.showMessageDialog(this, "No songs available to play.");
//        }
//    }
//}
    
    private void playSelectedSong(JTable table) {
    if (shuffleEnabled) {
        int randomIndex = new Random().nextInt(songs.size());
        playSongAtIndex(randomIndex, table);
    } else {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            playSongAtIndex(modelRow, table);
        } else if (!songs.isEmpty()) {
            playFirstSong();
        } else {
            JOptionPane.showMessageDialog(this, "No songs available to play.");
        }
    }
}

    
//    private void playSongAtIndex(int index, JTable table) {
//    Song song = songs.get(index);
//    songPlayer.stop();
//    songPlayer.play(song.getFilePath());
//    currentSongPosition = index;
//    setupTimers(song.getFilePath());
//    addToRecentSongs(song);
//
//    // Select the song in the table and scroll it into view
//    int viewRow = table.convertRowIndexToView(index);
//    table.setRowSelectionInterval(viewRow, viewRow);
//    table.scrollRectToVisible(new Rectangle(table.getCellRect(viewRow, 0, true)));
//}
//
//
//
//private void playFirstSong() {
//    currentSongPosition = 0;
//    songTable.setRowSelectionInterval(0, 0);
//    playSelectedSong(songTable);
//}
//
//private void playSelectedSong(Song song) {
//    songPlayer.stop();
//    songPlayer.play(song.getFilePath());
//    setupTimers(song.getFilePath());
//    currentSongPosition = songs.indexOf(song);
//    addToRecentSongs(song);
//}
    
    
    
    private void playSongAtIndex(int index, JTable table) {
    if (index < 0 || index >= songs.size()) {
        LOGGER.warning("Invalid song index: " + index);
        return;
    }
    Song song = songs.get(index);
    songPlayer.stop();
    try {
        Thread.sleep(100); // Small delay to ensure resources are released
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
    }
    songPlayer.play(song.getFilePath());
    currentSongPosition = index;
    setupTimers(song.getFilePath());
    addToRecentSongs(song);
    // Select the song in the table and scroll it into view
    int viewRow = table.convertRowIndexToView(index);
    table.setRowSelectionInterval(viewRow, viewRow);
    table.scrollRectToVisible(new Rectangle(table.getCellRect(viewRow, 0, true)));
}

private void playFirstSong() {
    if (!songs.isEmpty()) {
        currentSongPosition = 0;
        songTable.setRowSelectionInterval(0, 0);
        playSongAtIndex(0, songTable);
    }
}

private void playSelectedSong(Song song) {
    songPlayer.stop();
    try {
        Thread.sleep(100); // Small delay to ensure resources are released
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
    }
    songPlayer.play(song.getFilePath());
    setupTimers(song.getFilePath());
    currentSongPosition = songs.indexOf(song);
    addToRecentSongs(song);
}
    
    
//   private void setupTimers(String filePath) {
//    int songDuration = songPlayer.getSongDuration(filePath); // Get the duration of the song
//    progressBar.setMaximum(songDuration); // Set the max value of the progress bar
//
//    if (songTimer != null) {
//        songTimer.stop(); // Stop any existing timers
//    }
//
//    songTimer = new Timer(1000, new ActionListener() { // Timer updates every second
//        int elapsed = 0;
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            elapsed++;
//            progressBar.setValue(elapsed); // Update the progress bar
//            elapsedTimeLabel.setText(formatTime(elapsed)); // Update the elapsed time label
//            remainingTimeLabel.setText(formatTime(songDuration - elapsed)); // Update the remaining time label
//
//            if (elapsed >= songDuration) {
//                songTimer.stop(); // Stop the timer when the song is done
//                progressBar.setValue(0); // Reset the progress bar
//                elapsedTimeLabel.setText("0:00:00");
//                remainingTimeLabel.setText(formatTime(songDuration));
//            }
//        }
//    });
//
//    songTimer.start(); // Start the timer
//}


//private void setupTimers(String filePath) {
//    int songDuration = songPlayer.getSongDuration(filePath); // Get the duration of the song
//    progressBar.setMaximum(songDuration); // Set the max value of the progress bar
//
//    if (songTimer != null) {
//        songTimer.stop(); // Stop any existing timers
//    }
//
//    songTimer = new Timer(1000, new ActionListener() { // Timer updates every second
//        int elapsed = 0;
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            elapsed++;
//            progressBar.setValue(elapsed); // Update the progress bar
//            elapsedTimeLabel.setText(formatTime(elapsed)); // Update the elapsed time label
//            remainingTimeLabel.setText(formatTime(songDuration - elapsed)); // Update the remaining time label
//
//            if (elapsed >= songDuration) {
//                if (repeatEnabled) {
//                    songPlayer.stop();
//                    songPlayer.play(filePath); // Restart the song
//                    setupTimers(filePath); // Reset the timer
//                } else {
//                    songTimer.stop(); // Stop the timer when the song is done
//                    progressBar.setValue(0); // Reset the progress bar
//                    elapsedTimeLabel.setText("0:00:00");
//                    remainingTimeLabel.setText(formatTime(songDuration));
//                }
//            }
//        }
//    });
//
//    songTimer.start(); // Start the timer
//}




//private void setupTimers(String filePath) {
//    int songDuration = songPlayer.getSongDuration(filePath); // Get the duration of the song
//    progressBar.setMaximum(songDuration); // Set the max value of the progress bar
//
//    if (songTimer != null) {
//        songTimer.stop(); // Stop any existing timers
//    }
//
//    songTimer = new Timer(1000, new ActionListener() { // Timer updates every second
//        int elapsed = 0;
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            elapsed++;
//            progressBar.setValue(elapsed); // Update the progress bar
//            elapsedTimeLabel.setText(formatTime(elapsed)); // Update the elapsed time label
//            remainingTimeLabel.setText(formatTime(songDuration - elapsed)); // Update the remaining time label
//
//            if (elapsed >= songDuration) {
//                if (repeatEnabled) {
//                    songPlayer.stop(); // Ensure the current playback is completely stopped
//                    playSongAtIndex(currentSongPosition, songTable); // Restart the song
//                } else {
//                    songTimer.stop(); // Stop the timer when the song is done
//                    progressBar.setValue(0); // Reset the progress bar
//                    elapsedTimeLabel.setText("0:00:00");
//                    remainingTimeLabel.setText(formatTime(songDuration));
//                    if (shuffleEnabled) {
//                        playNextShuffledSong();
//                    }
//                }
//            }
//        }
//    });
//
//    songTimer.start(); // Start the timer
//}

private void setupTimers(String filePath) {
    int songDuration = songPlayer.getSongDuration(filePath);
    progressBar.setMaximum(songDuration);
    if (songTimer != null) {
        songTimer.stop();
    }
    songTimer = new Timer(1000, new ActionListener() {
        int elapsed = 0;
        @Override
        public void actionPerformed(ActionEvent e) {
            if (songPlayer.isPlaying()) {
                elapsed++;
                progressBar.setValue(elapsed);
                elapsedTimeLabel.setText(formatTime(elapsed));
                remainingTimeLabel.setText(formatTime(songDuration - elapsed));
                
                if (elapsed >= songDuration) {
                    handleSongEnd();
                }
            } else {
                handleSongEnd();
            }
        }
    });
    songTimer.start();
}

private void handleSongEnd() {
    songTimer.stop();
    progressBar.setValue(0);
    elapsedTimeLabel.setText("0:00:00");
    remainingTimeLabel.setText(formatTime(songDuration));

    if (repeatEnabled) {
        restartCurrentSong();
    } else if (shuffleEnabled) {
        playNextShuffledSong();
    } else {
        playNextSong();
    }
}

private void restartCurrentSong() {
    playSongAtIndex(currentSongPosition, songTable);
}

private void playNextShuffledSong() {
    if (shuffleEnabled) {
        int randomIndex = new Random().nextInt(songs.size());
        playSongAtIndex(randomIndex, songTable);
    }
}

private void playNextSong() {
    int nextIndex = (currentSongPosition + 1) % songs.size();
    playSongAtIndex(nextIndex, songTable);
}

//private void playNextShuffledSong() {
//    if (songs.size() > 1) {
//        int randomIndex;
//        do {
//            randomIndex = new Random().nextInt(songs.size());
//        } while (randomIndex == currentSongPosition);
//        playSongAtIndex(randomIndex, songTable);
//    } else {
//        restartCurrentSong();
//    }
//}

// Format time in H:MM:SS
private String formatTime(int seconds) {
    int hours = seconds / 3600;
    int minutes = (seconds % 3600) / 60;
    int secs = seconds % 60;
    return String.format("%d:%02d:%02d", hours, minutes, secs);
}

    

    private void selectNextSong() {
        if (songs.size() > 0) {
            if (currentSongPosition < songs.size() - 1) {
                currentSongPosition++;
            } else {
                currentSongPosition = 0; // Wrap around to the first song
            }
            songTable.setRowSelectionInterval(currentSongPosition, currentSongPosition);
        } else {
            JOptionPane.showMessageDialog(this, "No songs in the library.");
        }
    }

    private void selectPreviousSong() {
        if (songs.size() > 0) {
            if (currentSongPosition > 0) {
                currentSongPosition--;
            } else {
                currentSongPosition = songs.size() - 1; // Wrap around to the last song
            }
            songTable.setRowSelectionInterval(currentSongPosition, currentSongPosition);
        } else {
            JOptionPane.showMessageDialog(this, "No songs in the library.");
        }
    }

//   private void playNextSong(JTable table, boolean wrapAround) {
//    if (songs.size() > 0) {
//        songPlayer.stop();
//        if (currentSongPosition < songs.size() - 1) {
//            currentSongPosition++;
//        } else if (wrapAround) {
//            currentSongPosition = 0; // Wrap around to the first song
//        } else {
//            JOptionPane.showMessageDialog(this, "No further song in the library.");
//            return;
//        }
//        int viewRow = table.convertRowIndexToView(currentSongPosition);
//        table.setRowSelectionInterval(viewRow, viewRow);
//        playSelectedSong(table);
//    } else {
//        JOptionPane.showMessageDialog(this, "No songs in the library.");
//    }
//}
//
//private void playPreviousSong(JTable table) {
//    if (songs.size() > 0) {
//        songPlayer.stop();
//        if (currentSongPosition > 0) {
//            currentSongPosition--;
//        } else {
//            currentSongPosition = songs.size() - 1; // Wrap around to the last song
//        }
//        int viewRow = table.convertRowIndexToView(currentSongPosition);
//        table.setRowSelectionInterval(viewRow, viewRow);
//        playSelectedSong(table);
//    } else {
//        JOptionPane.showMessageDialog(this, "No songs in the library.");
//    }
//}
//    
//    private void playNextSong(JTable table, boolean wrapAround) {
//    if (songs.size() > 0) {
//        songPlayer.stop();
//        
//        int viewRow = table.convertRowIndexToView(currentSongPosition);
//        int nextViewRow = (viewRow < table.getRowCount() - 1) ? viewRow + 1 : (wrapAround ? 0 : viewRow);
//        
//        if (nextViewRow != viewRow) { // If the next song is different from the current one
//            int modelRow = table.convertRowIndexToModel(nextViewRow);
//            currentSongPosition = modelRow;
//            table.setRowSelectionInterval(nextViewRow, nextViewRow);
//            playSelectedSong(table);
//        } else if (!wrapAround) {
//            JOptionPane.showMessageDialog(this, "No further song in the library.");
//        }
//    } else {
//        JOptionPane.showMessageDialog(this, "No songs in the library.");
//    }
//}
//
//private void playPreviousSong(JTable table) {
//    if (songs.size() > 0) {
//        songPlayer.stop();
//        
//        int viewRow = table.convertRowIndexToView(currentSongPosition);
//        int previousViewRow = (viewRow > 0) ? viewRow - 1 : table.getRowCount() - 1;
//        
//        if (previousViewRow != viewRow) { // If the previous song is different from the current one
//            int modelRow = table.convertRowIndexToModel(previousViewRow);
//            currentSongPosition = modelRow;
//            table.setRowSelectionInterval(previousViewRow, previousViewRow);
//            playSelectedSong(table);
//        } else {
//            JOptionPane.showMessageDialog(this, "No previous song in the library.");
//        }
//    } else {
//        JOptionPane.showMessageDialog(this, "No songs in the library.");
//    }
//}

//    private void playNextSong(JTable table, boolean wrapAround) {
//    if (shuffleEnabled) {
//        int randomIndex = new Random().nextInt(songs.size());
//        currentSongPosition = randomIndex;
//        table.setRowSelectionInterval(randomIndex, randomIndex);
//        table.scrollRectToVisible(new Rectangle(table.getCellRect(randomIndex, 0, true)));
//        playSelectedSong(table);
//    } else {
//        if (songs.size() > 0) {
//            songPlayer.stop();
//            int viewRow = table.convertRowIndexToView(currentSongPosition);
//            int nextViewRow = (viewRow < table.getRowCount() - 1) ? viewRow + 1 : (wrapAround ? 0 : viewRow);
//
//            if (nextViewRow != viewRow) {
//                int modelRow = table.convertRowIndexToModel(nextViewRow);
//                currentSongPosition = modelRow;
//                table.setRowSelectionInterval(nextViewRow, nextViewRow);
//                playSelectedSong(table);
//            } else if (!wrapAround) {
//                JOptionPane.showMessageDialog(this, "No further song in the library.");
//            }
//        } else {
//            JOptionPane.showMessageDialog(this, "No songs in the library.");
//        }
//    }
//}
//
//private void playPreviousSong(JTable table) {
//    if (shuffleEnabled) {
//        int randomIndex = new Random().nextInt(songs.size());
//        currentSongPosition = randomIndex;
//        table.setRowSelectionInterval(randomIndex, randomIndex);
//        table.scrollRectToVisible(new Rectangle(table.getCellRect(randomIndex, 0, true)));
//        playSelectedSong(table);
//    } else {
//        if (songs.size() > 0) {
//            songPlayer.stop();
//            int viewRow = table.convertRowIndexToView(currentSongPosition);
//            int previousViewRow = (viewRow > 0) ? viewRow - 1 : table.getRowCount() - 1;
//
//            if (previousViewRow != viewRow) {
//                int modelRow = table.convertRowIndexToModel(previousViewRow);
//                currentSongPosition = modelRow;
//                table.setRowSelectionInterval(previousViewRow, previousViewRow);
//                playSelectedSong(table);
//            } else {
//                JOptionPane.showMessageDialog(this, "No previous song in the library.");
//            }
//        } else {
//            JOptionPane.showMessageDialog(this, "No songs in the library.");
//        }
//    }
//}

//    
//    private void playNextSong(JTable table, boolean wrapAround) {
//    if (shuffleEnabled) {
//        int randomIndex = new Random().nextInt(songs.size());
//        playSongAtIndex(randomIndex, table);
//    } else {
//        if (songs.size() > 0) {
//            int nextIndex = (currentSongPosition < songs.size() - 1) ? currentSongPosition + 1 : (wrapAround ? 0 : currentSongPosition);
//            playSongAtIndex(nextIndex, table);
//        } else {
//            JOptionPane.showMessageDialog(this, "No songs in the library.");
//        }
//    }
//}
//
//private void playPreviousSong(JTable table) {
//    if (shuffleEnabled) {
//        int randomIndex = new Random().nextInt(songs.size());
//        playSongAtIndex(randomIndex, table);
//    } else {
//        if (songs.size() > 0) {
//            int previousIndex = (currentSongPosition > 0) ? currentSongPosition - 1 : songs.size() - 1;
//            playSongAtIndex(previousIndex, table);
//        } else {
//            JOptionPane.showMessageDialog(this, "No songs in the library.");
//        }
//    }
//}
    
    private void playNextSong(JTable table, boolean wrapAround) {
    if (shuffleEnabled) {
        playNextShuffledSong();
    } else {
        if (songs.size() > 0) {
            int nextIndex = (currentSongPosition < songs.size() - 1) ? currentSongPosition + 1 : (wrapAround ? 0 : currentSongPosition);
            playSongAtIndex(nextIndex, table);
        } else {
            JOptionPane.showMessageDialog(this, "No songs in the library.");
        }
    }
}

private void playPreviousSong(JTable table) {
    if (shuffleEnabled) {
        playNextShuffledSong();
    } else {
        if (songs.size() > 0) {
            int previousIndex = (currentSongPosition > 0) ? currentSongPosition - 1 : songs.size() - 1;
            playSongAtIndex(previousIndex, table);
        } else {
            JOptionPane.showMessageDialog(this, "No songs in the library.");
        }
    }
}


    

    public Song addSongFromFile(File file) {
        Song song = DatabaseManager.addSong(file, null);  // Pass null for playlistId when adding a song without a specific playlist
        if (song != null) {
            songs = DatabaseManager.getAllSongs();
            loadSongsIntoTable();
            updateTree();
        }
        return song;
    }

    public List<Song> addSongsFromFiles(List<File> files, String playlistName) {
        List<Song> addedSongs = new ArrayList<>();

        for (File file : files) {
            Song existingSong = DatabaseManager.getSongByFilePath(file.getPath());

            if (existingSong != null) {
                // The song already exists in the library, just return the existing song
                addedSongs.add(existingSong);
            } else {
                // The song does not exist, add it to the library and the playlist
                Song newSong = DatabaseManager.addSong(file, null);  // Add to library with no playlist
                if (newSong != null) {
                    addedSongs.add(newSong);
                }
            }
        }

        return addedSongs;
    }

    public List<Song> addSongsFromFiles(List<File> files) {
        return addSongsFromFiles(files, null);
    }

    public void addSong() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 files", "mp3"));
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            addSongFromFile(selectedFile);
        }
    }

    public Song addSongFromFileToPlaylist(File file, String playlistName) {
        int playlistId = DatabaseManager.getPlaylistIdByName(playlistName);
        if (playlistId != -1) {
            Song song = DatabaseManager.addSong(file, playlistId);
            if (song != null) {
                song.setArtist(playlistName);
                song.setPlaylistId(playlistId);

                // Update the artist and playlist ID in the database
                try (Connection conn = DriverManager.getConnection(DatabaseManager.DB_URL);
                     PreparedStatement updateSongStmt = conn.prepareStatement(
                             "UPDATE songs SET artist = ?, playlist_id = ? WHERE id = ?")) {
                    updateSongStmt.setString(1, playlistName);
                    updateSongStmt.setInt(2, playlistId);
                    updateSongStmt.setInt(3, song.getId());
                    updateSongStmt.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("SQL Error: " + e.getMessage());
                    e.printStackTrace();
                }

                // Insert into playlist_songs table
                try (Connection conn = DriverManager.getConnection(DatabaseManager.DB_URL);
                     PreparedStatement insertPlaylistSongStmt = conn.prepareStatement(
                             "INSERT INTO playlist_songs (playlist_id, song_id) VALUES (?, ?)")) {
                    insertPlaylistSongStmt.setInt(1, playlistId);
                    insertPlaylistSongStmt.setInt(2, song.getId());
                    insertPlaylistSongStmt.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("SQL Error: " + e.getMessage());
                    e.printStackTrace();
                }

                songs = DatabaseManager.getAllSongs();
                loadSongsIntoTable();
                updateTree();
            }
            return song;
        } else {
            System.out.println("Playlist not found: " + playlistName);
            return null;
        }
    }

    public void deleteSong() {
        int selectedRow = songTable.getSelectedRow();
        if (selectedRow != -1) {
            Song song = songs.get(selectedRow);
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + song.getTitle() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                DatabaseManager.deleteSong(song.getId());
                songs.remove(selectedRow);
                loadSongsIntoTable();
                updateTree();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a song to delete.");
        }
    }

    public void deleteSongById(int songId) {
        Song song = DatabaseManager.getSongById(songId);
        if (song == null) {
            JOptionPane.showMessageDialog(this, "Song not found.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + song.getTitle() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            DatabaseManager.deleteSong(songId);

            songs.removeIf(s -> s.getId() == songId);
            loadSongsIntoTable();
            updateTree();

            if (playlistWindow != null) {
                playlistWindow.removeSongById(songId);
                playlistWindow.updatePlaylist();
            }
        }
    }

    private void openSong() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 files", "mp3"));
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                Mp3File mp3file = new Mp3File(selectedFile.getPath());
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

                Song tempSong = new Song(-1, title, artist, album, year, genre, comment, selectedFile.getPath());
                songPlayer.play(selectedFile.getPath());
                isPlaying = true;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void createPlaylist() {
        String playlistName = JOptionPane.showInputDialog(this, "Enter playlist name:");
        if (playlistName != null && !playlistName.trim().isEmpty()) {
            DatabaseManager.createPlaylist(playlistName);  // Create the playlist in the database
            updateTree();  // Update the tree to show the new playlist

            // Automatically load and display the new playlist
            int playlistId = DatabaseManager.getPlaylistIdByName(playlistName);
            if (playlistId != -1) {
                loadSongsByPlaylist(playlistId);  // Load the songs in the newly created playlist into the table
            }

            // Expand the "Playlist" node and select the new playlist
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) directoryTree.getModel().getRoot();
            Enumeration<?> enumeration = rootNode.children();
            DefaultMutableTreeNode playlistNode = null;

            // Find the "Playlist" node
            while (enumeration.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
                if (node.getUserObject().toString().equals("Playlist")) {
                    playlistNode = node;
                    break;
                }
            }

            if (playlistNode != null) {
                // Expand the "Playlist" node
                directoryTree.expandPath(new TreePath(playlistNode.getPath()));

                // Find and select the new playlist node
                enumeration = playlistNode.children();
                while (enumeration.hasMoreElements()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
                    if (node.getUserObject().toString().equals(playlistName)) {
                        directoryTree.setSelectionPath(new TreePath(node.getPath()));
                        break;
                    }
                }
            }
        }
    }

    private void deletePlaylist(String playlistName) {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the playlist '" + playlistName + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            DatabaseManager.deletePlaylistAndSongs(playlistName);
            updateTree();
            refreshSongsAndTable();
            JOptionPane.showMessageDialog(this, "Playlist '" + playlistName + "' deleted.");
        }
    }

    private void updateTree() {
        libraryTreeNode = new DefaultMutableTreeNode("Library");
        playlistTreeNode = new DefaultMutableTreeNode("Playlist");

        List<String> playlists = DatabaseManager.getAllPlaylists();
        for (String playlist : playlists) {
            playlistTreeNode.add(new DefaultMutableTreeNode(playlist));
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Root") {
            {
                add(libraryTreeNode);
                add(playlistTreeNode);
            }
        });

        directoryTree.setModel(treeModel);
    }

    private void openInNewWindow(String playlistName) {
        int playlistId = DatabaseManager.getPlaylistIdByName(playlistName);
        if (playlistId != -1) {
            List<Song> playlistSongs = DatabaseManager.getSongsByPlaylistId(playlistId);
            playlistWindow = new PlaylistWindow(playlistName, playlistSongs, songPlayer, this);
            playlistWindow.setVisible(true);
            refreshSongsAndTable();
        } else {
            System.out.println("Playlist ID not found for playlist: " + playlistName);
        }
    }

    public void stopSong() {
    if (songTimer != null) {
        songTimer.stop(); // Stop the timer
    }
    progressBar.setValue(0); // Reset the progress bar
    elapsedTimeLabel.setText("0:00:00");
    remainingTimeLabel.setText(formatTime(songDuration));
    songPlayer.stop();
    isPlaying = false;
}
    
    public void pauseSong() {
    if (songTimer != null) {
        songTimer.stop();
    }
    songPlayer.pause();
    isPlaying = false;
}

    public JTable getSongTable() {
        return songTable;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void updateCurrentTable(JTable table) {
        currentTable = table;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MyTunesUI().setVisible(true);
        });
    }
}
