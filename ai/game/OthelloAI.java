/*******************
 * Christian A. Duncan
 * Edited: Diego Holguin
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import cad.ai.game.*;

/***********************************************************
 * The AI system for a OthelloGame.
 *   Most of the game control is handled by the Server but
 *   the move selection is made here - either via user or an attached
 *   AI system.
 ***********************************************************/
public class OthelloAI extends AbstractAI
 {
    public OthelloGame game;  // The game that this AI system is playing
    public OthelloGame practiceGame; 
    public boolean DEBUG = false; 
    protected Random ran;
    int currentPlayer; 
    private int movesMade;  
    private int maxDepth = 7; 
    
    public OthelloAI()
     {
        System.out.println("DEBUG: Creating AI.");
    	game = null;
    	ran = new Random();
        practiceGame = new OthelloGame(-1, null, null, false, 0);
    }

    public synchronized void attachGame(Game g) 
    {
        System.out.println("DEBUG: Attaching game.");
    	game = (OthelloGame) g;
        movesMade = 2; 
        currentPlayer = game.getPlayer();
    }
    
    /**
     * Returns the Move as a String "rc" (e.g. 2b)
     **/
    public synchronized String computeMove() 
    {
    	if (game == null) 
        {
    	    System.err.println("CODE ERROR: AI is not attached to a game.");
    	    return "0a";
    	}
	
	    char[][] board = (char[][]) game.getStateAsObject();

        // Determine Maximum score among all opponent's options (max of min)
        OthelloGame.Action bestAction = null;

        // Possible actions the AI is able to make, based upon whether it's home or away 
        ArrayList<OthelloGame.Action> actions = game.getActions(currentPlayer);

        // Lowest Integer Value 
        int bestScore = Integer.MIN_VALUE;  

        System.out.println("AI: Moves made=" + movesMade);
        if(movesMade != 0 )
        {
            int randomNum = ThreadLocalRandom.current().nextInt(0, actions.size());
            
            bestAction = actions.get(randomNum); 
            bestScore = -1; 

            movesMade--; 
        }
        else
        {
            // Alpha-Beta Pruning Variables
            int alpha = Integer.MIN_VALUE;
            int beta = Integer.MAX_VALUE;

            // For every actions
            for (OthelloGame.Action a: actions)
            {
                // MiniMax 
                //int score = (currentPlayer == 0) ? minValue(board, a) : -maxValue(board, a); 

                // MiniMax - Alpha-Beta Pruning
                //int score = (currentPlayer == 0) ? minValue(board,a,alpha,beta) : -maxValue(board,a,-beta,-alpha);

                // MiniMax - Depth
                //int score = (currentPlayer == 0) ? minValue(board, a, maxDepth) : -maxValue(board, a, maxDepth);

                // MiniMax - Alpha-Beta Pruning - Depth
                int score = (currentPlayer == 0) ? minValue(board, a, alpha, beta, maxDepth) : -maxValue(board, a, alpha, beta, maxDepth); 
                
                if (score > bestScore) 
                {
                    bestAction = a;
                    bestScore = score;

                    if(bestScore > alpha) alpha = bestScore;
                }
            }
        }

        // First get the list of possible moves
        // Now just pick of them out at random
        System.out.println("Choosing Action:" + bestAction.toString() + ", Score:" + bestScore);
        return bestAction.toString();
    }	

    /*********************************************************************************
     *                              MIN VALUE FUNCTIONS                              *
     *********************************************************************************/

    /******************************************************
    * board  - The current board state
    * action - the action the other player wants to make
    *******************************************************/
    private int minValue(char[][] board, OthelloGame.Action action) 
    { 
        //if(DEBUG) System.out.println("Action is NULL? " + (action == null));

        // Determine if the board is a terminal board, in addition to updating the working board to reflect our clone 
        practiceGame.updateState(1, board);
        if(action != null)
        {
            boolean res = practiceGame.processMove(0,action.row,action.col);
            if (practiceGame.computeWinner()) return practiceGame.getHomeScore() - practiceGame.getAwayScore();
        }

        char[][] newBoard = (char[][]) practiceGame.getStateAsObject();

        // Once again determine the possible actions based upon the other player 
        ArrayList<OthelloGame.Action> actions = practiceGame.getActions(1);
        if(actions.size() == 0) return maxValue(newBoard, null);


        // Determine Maximum value among all possible actions
        int bestScore = Integer.MAX_VALUE; // Positive "Infinity"
        for (OthelloGame.Action a: actions)
        {
            int score = maxValue(newBoard, a);
            if (score < bestScore) bestScore = score;
        }

        return bestScore;
    }

    /******************************************************
    * board  - The current board state
    * action - The action the other player wants to make
    * _depth - The current searchs' instance depth level 
    *******************************************************/
    private int minValue(char[][] board, OthelloGame.Action action, int _depth) 
    {
        int currentDepth = _depth; 
        if(DEBUG) {System.out.println("MIN Value Depth: " + currentDepth); System.out.println("Action is NULL? " + (action == null));}

        // Determine if the board is a terminal board, in addition to updating the working board to reflect our clone 
        practiceGame.updateState(1, board);
        if(action != null)
        {
            boolean res = practiceGame.processMove(0,action.row,action.col);
            if (practiceGame.computeWinner() || currentDepth <= 0) 
            {
                if(_depth == 0 && DEBUG) System.out.println("Ending via depth");
                return practiceGame.getHomeScore() - practiceGame.getAwayScore();
            }
        }

        // Copy the working state of the board for further evaluation 
        char[][] newBoard = (char[][]) practiceGame.getStateAsObject();

        // Once again determine the possible actions based upon the other player 
        ArrayList<OthelloGame.Action> actions = practiceGame.getActions(1);
        if(actions.size() == 0) return maxValue(newBoard, null, currentDepth - 1);


        // Determine Maximum value among all possible actions
        int bestScore = Integer.MAX_VALUE;
        for (OthelloGame.Action a: actions)
        {
            int score = maxValue(newBoard, a, currentDepth); 
            if (score < bestScore) bestScore = score;
        }

        return bestScore;
    }

    /******************************************************
    * board  - The current board state
    * action - The action the other player wants to make
    * _depth - The current searchs' instance depth level 
    *******************************************************/
    private int minValue(char[][] board, OthelloGame.Action action, int _alpha, int _beta) 
    {
        //if(DEBUG) {System.out.println("Alpha: " + _alpha + " Beta: " + _beta);}

        // Determine if the board is a terminal board, in addition to updating the working board to reflect our clone 
        practiceGame.updateState(1, board);
        if(action != null)
        {
            boolean res = practiceGame.processMove(0,action.row,action.col);
            if (practiceGame.computeWinner()) 
            {
                return practiceGame.getHomeScore() - practiceGame.getAwayScore();
            }
        }

        // Copy the working state of the board for further evaluation 
        char[][] newBoard = (char[][]) practiceGame.getStateAsObject();

        // Once again determine the possible actions based upon the other player 
        ArrayList<OthelloGame.Action> actions = practiceGame.getActions(1);
        if(actions.size() == 0) return maxValue(newBoard, null, _alpha, _beta);


        // Determine Maximum value among all possible actions
        int bestScore = Integer.MAX_VALUE;
        for (OthelloGame.Action a: actions)
        {
            int score = maxValue(newBoard, a, _alpha, _beta); 
            if (score < bestScore) 
            {
                bestScore = score;

                if(bestScore <= _alpha) return bestScore; 

                if(bestScore < _beta) _beta = bestScore;
            }
        }

        return bestScore;
    }

    /******************************************************
    * board  - The current board state
    * action - the action the other player wants to make
    * _alpha - Reference to the aplha integer value
    * _beta  - Reference to the beta integer value 
    * _depth - The current searchs' instance depth level 
    *******************************************************/
    private int minValue(char[][] board, OthelloGame.Action action, int _alpha, int _beta, int _depth) 
    {
        int currentDepth = _depth; 
        //if(DEBUG) {System.out.println("MIN Value Depth: " + currentDepth); System.out.println("Action is NULL? " + (action == null)); }

        // Determine if the board is a terminal board, in addition to updating the working board to reflect our clone 
        practiceGame.updateState(1, board);
        if(action != null)
        {
            boolean res = practiceGame.processMove(0,action.row,action.col);
            if (practiceGame.computeWinner() || currentDepth <= 0) 
            {
                //if(_depth == 0 && DEBUG) System.out.println("Ending via depth");
                return practiceGame.getHomeScore() - practiceGame.getAwayScore();
            }
        }

        // Copy the working state of the board for further evaluation 
        char[][] newBoard = (char[][]) practiceGame.getStateAsObject();

        // Once again determine the possible actions based upon the other player 
        ArrayList<OthelloGame.Action> actions = practiceGame.getActions(1);
        if(actions.size() == 0) return maxValue(newBoard, null, _alpha, _beta, currentDepth - 1);

        // Determine Maximum value among all possible actions
        int bestScore = Integer.MAX_VALUE; // Positive "Infinity"
        for (OthelloGame.Action a: actions)
        {
            int score = maxValue(newBoard, a, _alpha, _beta, currentDepth - 1); 
            if (score < bestScore) 
            {
                bestScore = score;

                if(bestScore <= _alpha) return bestScore; 

                if(bestScore < _beta) _beta = bestScore;
            }
        }

        return bestScore;
    }


    /*********************************************************************************
     *                              MAX VALUE FUNCTIONS                              *
     *********************************************************************************/

    private int maxValue(char[][] board, OthelloGame.Action action, int _alpha, int _beta, int _depth) 
    {
        int currentDepth = _depth; 
        //if(DEBUG) {System.out.println("MAX Value Depth: " + currentDepth); System.out.println("Action is NULL? " + (action == null)); }

         // Determine if the board is a terminal board, in addition to updating the working board to reflect our clone 
        practiceGame.updateState(0, board);
        if(action != null)
        {
            boolean res = practiceGame.processMove(1,action.row,action.col);
            if (practiceGame.computeWinner() || currentDepth <= 0) 
            {
                //if(_depth == 0 && DEBUG) System.out.println("Ending via depth");
                return practiceGame.getHomeScore() - practiceGame.getAwayScore();
            }
        }

        // Copy the working state of the board for further evaluation 
        char[][] newBoard = (char[][]) practiceGame.getStateAsObject();

        // Once again determine the possible actions based upon the player, and behind the scenes, our working baord aka clone 
        ArrayList<OthelloGame.Action> actions = practiceGame.getActions(0);
        if(actions.size() == 0) return minValue(newBoard, null, _alpha, _beta, currentDepth - 1);

        // Determine Maximum value among all possible actions
        int bestScore = Integer.MIN_VALUE; // Positive "Infinity"
        for (OthelloGame.Action a: actions)
        {
            int score = minValue(newBoard, a, _alpha, _beta, currentDepth - 1); // Here
            if (score > bestScore)
            {
                bestScore = score;

                if(bestScore >= _beta) return bestScore; 

                if(bestScore > _alpha) _alpha = bestScore;
            }
        }

        return bestScore;
    }

        /******************************************************
    * board  - The current board state
    * action - The action the other player wants to make
    * _depth - The current searchs' instance depth level 
    *******************************************************/
    private int maxValue(char[][] board, OthelloGame.Action action, int _alpha, int _beta) 
    {
        if(DEBUG) { System.out.println("Alpha:  " + _alpha + " Beta: " + _beta); }

         // Determine if the board is a terminal board, in addition to updating the working board to reflect our clone 
        practiceGame.updateState(0, board);
        if(action != null)
        {
            boolean res = practiceGame.processMove(1,action.row,action.col);
            if (practiceGame.computeWinner()) 
            {
                return practiceGame.getHomeScore() - practiceGame.getAwayScore();
            }
        }

        // Copy the working state of the board for further evaluation 
        char[][] newBoard = (char[][]) practiceGame.getStateAsObject();

        // Once again determine the possible actions based upon the player, and behind the scenes, our working baord aka clone 
        ArrayList<OthelloGame.Action> actions = practiceGame.getActions(0);
        if(actions.size() == 0) return minValue(newBoard, null, _alpha, _beta);

        // Determine Maximum value among all possible actions
        int bestScore = Integer.MIN_VALUE; // Positive "Infinity"
        for (OthelloGame.Action a: actions)
        {
            int score = minValue(newBoard, a, _alpha, _beta); // Here
            if (score > bestScore)
            {
                bestScore = score;

                if(bestScore >= _beta) return bestScore; 

                if(bestScore > _alpha) _alpha = bestScore;
            }
        }

        return bestScore;
    }

    /******************************************************
    * board  - The current board state
    * action - The action the other player wants to make
    * _depth - The current searchs' instance depth level 
    *******************************************************/
    private int maxValue(char[][] board, OthelloGame.Action action, int _depth) 
    {
        int currentDepth = _depth; 
        if(DEBUG) { System.out.println("MAX Value Depth: " + currentDepth);System.out.println("Action is NULL? " + (action == null)); }

         // Determine if the board is a terminal board, in addition to updating the working board to reflect our clone 
        practiceGame.updateState(0, board);
        if(action != null)
        {
            boolean res = practiceGame.processMove(1,action.row,action.col);
            if (practiceGame.computeWinner() || currentDepth <= 0) 
            {
                if(_depth == 0 && DEBUG) System.out.println("Ending via depth");
                return practiceGame.getHomeScore() - practiceGame.getAwayScore();
            }
        }

        // Copy the working state of the board for further evaluation 
        char[][] newBoard = (char[][]) practiceGame.getStateAsObject();

        // Once again determine the possible actions based upon the player, and behind the scenes, our working baord aka clone 
        ArrayList<OthelloGame.Action> actions = practiceGame.getActions(0);
        if(actions.size() == 0) return minValue(newBoard, null, currentDepth - 1);

        // Determine Maximum value among all possible actions
        int bestScore = Integer.MIN_VALUE; // Positive "Infinity"
        for (OthelloGame.Action a: actions)
        {
            int score = minValue(newBoard, a, currentDepth - 1); // Here
            if (score > bestScore) bestScore = score;
        }

        return bestScore;
    }

    /******************************************************
    * board  - The current board state
    * action - the action the other player wants to make
    *******************************************************/
    private int maxValue(char[][] board, OthelloGame.Action action) 
    {
        // Determine if the board is a terminal board, in addition to updating the working board to reflect our clone 
        practiceGame.updateState(0, board);
        if(action != null)
        {
            boolean res = practiceGame.processMove(1,action.row,action.col);
            if (practiceGame.computeWinner()) 
            {
                // We have a winner - return its Utility
                return practiceGame.getHomeScore() - practiceGame.getAwayScore();
            }
        }

        // Copy the working state of the board for further evaluation 
        char[][] newBoard = (char[][]) practiceGame.getStateAsObject();

        // Once again determine the possible actions based upon the player, and behind the scenes, our working baord aka clone 
        ArrayList<OthelloGame.Action> actions = practiceGame.getActions(0);
        if(actions.size() == 0) return minValue(newBoard, null);

        // Determine Maximum value among all possible actions
        int bestScore = Integer.MIN_VALUE;
        for (OthelloGame.Action a: actions)
        {
            int score = minValue(newBoard, a); // Here
            if (score > bestScore) bestScore = score;
        }

        return bestScore;
    }

    /**
     * Inform AI who the winner is
     *   result is either (H)ome win, (A)way win, (T)ie
     **/
    @Override
    public synchronized void postWinner(char result) {
	// This AI probably wants to store what it has learned
	// about this particular game.
	game = null;  // No longer playing a game though.
    }

    /**
     * Shutdown the AI - allowing it to save its learned experience
     **/
    @Override
    public synchronized void end() {
	// This AI probably wants to store (in a file) what
	// it has learned from playing all the games so far...
    }
}
