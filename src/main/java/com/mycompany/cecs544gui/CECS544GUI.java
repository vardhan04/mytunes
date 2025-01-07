/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.cecs544gui;

import javax.swing.SwingUtilities;

public class CECS544GUI {
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();
        
        // Ensuring the GUI runs on Event Dispatch
        SwingUtilities.invokeLater(() -> {
            // Displaying the application window
            MyTunesUI myTunesUI = new MyTunesUI();
            myTunesUI.setVisible(true);
        });
    }
}

