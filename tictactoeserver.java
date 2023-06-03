/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/File.java to edit this template
 */


import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 *
 * @author tkong
 */

// CS 3913 Project (Server Code)
public class tictactoeserver {
    private String[] board = new String[9]; // tic-tac-toe board consists of 9 spaces
    private Player[] players; // array of Players (MAX LIMIT OF 2)
    private int currPlayer; // track current player's turn
    
    private ServerSocket server; // server socket to connect clients
    private ExecutorService runGame; // game is ready for players
    
    private Lock gameLock; // lock game to wait for specific conditions to be matched...
    private Condition opponentConnected; // wait for "opponent" to connect condition
    private Condition opponentTurn; // wait for opponent's turn to finish condition
    
    private final static String[] Symbols = {"X", "O"}; //  Two Symbols: X or O
    private final static int PLAYER_X = 0; // Player 0 will be X & Player 1 will be O
    
    public int portNum = 5190; // port number for Server, can be changed in the future
    public boolean gameEnd = false; // Game state (ongoing = false or end = true)
    public static String winner = ""; // prepare a Winner (after game ends)

    // set up tic-tac-toe server
    public tictactoeserver() {
        runGame = Executors.newFixedThreadPool(2); // MAX TWO PLAYERS/THREADS
        gameLock = new ReentrantLock(); // create lock for game

        opponentConnected = gameLock.newCondition(); // check both players connected
        opponentTurn = gameLock.newCondition(); // check player's turn
        
        players = new Player[2]; // create array of size 2 for 2 players
        currPlayer = PLAYER_X; // set first connected player to first player (X)
        for (int i = 0; i < 9; i++) { // create tic-tac-toe board
            board[i] = ""; // set each board space as empty string variable
        }
        
        try { server = new ServerSocket(portNum, 2); } // set up ServerSocket in port 5190 for 2 clients
        catch (Exception ex) {} // catch exception
        
        System.out.println("Server waiting for Players..."); // SERVER WAIT MSG
    }
    
    public static void main(String args[]) { // start tictactoe.java
        tictactoeserver application = new tictactoeserver(); // set server up
        application.start(); // start server 
    }

    public void start() {
        for (int i = 0; i < players.length; i++) { // wait for each client to connect
            try {
                players[i] = new Player(server.accept(), i); // create Player
                runGame.execute(players[i]); // start runnable
            } catch (Exception ex) {} // catch exception
        }

        gameLock.lock(); // lock game

        try {
            players[PLAYER_X].setSus(false); // resume player turn
            opponentConnected.signal(); // wake up current player's thread
        } 
        finally { gameLock.unlock(); } // unlock game after all players arrive
    }

    public boolean checkMove(int loc, int player) { // parameters = (location of s being placed, current player)
        while (player != currPlayer) { // opposing player must wait for turn
            gameLock.lock(); // lock game to wait for other player to go
            
            try { opponentTurn.await(); } // wait for player's turn
            catch (Exception ex) {} // catch exception
            finally { gameLock.unlock(); } // unlock game after waiting
        }
      
        if (emptySpace(loc)) { // if location empty, its a valid move
            board[loc] = Symbols[currPlayer]; // place symbol on the board
            currPlayer = (currPlayer + 1) % 2; // switch player turn
            players[currPlayer].opponentMoved(loc);  // copy movement onto opponent board
            gameLock.lock(); // lock game
            
            try { opponentTurn.signal(); } // signal other player to continue
            finally { gameLock.unlock(); }// unlock game after signaling
            return true; // true = valid move
        }
        else { return false; } // false = invalid move
    }
    
    public boolean winCondition() { // CHECK IF WE HAVE A WINNER
        // True = We Have a Winner or game is tied, False = Game ongoing
        // ALL POSSIBLE WIN CONDITIONS
        if (board[0].equals(board[1]) && board[0].equals(board[2]) && !board[0].equals("")) { // STRAIGHT LINE IN FIRST ROW
            winner = board[1];
            return true;
        }
        else if(board[3].equals(board[4]) && board[3].equals(board[5]) && !board[3].equals("")){ // STRAIGHT LINE IN SECOND ROW
            winner = board[3];
            return true;
        }
        else if(board[6].equals(board[7]) && board[6].equals(board[8]) && !board[6].equals("")){ // STRAIGHT LINE IN THIRD ROW
            winner = board[6];
            return true;
        }
        else if(board[0].equals(board[3]) && board[0].equals(board[6]) && !board[0].equals("")){ // STRAIGHT LINE DOWN FIRST COLUMN
            winner = board[0];
            return true;
        }
        else if(board[1].equals(board[4]) && board[1].equals(board[7]) && !board[1].equals("")){ // STRAIGHT LINE DOWN SECOND COLUMN
            winner = board[1];
            return true;
        }
        else if(board[2].equals(board[5]) && board[2].equals(board[8]) && !board[2].equals("")){ // STRAIGHT LINE DOWN THIRD COLUMN
            winner = board[2];
            return true;
        }
        else if(board[0].equals(board[4]) && board[4].equals(board[8]) && !board[0].equals("")){ // LEFT TO RIGHT DIAGONAL LINE
            winner = board[0];
            return true;
        }
        else if(board[2].equals(board[4]) && board[4].equals(board[6]) && !board[2].equals("")){ // RIGHT TO LEFT DIAGONAL LINE
            winner = board[2];
            return true;
        }
        
        if (!stillSpace()) { // NOT A WIN CONDITION BUT AN END CONDITION
            winner = "Nobody!\nITS A TIE"; // ALL SPACES USED UP WITH NO WINNER
            return true;
        }
        return false; // No one won yet
    }
    
    public boolean emptySpace(int loc) {
        return (board[loc].equals("")); // check if space is empty
    }
    
    public boolean stillSpace() {
        for (int i = 0; i < 9; i++) { // check each of the 9 board space
            if (emptySpace(i)) { // if any is empty game still ongoing
                return true; 
            }
        }
        return false; // if all space filled up with X or O, game is over and no Winner
    }
    
    public boolean GameOver() {
        return gameEnd; // False = game ongoing, True = game end
    }

    
    private class Player implements Runnable {
        private Socket conn; // connection to client
        private Scanner sin; // input from client
        private PrintStream sout; // output to client
        
        private int playerNum; // Player Number
        private String s; // Player's corresponding symbol
        private boolean sus = true; // check if thread is suspended

        // initalize Player thread
        public Player(Socket sock, int num) {
            playerNum = num; // set player's number
            s = Symbols[playerNum]; // get corresponding player s
            conn = sock; // store socket for client
            
            try { // get inputs and outputs from Socket
                sin = new Scanner(conn.getInputStream()); 
                sout = new PrintStream(conn.getOutputStream());
            }
            catch (Exception ex) {} // catch excpetion
        }
        
        @Override
        public void run() {
            try {
                System.out.println("Player " + s + " is Here!"); // SUCCESSFUL CONNECTION MSG
                sout.format(s + "\n"); // inform player of their s
                sout.flush(); // flush output 
                
                if (playerNum == PLAYER_X) { // if only person connected so far
                    sout.format("Player X Connected!\nWaiting...\n"); // send wait msg
                    sout.flush(); // flush output 
                    gameLock.lock(); // lock game to wait for second player
                    
                    try  {
                        while(sus) { // keep waiting
                            opponentConnected.await(); // only free when player O to connect
                        } 
                    }
                    catch (Exception ex) {} // catch exception
                    finally {
                        gameLock.unlock(); // unlock game after opponent connects
                    }
                    
                    sout.format("Opponent Connected!\nYour move...\n"); // inform that opponent player connected
                    sout.flush(); // flush output 
                }
                
                else { // Second player has connected
                    sout.format("Player O Connected!\nPlease wait...\n"); // ready to start, player 1 goes first
                    sout.flush(); // flush output 
                }

                while (!GameOver()) { // while game ongoing
                    int loc = 0; // initialize location
                    if (sin.hasNext()) { // the next input recieved is
                        loc = sin.nextInt(); // the location of the symbol
                    }

                    // check if it was a valid move made by player
                    if (checkMove(loc, playerNum)) { // move was valid
                        sout.format("Valid\n"); // notify client
                        sout.flush(); // flush output 
                    }
                    
                    else {// move was invalid
                        sout.format("Invalid\n"); // notify client
                        sout.flush(); // flush output 
                    }
                    
                    // check if that was the winning move (invalid move cant ever result in wins)
                    if (winCondition()) { // if it was the winning move
                       sout.format("Winner is " + winner + "!\n"); // notify client
                       sout.flush(); // flush output 
                       gameEnd = true; // game ended
                    } 
                }
            }
            finally {
                try { // Clients are free to disconnet, server is FREE
                    System.out.println("Winner is " + winner + "!\n"); // server celebrates winner too!
                    conn.close(); // close connection to client 
                    server.close(); // close server
                }
                catch (Exception ex) {} // catch exception
            }
        } 
        
        public void opponentMoved(int loc) {
            if (winCondition()) { // check if opponent won already
                sout.format("Opponent Won\n"); // notify Opponent finished their turn
                sout.format("%d\n", loc); // send location of new s
                sout.flush(); // flush output 
                
                sout.format("Winner is " + winner + "!\n"); // Notify client of winner
                sout.flush(); // flush output 
                gameEnd = true; // Game has ended
            }
            else { // game ongoing
                sout.format("Opponent moved\n");  // notify Opponent finished their turn
                sout.format(loc + "\n"); // send location of new s
                sout.flush(); // flush output 
            }
        }
       
        public void setSus(boolean status) {
            sus = status; // set whether or not thread is suspended
        } 
    }
}



