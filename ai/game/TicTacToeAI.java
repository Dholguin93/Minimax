/*******************
 * Author :Christian A. Duncan
 * Modified : Diego Holguin
 * CSC350: Intelligent Systems
 * Spring 2017
 *
 * AI Game Client
 * This project is designed to link to a basic Game Server to test
 * AI-based solutions.
 * See README file for more details.
 ********************/

package cad.ai.game;

import java.util.Random;
import java.util.*;
import java.io.File;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.*;

/***********************************************************
 * The AI system for a TicTacToeGame.
 *   Most of the game control is handled by the Server but
 *   the move selection is made here - either via user or an attached
 *   AI system.
 ***********************************************************/
public class TicTacToeAI extends AbstractAI {
    public TicTacToeGame game;  // The game that this AI system is playing
    Random ran; // Random Seed

    boolean isAIHome, useMemory, saveMem; // AI Identifers 
    String saveAIByType; // The File ID to save/write with 
    LinkedList<String> GameStates = new LinkedList<String>(); // List of Game States for Tic Tac Toe 

    // Maps GameStates to the GameOutcomes
    // Think : For every move that the AI takes, it would be helpful to know if this is a good, bad, or neutral move.. this data is within the GameOutcomes Object
    Hashtable<String, GameOutcomes> AI_Memory = new Hashtable<String, GameOutcomes>(); 

    // GameOutcomes Object 
    public class GameOutcomes
    {
        // Instance variable that contains the data needed to determine the ratio of this "Move" being a winning, losing, or tieing move  
    	public int numberOfWins, numberOfLosses, numberOfTies, numberOfGames;

        public GameOutcomes()
        {
            this.numberOfWins = 0;
            this.numberOfLosses = 0;
            this.numberOfTies = 0;
            this.numberOfGames = 0;
        }

        public void AIHasWon() { this.numberOfWins ++; this.numberOfGames ++; }
        public void AIHasLost() { this.numberOfLosses ++; this.numberOfGames ++; }
        public void AIHasTied() { this.numberOfTies ++; this.numberOfGames ++; }
        public int GetTotalGames() { return this.numberOfGames; }
        public void SetOutcome(int numWins, int numLoses, int numTies) { this.numberOfWins = numWins; this.numberOfLosses = numLoses; this.numberOfTies = numTies; this.numberOfGames = (numWins + numLoses + numTies);}
        public double GetUtailityScore() { return (((double)this.numberOfWins - (double)this.numberOfLosses) / ((double)this.numberOfGames)); }
    }

    
    public TicTacToeAI() {
	game = null;
	ran = new Random();
    }

    public TicTacToeAI(boolean home) {
	game = null;
	isAIHome = home;
	ran = new Random();
    }

    public TicTacToeAI(boolean home, boolean _useMem, boolean _saveMem, String fileType) 
    {
        // Basic stuff 
        game = null;
        isAIHome = home;
        ran = new Random();

        saveMem = _saveMem; 

        // Use memory only when flag is set to true 
        useMemory = _useMem; 

        // Save/Load based upon type of AI -- Home, Away, of Master
        saveAIByType = fileType; 

        // Indicate whether we're using memory for the AI 
        useMemory = _useMem; 
        if(_useMem) readMemoryFromFile(); 
    }

    public synchronized void attachGame(Game g) {
	game = (TicTacToeGame) g;
    }
    
    /**
     * Returns the Move as a String "R,S"
     *    R=Row
     *    S=Sticks to take from that row
     **/
    public synchronized String computeMove() 
    {
        // Error Handling 
	    if (game == null) 
        {
            System.err.println("CODE ERROR: AI is not attached to a game.");
            return "0,0";
	    }
	
        // Obtain the state of the board  
	    char[] board = (char[]) game.getStateAsObject();

        // Initalize a StringBuffer to contain the state of the board, with a visual identifier for spaces!
        StringBuffer savedState = new StringBuffer();

        // Append each character of the board state into a StringBuffer
        // The '_" character represents an empty space
        for(int i = 0; i < board.length; i++)
        {
            if(board[i] != ' ')
            {
                savedState.append(board[i]);
            }
            else if (board[i] == ' ')
            {
                savedState.append('_');
            }
        }

        // Add this GameState to the LinkedList if it isn't a repeated move 
        if(!GameStates.contains(savedState.toString())) GameStates.add(savedState.toString());

        int pos; 
        if(useMemory)
        {
            
            // Get the best move and identify the index where the AI will select to append a character at 
            char[] bestBoardMove = getBestMove(savedState.toString()).toCharArray(); 
            char[] savedCharArray = savedState.toString().toCharArray();
            boolean keepLooking = true; for(pos = 0; pos < savedCharArray.length && keepLooking; pos++)
            {
                if(bestBoardMove[pos] != savedCharArray[pos]) { keepLooking = false; pos--;}
            }
        }
        else
        {
            // First see how many open slots there are
            int openSlots = 0;
            int i = 0;
            for (i = 0; i < board.length; i++)
                if (board[i] == ' ') openSlots++;

            // Now pick a random open slot
            int s = ran.nextInt(openSlots);

            // And get the proper row
            i = 0;
            while (s >= 0) {
                if (board[i] == ' ') s--;  // One more open slot down
                i++;
            }

            // The position to use is the previous position
            pos = i - 1;
         }

    return "" + pos;
    }	

    /**
     * Inform AI who the winner is
     *   result is either (H)ome win, (A)way win, (T)ie
     **/
    @Override
    public synchronized void postWinner(char result) 
    {
        /**
         * Get the player's number (0=Home, 1=Away)
         *  -1 is returned if this is the Server version which is not attached to a specific player.
         **/
        int ai_HomeAway = game.getPlayer();

        // Work our way from the very first move to the very last move that the AI has taken throughout the current instance of the game
        for(int i = 0; i < GameStates.size(); i++)
        {
            // Get each corressponding move 
            String _gamestate = GameStates.get(i);

            // Check if this move is a "new" move for the AI, and if so , add it to the AI's memory 
            if(!AI_Memory.containsKey(_gamestate))
            {
                // Initialzie the new GameOutcomes Object for the "new" move 
                GameOutcomes newOutcome = new GameOutcomes();
                //System.out.println("Creating New GameOutcomes For The GameState : " + _gamestate);
                // If the away player has won ... 
                if(result == 'A')
                {
                    // and the AI is away, then the AI has won!
                    if(ai_HomeAway == 1) newOutcome.AIHasWon(); // If the AI won, increment the number of wins for the GameOutcomes Object of the "new" move 

                    // and the AI is home, then the AI has lost!
                    if(ai_HomeAway == 0) newOutcome.AIHasLost();  // if the AI lost, increment the number of losses for the GameOutcomes Object of the "new" move
                }
                // If the home player has won ...
                else if (result == 'H')
                {
                    // and the AI is home, then the AI has won!
                    if(ai_HomeAway == 0) newOutcome.AIHasWon(); // If the AI won, increment the number of wins for the GameOutcomes Object of the "new" move 

                    // and the AI is away, then the AI has lost!
                    if(ai_HomeAway == 1) newOutcome.AIHasLost();  // if the AI lost, increment the number of losses for the GameOutcomes Object of the "new" move
                }
                // If the AI tied, increment the number of ties for the GameOutcomes Object of the "new" move
                else if (result == 'T') newOutcome.AIHasTied(); // If the AI tied, increment the number of ties for the GameOutcomes Object of the "new" move 

                // Now add this "new" move to the AI's memory 
                AI_Memory.put(_gamestate, newOutcome);
             }
            // If this wasn't a "new" move for the AI, just update the "move" GameOutcomes Objects' value to reflect whether the AI has won, lost, or tied 
            else if (AI_Memory.containsKey(_gamestate))
            {
                // Get the GameOutcomes Object associated with the move 
                GameOutcomes runtime_outcome = AI_Memory.get(_gamestate);

                // If the away player has won ... 
                if(result == 'A')
                {
                    // and the AI is away, then the AI has won!
                    if(ai_HomeAway == 1) runtime_outcome.AIHasWon(); // If the AI won, increment the number of wins for the GameOutcomes Object of the "new" move 

                    // and the AI is home, then the AI has lost!
                    if(ai_HomeAway == 0) runtime_outcome.AIHasLost();  // if the AI lost, increment the number of losses for the GameOutcomes Object of the "new" move
                }
                // If the home player has won ...
                else if (result == 'H')
                {
                    // and the AI is home, then the AI has won!
                    if(ai_HomeAway == 0) runtime_outcome.AIHasWon(); // If the AI won, increment the number of wins for the GameOutcomes Object of the "new" move 

                    // and the AI is away, then the AI has lost!
                    if(ai_HomeAway == 1) runtime_outcome.AIHasLost();  // if the AI lost, increment the number of losses for the GameOutcomes Object of the "new" move
                }
                // If the AI tied, increment the number of ties for the GameOutcomes Object of the "new" move
                else if (result == 'T') runtime_outcome.AIHasTied(); // If the AI tied, increment the number of ties for the GameOutcomes Object of the "new" move 
            }
        }

        // No longer playing a game though.
    	game = null;  
    }

    /**
     * Shutdown the AI - allowing it to save its learned experience
     **/
    @Override
    public synchronized void end() 
    {
        // Save contents only when AI is specified to do so 
        if(saveMem)
        {
            // Initialize the string that will store the board state 
            StringBuffer BoardState_And_Outcome = new StringBuffer();

            // For every Gamestate tallied thus far, add the board state, and the win,loss, and tie outcomes associated with that GameState 
            for(int i = 0; i < GameStates.size(); i ++)
            {
                // Add the board state first
                BoardState_And_Outcome.append(GameStates.get(i));

                // Add a seperator 
                BoardState_And_Outcome.append(" ");

                // Retrieve the GameOutcomes object ossiated to the board state we're evaluting at the moment 
                GameOutcomes curOutcome = AI_Memory.get(GameStates.get(i));

                // Add the board state's number of Wins, losses, and ties 
                BoardState_And_Outcome.append("W: " + curOutcome.numberOfWins + ", L: " + curOutcome.numberOfLosses + ", T: " + curOutcome.numberOfTies + "\n");
            }

            // Write the experiences learnt by the AI to it's respective file 
            writeMemoryToFile(BoardState_And_Outcome.toString());
        }

    }

    public void writeMemoryToFile(String fileContents)
    {
        // Obtain absolute path to the cad directory 
        String currentDirectory = Paths.get("cad/").toAbsolutePath().normalize().toString();

        File file = new File(currentDirectory + "/AIMemory" + saveAIByType + ".txt");
        try
        {
            // Create the file if it doesn't already exist 
            if(!file.exists()) file.createNewFile();

            // true = append file
            Writer writer = new BufferedWriter(new FileWriter(currentDirectory + "/AIMemory" + saveAIByType + ".txt", false));
            writer.write(fileContents);
            writer.close(); 
        }
        catch (IOException exception)
        {
            System.err.println("[AI-ERROR] : When Writing File");
        }
    }


    public void readMemoryFromFile()
    {
        String currentDirectory = Paths.get("cad/").toAbsolutePath().normalize().toString();
        File file = new File(currentDirectory + "/AIMemory" + saveAIByType + ".txt");

        // Check that the file even exists.. Error Handling 
        if(!file.exists()) { /*System.out.println("No AI Memory File Active");*/ }
        else
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(currentDirectory + "/AIMemory" + saveAIByType + ".txt"));
                String lineRead = ""; while( (lineRead = reader.readLine()) != null)
                {

                    // Obtain the Index of the wins, losses, and ties for each line 
                    int winIndex = lineRead.indexOf('W');
                    int lossIndex = lineRead.indexOf('L');
                    int tiesIndex = lineRead.indexOf('T');

                    // Parse the game state for each line into it's respective value 
                    String gameState = lineRead.substring(0,winIndex-1);

                    // Parse the win, losses, and tie for each line into their respective integer values 
                    int winTally = Integer.parseInt(lineRead.substring(winIndex + 3, winIndex + 4));
                    int lossTally = Integer.parseInt(lineRead.substring(lossIndex + 3, lossIndex + 4));
                    int tieTally = Integer.parseInt(lineRead.substring(tiesIndex + 3, tiesIndex + 4));

                    // Instantiate and save the loaded number of wins, losses, and ties for the board state to the programs respective container 
                    GameOutcomes outcomeToSave = new GameOutcomes();
                    outcomeToSave.SetOutcome(winTally, lossTally, tieTally);

                    // Save the contents of the gameState to the programs respective container 
                    GameStates.add(gameState);
                    AI_Memory.put(gameState, outcomeToSave);
                }
            }
            catch (IOException exception)
            {
                System.err.println("[AI-ERROR] : When Reading File");
            }
        }
    }

    public LinkedList<String> returnPossibleMoves(String currentBState, int isHome)
    {
        // Instiantiate a container that holds all of the possible moves that can be made within the current state 
        LinkedList<String> possibleMoves = new LinkedList<String>();
        StringBuffer moveString = new StringBuffer(); 
        char[] stringToCharArray = currentBState.toCharArray();

        // For Every possible move out there .. 
        for(int i = 0; i < stringToCharArray.length; i++)
        {
            char[] manipulatedString = currentBState.toCharArray();  
            if(stringToCharArray[i] == '_')
            {
                // Manipulate based upon whether the AI is home or away 
                if(isHome == 0) manipulatedString[i] = 'X';
                else if (isHome == 1) manipulatedString[i] = '0';

                // Add the possible moves to the linkedlist 
                possibleMoves.add(new String(manipulatedString));
            }
        }

        // Return all of the possible moves 
        return possibleMoves;
    }

    public String getBestMove(String currentBState)
    {
        // Return all valid moves that the AI is able to take 
        LinkedList<String> movesForAI = returnPossibleMoves(currentBState, game.getPlayer());  


        // Check if the current game state is a "new" board state or a one we can use our reinforcement learning to solve!
        if(AI_Memory.get(currentBState) == null) { /* System.out.println("AI Has Never Seen This Game Before"); */ } 
        else nG = AI_Memory.get(currentBState).GetTotalGames(); 

        double heat = 10.0 * Math.pow(0.9, (nG/(movesForAI.size() + 1 * 1000.0)));

        // Variable holding the board with the highest score value 
        // check for null 
        String highestBoardScore = movesForAI.get(0); 
        double highScore = getUtaility(highestBoardScore)+ ran.nextDouble() * heat; 

        // Iterate through all of the possible moves ... 
        for(int i = 0; i < movesForAI.size(); i++)
        {
            // Obtain the score for each possible move
            double utailityScoreOfMove = getUtaility(movesForAI.get(i)) + ran.nextDouble() * heat;

            // Store it only if it is the highest score value for the board 
            if(highScore < utailityScoreOfMove) { highestBoardScore = movesForAI.get(i); highScore = utailityScoreOfMove; }
        }

        // Return the board with the highest score 
        return highestBoardScore; 
    }

    public double getUtaility(String board)
    {
        GameOutcomes g = AI_Memory.get(board); 

        // For new boards, we'll have an "open" mind when it comes to exploration!  
        // Note : GetUtailityScore by defualt, returns a value between 0 - 1
        if(g == null ) return 1.5; 
        else  return AI_Memory.get(board).GetUtailityScore();
    }
}
