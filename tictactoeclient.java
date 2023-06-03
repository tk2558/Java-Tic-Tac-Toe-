/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/File.java to edit this template
 */

/**
 *
 * @author tkong
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintStream;
import java.net.Socket;
import java.net.InetAddress;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

// CS 3913 Project (Client Code)
public class tictactoeclient extends JFrame implements Runnable {
    final private JPanel displayPanel; // panel to hold board
    final private JPanel boardPanel; // panel for tic-tac-toe board
    final private JTextArea msgs; // JTextArea to display msgs from server to client
    final private Space board[][]; // tic-tac-toe board
    private Space curSp; // current space
    
    public int portNum = 5190; // port number for Server, can be changed in the future
    private Socket sock; // connection to server
    private Scanner sin; // input from server
    private PrintStream sout; // output to server
    
    final private String localHost = "127.0.0.1"; // local host
    private String mySymbol; // this client's symbol
    private boolean myTurn; // determines which client's turn it is
    
    private final String X_Symbol = "X"; // symbol for first client who connected
    private final String O_Symbol = "O"; // symbol for second client who connected


    public tictactoeclient() {
        msgs = new JTextArea(3, 15); // set up JTextArea
        msgs.setEditable(false); // area is not editable (only recieve msg from server)
        add(new JScrollPane(msgs), BorderLayout.EAST); // set up in EAST side

        boardPanel = new JPanel(); // set up panel for spaces in board
        boardPanel.setLayout(new GridLayout(3, 3)); // TIC-TAC-TOE IS 3 BY 3 GRID
        board = new Space[3][3]; // board = 3x3 spaces

        for (int row = 0; row < board.length; row++) { // loop over rows
            for (int column = 0; column < board[row].length; column++) { // loop over columns
                board[row][column] = new Space("", row * 3 + column); // create empty space
                boardPanel.add(board[row][column]); // add space to panel
            }
        }

        displayPanel = new JPanel(); // set display panel to contain boardPanel
        displayPanel.add(boardPanel, BorderLayout.CENTER); // add board panel
        //displayPanel.setBackground(Color.black);
        add(displayPanel, BorderLayout.CENTER); // panels all combined
        
        setSize(425, 300); // set size of window
        setVisible(true); // set visibility
        clientStart(); // start the client
    }

    public static void main( String args[] ) {
        tictactoeclient application = new tictactoeclient(); // start client
        application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE ); // set to end when frame closes
    }
    
    private void clientStart() {
        try {
            sock = new Socket(InetAddress.getByName(localHost), portNum); // connect to server
            sin = new Scanner(sock.getInputStream()); // stream for input
            sout = new PrintStream(sock.getOutputStream()); // stream for output
        }
        catch (Exception ex) {} // catch exception
        
        ExecutorService worker = Executors.newFixedThreadPool(1); // create and start thread for client
        worker.execute( this ); // execute client
    }
    

    @Override
    public void run() {
        mySymbol = sin.nextLine(); // get player's symbol (X or O)
        setTitle("Welcome, Player " + mySymbol + "!"); // Set title of JFRame
        myTurn = mySymbol.equals(X_Symbol); // determine if client's turn

        while (true) { // ALWAYS READY TO GET MSG FROM SERVER
            if (sin.hasNextLine()) { getMsg(sin.nextLine()); } // keep getting msgs from server
        } 
    } 

    private void getMsg(String msg) {
        switch (msg) {
            case "Valid" -> {
                // message validated move
                setSymbol(curSp, mySymbol); // set symbol in space
                displayMsg("Opponent's turn...\n"); // oppponent may proceed
            }
            case "Invalid" -> {
                // message invalidated move
                myTurn = true; // still this client's turn
                displayMsg("Try again...\n"); // player needs to pick another space
            }
            case "Opponent moved" ->                 {
                    // message indicates player's turn now
                    int loc = sin.nextInt(); // get move location
                    sin.nextLine(); // skip newline after int location
                    int row = loc / 3; // get row
                    int column = loc % 3; // get column
                    setSymbol(board[row][column], mySymbol.equals(X_Symbol) ? O_Symbol : X_Symbol); // set symbol move
                    myTurn = true; // now this client's turn
                    displayMsg("Your turn...\n"); // display msg
                }
            case "Opponent Won" ->                 {
                    // message indicates game is over and they LOST
                    int loc = sin.nextInt(); // get move location
                    sin.nextLine(); // skip newline after int location
                    int row = loc / 3; // get row
                    int column = loc % 3; // get column
                    setSymbol(board[row][column], mySymbol.equals(X_Symbol) ? O_Symbol : X_Symbol); // set symbol move
                    displayMsg("Opponent Won!\n"); // display msg
                }
            default -> displayMsg(msg + "\n"); // display the msg
        }
    }
    
    // send msg to server indicating clicked space
    public void sendClickedSpace(int loc) {
        if (myTurn) { // if it is player's turn
            sout.format( "%d\n", loc); // send location to server
            sout.flush(); // flush output
            myTurn = false; // not player's turn anymore
        }
    }

    private void displayMsg(final String msg)  {
        msgs.append(msg); // updates msg output
    }

    private void setSymbol(final Space sp, final String symbol) {
        sp.setSymbol(symbol); // set symbol in space
    }

    public void setCurSp(Space sp) { // set current Space
        curSp = sp;
    }


    private class Space extends JPanel {
        private String symbol; // symbol to be drawn
        final private int loc; // location of space

        public Space(String spSymbol, int spLoc) { // initalize space
            symbol = spSymbol; // set symbol for this space
            loc = spLoc; // set location of this space

            addMouseListener(new MouseAdapter() { // MOUSE CLICK ACTION LISTENER
            @Override
            public void mouseReleased(MouseEvent e) {
                setCurSp(Space.this); // set current space to keep track
                sendClickedSpace(getSpaceLoc()); // get location of this space
                } 
            }); 
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);  // drawing
            g.drawRect( 0, 0, 74, 74 ); // draw space
            g.setFont(new Font("Segoe Script", Font.BOLD, 75)); // set font of symbol
            if (symbol.equals("X")) { // If player symbol is X...
                g.setColor(Color.green); // set color as green
            }
            else { // If player symbol is O...
                g.setColor(Color.red); // set color as red
            }
            g.drawString( symbol, 8, 60 ); // draw symbol
        }
        
        public void setSymbol( String newSymbol ) {
            symbol = newSymbol; // set symbol of space
            repaint(); // repaint space
        }
        
        public int getSpaceLoc() {
            return loc; // return location of space
        }
        
        @Override
        public Dimension getPreferredSize() {
            return new Dimension( 75, 75); // set preferred size of space
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize(); // set minimum size of space
        }
    }
}
