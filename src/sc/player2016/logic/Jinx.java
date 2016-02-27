package sc.player2016.logic;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sc.plugin2016.GameState;
import sc.plugin2016.Move;

public class Jinx {
	
	public static boolean jinxIsPlayingVertical;
	
	//Hopefully not necessary in final version...
	private static final Random rand = new SecureRandom();
	
	private static final int TIMELIMIT = 1500;

	private static Board board;
	
	private static Field nextMoveByJinx;
	
	//lastMoveByJinx = secondLastMove
	//Needed in board.getPossibleMoves().
	//Updated in findMove()
	private static Field lastMoveByJinx = null;
	
	enum FieldColor{
		BLACK, GREEN, JINX, OPPONENT
	}
	
	/*Generates the best move, Jinx1 is able to find.
	 * 
	 * */
	public static Move findMove(GameState gameState){
            
            System.out.println("Round " + gameState.getRound() + "  ----------------------");

            Move selection = null;
              
            if(gameState.getTurn() == 0){
                jinxIsPlayingVertical = true;
                Evaluator.setFactor();
                
                //initialize board
                board = new Board(gameState);
                selection = getFirstMove();
            }else{
                if(gameState.getTurn() == 1){
                    jinxIsPlayingVertical = false;
                    Evaluator.setFactor();
                    
                    //initialize board
                    board = new Board(gameState);
                    
                    //don't just set field color (also set start/end field of opponent graph)
                    board.updateBoard(board.getField(gameState.getLastMove().getX(), gameState.getLastMove().getY()), false);
                    
                    selection = getSecondMove(gameState);
                    System.out.println("SECOND MOVE: "+ selection.getX() + ", " + selection.getY());
                }else{
                    //update board (add move (and sometimes connection or start/end field) by opponent)
                    board.updateBoard(board.getField(gameState.getLastMove().getX(), gameState.getLastMove().getY()), false);
                    
                    
                    System.out.println("evaluationOfConflictzone 1 = " +
                        board.evaluateBoardPosition());
            
                    
                    Field nextMove = calcBestMoveIterative(board.getField(gameState.getLastMove().getX(), gameState.getLastMove().getY()),lastMoveByJinx, TIMELIMIT);
                    selection = new Move(nextMove.getX(), nextMove.getY());
                }
            }
            
            System.out.println("CONFLICTZONEVALUE: " + board.evaluateConflict());
            
            //update board (add move (and sometimes connection) by jinx)
            board.updateBoard(board.getField(selection.getX(), selection.getY()), true);
                 
            
            System.out.println(board.getNumberOfSetFields() + " stones set");
            System.out.println("graphsByJinx = " + board.graphsByJinx + 
                    "\ngraphsByOpponent = " + board.graphsByOpponent);
            
            if(gameState.getTurn() > 1){
                System.out.println("evaluationOfBoardPosition = " +
                        board.evaluateBoardPosition());
                    
                if(jinxIsPlayingVertical){
                    System.out.println("evaluationOfConflictzone = " +
                            Evaluator.evaluateCurrentConflictzone(
                                    board.graphsByJinx,
                                    board.graphsByOpponent, board.isJinxTurn));
                    System.out.println("startJinx = " + board.graphsByJinx.get(0).getMinYField() + "  endJinx = " + board.graphsByJinx.get(0).getMaxYField());	
                    System.out.println("startOpponent = " + board.graphsByOpponent.get(0).getMinXField() + "  endOpponent = " + board.graphsByOpponent.get(0).getMaxXField());
                }else{
                    System.out.println("evaluationOfConflictzone = " +
                            Evaluator.evaluateCurrentConflictzone(
                                    board.graphsByOpponent,
                                    board.graphsByJinx, !board.isJinxTurn));
                    System.out.println("startJinx = " + board.graphsByJinx.get(0).getMinXField() + "  endJinx = " + board.graphsByJinx.get(0).getMaxXField());	
                    System.out.println("startOpponent = " + board.graphsByOpponent.get(0).getMinYField() + "  endOpponent = " + board.graphsByOpponent.get(0).getMaxYField());
                }
                System.out.println("pointsJinx = " + board.graphsByJinx.get(0).getPoints(Jinx.jinxIsPlayingVertical)
                    + "  pointsOpponent = " + board.graphsByOpponent.get(0).getPoints(!Jinx.jinxIsPlayingVertical));
            
            }
            
            System.out.println("CONFLICTZONEVALUE: " + board.evaluateConflict());
            lastMoveByJinx = board.getField(selection.getX(), selection.getY());
            return selection;
	}
	
	private static int numberOfCutoffs;
        private static long startTime;
        private static int timeToSearchInMS;
        private static boolean timeIsOver;
	private static Field calcBestMoveIterative(Field lastMove, Field secondLastMove, int timeToSearch){
		startTime = System.currentTimeMillis();
                timeToSearchInMS = timeToSearch;
                timeIsOver = false;
                
                Field result = null;
                numberOfCutoffs = 0;
		ArrayList<Field> preselectedMoves = board.preselectMoves(lastMove, jinxIsPlayingVertical);
                
                ArrayList<Field> sortedMoves = new ArrayList<Field>();
                ArrayList<Float> valuesOfSortedMoves = new ArrayList<>();
                
                float maxValue;
		
                for(int depth=1; !timeIsOver; depth++){
                    sortedMoves.clear();
                    valuesOfSortedMoves.clear();
                    
                    //reset maxValue 
                    //e. g. depth 2 always has smaller/equal values than depth 1,
                    //because it is the opponents turn (look at the evaluation-function of Board).
                    //(result can stay the same, because the sortedMoves are ordered
                    //by value; that means, if the search stops because of time,
                    //the current depth is probably not finished yet, but the
                    //first moves of sortedMoves are already inspected. The first moves were the best of the calculation with depth-1,
                    //so if there are better moves at the end of sortedMoves, Jinx
                    //would not have played them after the search results of depth-1)
                    maxValue = Integer.MIN_VALUE;
                    
//                    System.out.println("Depth " + depth);
                    
                    int i=0;
                    for(Field move : preselectedMoves){
                        i++;
			board.updateBoard(move, true);
			float value = min(depth-1, maxValue, Float.MAX_VALUE, move, lastMove, depth);
			board.undoMove(move, true);
			if(value > maxValue){
                            maxValue = value;
                            result = move;
//                            System.out.println("----------1. Move " + move + " with max = " + value + " ---------");
			}
                        //Insert the inspected/evaluated move at the right position
                        //(the smaller the value the higher the list index =>
                        //the better the move is at the currently searched depth the 
                        //erlier it will be inspected in the next iteration (with 
                        //depth increased by 1))
                        insertMoveIn(sortedMoves, valuesOfSortedMoves, move, value);
//                        if(System.currentTimeMillis()-startTime > timeToSearchInMS){
//                             timeIsOver = true;
//                             System.out.println("\nDepth = " + depth + "  " + i + "/" + possibleMoves.size() + " moves");
//                             break;
//                        }
                        if(timeIsOver){
                            System.out.println("\nDepth = " + depth + "  " + i + "/" + preselectedMoves.size() + " moves");
                            break;
                        }
                    }
                    //possibleMoves are now resorted for faster search with depth++
                    //This sort hopefully leads to much more cutoffs in the alpha-beta-search
                    preselectedMoves = cloneList(sortedMoves);
                    
                    
//                    System.out.println("Possible moves: " + preselectedMoves);
//                    
//                    System.out.println("Best move: " + result + " with " + maxValue);
//                    System.out.println("--------end of depth = " + depth + " ------");
//                    System.out.println();
                    
                    
                }//end of 'increase depth by 1 loop'    
//                System.out.println("Zeit: " + (System.currentTimeMillis() - startTime)/(float)1000);
//                System.out.println("Number of cutoffs = " + numberOfCutoffs);
                //timeIsOver => return best move found
                return result;
	}
	
	private static void insertMoveIn(ArrayList<Field> sortedMoves, ArrayList<Float> valuesOfSortedMoves, Field move, float value){
            int i=0;
            while(i<valuesOfSortedMoves.size() && valuesOfSortedMoves.get(i) >= value){
                i++;
            }
            sortedMoves.add(i, move);
            valuesOfSortedMoves.add(i, value);
        }
	
	//needs last Move of the opponent. Calculates next move reacting to olastMove.
	private static Field calcBestMove(Field lastMove, Field secondLastMove, int depth){
		long startTime = System.currentTimeMillis();
                numberOfCutoffs = 0;
		float evaluation = max(depth, -100000, 100000, lastMove, secondLastMove, depth);
		System.out.println("Time = " + (System.currentTimeMillis() - startTime)/(float)1000 + " ");
                System.out.println("Number of cutoffs = " + numberOfCutoffs);
		return nextMoveByJinx;
		
	}
	
	private static float max(int depth, float alpha, float beta, Field lastMove, Field secondLastMove, int depthAtStart){
//		System.out.println("max mit tiefe = " + depth);
		if(depth == 0){
			return board.evaluateBoardPosition();
		}else if(depth == depthAtStart-1){
			System.out.print(".");
		}
		ArrayList<Field> preselectedMoves = board.preselectMoves(lastMove, jinxIsPlayingVertical);
		float maxValue = alpha; //minimum that jinx can reach (found in previous nodes)
		for(Field move : preselectedMoves){
			board.updateBoard(move, true);
			float value = min(depth-1, maxValue, beta, move, lastMove, depthAtStart);
			board.undoMove(move, true);
			if(value > maxValue){
				maxValue = value;
				if(maxValue >= beta){ //opponent would never allow this move (previous combination
                                    numberOfCutoffs++;// searched, that had a better (lower) result for opponent)
                                    break;			
                                }
					
//				if(depth == depthAtStart){
//					nextMoveByJinx = move;
//                                        System.out.println("----------1. Move " + move + " with max = " + value + " ---------");
//				}else if(depth == depthAtStart-1){
//                                        System.out.println("-----2. Move " + move + " with max = " + value + " ----");
//                                }else if(depth == depthAtStart-2){
//                                        System.out.println("3. Move " + move + " with max = " + value);
//                                }
			}
                        if(depth >= depthAtStart-3 && System.currentTimeMillis()-startTime > timeToSearchInMS){
                            timeIsOver = true;
                            //not all possibleMoves were searched yet,
                            //so do not include this max() call results
                            //in the final move decision
                            return Integer.MAX_VALUE;
                        }
		}
		return maxValue;
	}
	
	private static float min(int depth, float alpha, float beta, Field lastMove, Field secondLastMove, int depthAtStart){
		
//		System.out.println("min mit tiefe = " + depth);
		if(depth == 0){
			return board.evaluateBoardPosition();
		}else if(depth == depthAtStart-1){
//			System.out.print(".");
		}
		ArrayList<Field> possibleMoves = board.preselectMoves(lastMove, !jinxIsPlayingVertical);
		float minValue = beta; //maximum that beta can reach (found in previous nodes)
                Field minMove = null;
		for(Field move : possibleMoves){
			board.updateBoard(move, false);
			float value = max(depth-1,alpha, minValue, move, lastMove, depthAtStart);
			board.undoMove(move, false);
			if(value < minValue){
				minValue = value;
                                minMove = move;
				if(minValue <= alpha){//jinx would never allow this move (previous combination
                                    numberOfCutoffs++;// searched, that had a better (higher) result for jinx)
                                    break;			
                                }
//                                if(depth == depthAtStart){
//                                        System.out.println("----------1. Move " + move + " with min = " + value + " ---------");
//				}else if(depth == depthAtStart-1){
//                                        System.out.println("-----2. Move " + move + " with min = " + value + " ----");
//                                }else if(depth == depthAtStart-2){
//                                        System.out.println("3. Move " + move + " with min = " + value);
//                                }
			}
//                        if(depth == depthAtStart-1 && move.getX() == 19 ){
//                            System.out.println("Move " + move + " has " + value);
//                        }
                        if(depth >= depthAtStart-3 && System.currentTimeMillis()-startTime > timeToSearchInMS){
                            timeIsOver = true;
                            //not all possibleMoves were searched yet,
                            //so do not include this min() call results
                            //in the final move decision
                            return Integer.MIN_VALUE;
                        }
		}
//                if(depth == depthAtStart-1){
//                    System.out.println("Best min move after " + lastMove + ": " + minMove + " mit " + minValue);
//                }
		return minValue;
	}
	
        //Gets first move of the game (is just needed, when Jinx begins) depending on 
	//the positions of the swamps (maxNumberOfSwampFields = 3x3 + 2x2 + 2x2 + 1 = 18)
        private static Move getFirstMove(){
		boolean isSwampInColumn;
                int startColOfLongestDistance = 1;
                int endColOfLongestDistance = 1;
                int startColOfCurrentDistance = 1;
                
                //Find column with maximum possible horizontal distance to next
                //swamp field (x coordinate of result)
                for(int col=1; col < 23; col++){
                    isSwampInColumn = false;
                    for(int row=1;row<23; row++){
                        if(board.getField(col, row).getFieldColor() == FieldColor.GREEN){
                            isSwampInColumn = true;
                            break;
                        }
                    }
                    if(isSwampInColumn){
                        if(endColOfLongestDistance-startColOfLongestDistance < (col-1) - startColOfCurrentDistance){
                            startColOfLongestDistance = startColOfCurrentDistance;
                            endColOfLongestDistance = col-1;
                        }
                        startColOfCurrentDistance = col+1;
                    }
                }
                //if the biggest corridor is until col 22, than this if is necessary
                if(endColOfLongestDistance-startColOfLongestDistance < 22 - startColOfCurrentDistance){
                            startColOfLongestDistance = startColOfCurrentDistance;
                            endColOfLongestDistance = 22;
                }
                
                int row = 11;//or 12; mabye also depending on swamp 
                             //locations somehow
                int col;
                if((endColOfLongestDistance - startColOfLongestDistance) % 2 == 0){
                    col = startColOfLongestDistance + (endColOfLongestDistance - startColOfLongestDistance)/2;
                }else{
                    //get the (of two, either midOfCorridor-0.5 or midOfCorridor+0.5) 
                    //mid field, that is nearer to the center (11.5)
                    float midOfCorridor = startColOfLongestDistance + (endColOfLongestDistance -startColOfLongestDistance)/(float)2;
                    
                    if(Math.abs((int)midOfCorridor - 11.5) <= Math.abs((int)midOfCorridor+1 - 11.5)){
                        col = (int) midOfCorridor;
                    }else{
                        col = (int) midOfCorridor+1;
                    }
                }
                
		return new Move(col, row);
	}
	
        private static Move getFirstMoveOfGameGK(GameState gameState) {
                ArrayList<Move> firstMoves1 = new ArrayList();
                ArrayList<Move> firstMoves2 = new ArrayList();
                ArrayList<Move> firstMoves3 = new ArrayList();
                ArrayList<Move> firstMoves4 = new ArrayList();

                int [] horiz = {8, 15};
                int [] vertic = {5, 18};

                switch (gameState.getStartPlayerColor()) {
                    case BLUE : 
                        //do nothing default;
                        break;
                    case RED :
                        horiz[0] = 1; 
                        horiz[1] = 22;
                        vertic[0] = 8;
                        vertic[1] = 15;
                        break;
                }  

                firstMoves1.addAll(gameState.getPossibleMoves());

                System.out.println("Debug1");

        //Level 1
                // tests if Move is in Field of Interest(the Middel)
                for (int i = 0; i < firstMoves1.size(); i++) {
                    if (firstMoves1.get(i).getX() >= horiz[0] && horiz[1] >= firstMoves1.get(i).getX()){
                        if (firstMoves1.get(i).getY() >= vertic[0] && vertic[1] >= firstMoves1.get(i).getY()) {
                            firstMoves2.add(firstMoves1.get(i));
                        }
                    }
                //System.out.println(i);
                }
        // Level 2    
            //eliminate Moves with swamps in dirct neighborhood
                //System.out.println(firstMoves1);
                System.out.println("Debug2");

                int [][] neighborhood = {
                    {0, -1}, {1, -1}, {1,0}, {1,1}, {0,1}, {-1,1}, {-1, 0}, {-1,-1}
                };

                for (int i = 0; i < firstMoves2.size(); i++) { 
                    boolean swampNear = false;
                    for (int j = (neighborhood.length -1); j >= 0; j--) {

                        int mx = firstMoves2.get(i).getX();
                        int my = firstMoves2.get(i).getY(); 

                        mx = mx + neighborhood[j][0];
                        my = my + neighborhood[j][1];

                        //Field occuField = new Field(mx, my);

                        //occuField.assignType(gameState);

                        if (board.getField(mx, my).getFieldColor() == Jinx.FieldColor.GREEN){
                            swampNear = true;
                        }

                    } 
                    if (!swampNear){
                        firstMoves3.add(firstMoves2.get(i));
                    }

                } 
        //Level 3
            //find cooridors between swamps and get best Row for the MoveY
                 System.out.println("Debug3");
                 
                //gets all Y-Rows where no  swamps are
                ArrayList<Integer> noSwampY = new ArrayList();
                boolean swampRow;
                
                for (int i = 0; i <= 23; i++){
                    swampRow = false;
                    for (int j = 0; j <= 23; j++){
                        //System.out.println(i + "," + j);
                        if (swampRow == false){
                            if (board.getField(i, j).getFieldColor() == Jinx.FieldColor.BLACK && j == 23){
                                if (!(noSwampY.contains(j))){
                                    noSwampY.add(i);
                                }
                            } else if (board.getField(i, j).getFieldColor() == Jinx.FieldColor.GREEN) {
                            swampRow = true;
                            }
                        }
                    }
                }
                System.out.println("noSwampRows:" + noSwampY);
                
                 System.out.println("Debug4");
                 
                //find  cooridors
                ArrayList<Integer[]> cooridors = new ArrayList(); 
                boolean stillCooridor = false; //flase == sseking for new ; true == have one
                int start = 0; 
                
                for (int i = 1; i <= 23; i++){
                    //System.out.println(i);
                    if (noSwampY.contains(i) && stillCooridor == false){
                        start = i;
                        stillCooridor = true;
                    } else if (stillCooridor == true && !noSwampY.contains(i) || i == 23 ){
                        int helper = i - 1;
                        Integer [] a = {start, helper};
                        cooridors.add(a);
                        stillCooridor = false;
                    }
                }
                
                System.out.print("SwampFreeCooridors:");
                for (Integer[] outer : cooridors) {
                    for(Integer inner : outer) {
                      System.out.print(inner + ",");
                    }
                  }
                System.out.println("");
                
                //find largest Cooridor
                int indexOfLargestCor = 1000;
                int size = 0;
                
                for (int i = 0; i < cooridors.size(); i++){
                    System.out.println("i: " + i);
                    if (size < (cooridors.get(i)[1] - cooridors.get(i)[0])){
                        size = cooridors.get(i)[1] - cooridors.get(i)[0];
                        indexOfLargestCor = i;
                    }
                }
                
                System.out.println("indexOfLargestCor: "  + indexOfLargestCor);
                
                //get right rowY
                int center = (int) Math.floor((cooridors.get(indexOfLargestCor)[1] - cooridors.get(indexOfLargestCor)[0]) / 2);
                int rowX;
                rowX = cooridors.get(indexOfLargestCor)[0] + center;
                
                System.out.println("rowY: " + rowX);
                
                //get all Moves with the right Y of the preselectet
                for (int i = 0; i < firstMoves3.size(); i++){
                    if (firstMoves3.get(i).getX() == rowX ){
                        firstMoves4.add(firstMoves3.get(i));
                        System.out.println(i);
                    }
                }
                
        //Level 4     
                System.out.println("Debug5");


               // Move result = firstMoves3.get(rand.nextInt(firstMoves3.size()));;

                Move result = firstMoves4.get(0);
                
                System.out.println("FirstMove: " + result.getX() + "," +  result.getY());
               
                return result;
        }
        
        private static Move getSecondMove (GameState gameState) {
            Move result = null;
            int rowX = gameState.getLastMove().getX();
                
                //Level 3
            //find cooridors between swamps and get best Row for the MoveY
         
                //gets all Y-Rows where no  swamps are
                ArrayList<Integer> noSwampY = new ArrayList();
                boolean swampRow;
                
                
                String bal = board.getField(7, 8).getFieldColor().toString();
                
                
                for (int j = 1; j <= 22; j++){
                    swampRow = false;
                    for (int i = 1; i <= 22; i++){
                        if (swampRow == false){
                            if ((board.getField(i, j).getFieldColor() == Jinx.FieldColor.BLACK || board.getField(i, j).getFieldColor() == Jinx.FieldColor.OPPONENT) && i == 22){
                                if (!(noSwampY.contains(j))){
                                    noSwampY.add(j);
                                  }
                            } else if (board.getField(i, j).getFieldColor() == Jinx.FieldColor.GREEN) {
                            swampRow = true;
                            }
                        }
                    }
                }
                System.out.println("noSwampRows:" + noSwampY);
                
              //   System.out.println("Debug4");
                 
                //find  cooridors
                ArrayList<Integer[]> cooridors = new ArrayList(); 
                boolean stillCooridor = false; //flase == sseking for new ; true == have one
                int start = 0; 
                
                for (int i = 1; i <= 22; i++){
                    //System.out.println(i);
                    if (noSwampY.contains(i) && stillCooridor == false){
                        start = i;
                        stillCooridor = true;
                    } else if (stillCooridor == true && !noSwampY.contains(i)){
                        int helper = i - 1;
                        Integer [] a = {start, helper};
                        cooridors.add(a);
                        stillCooridor = false;
                    }
                }
                
                if (stillCooridor) {
                        Integer [] a = {start, 21};
                        cooridors.add(a);
                        stillCooridor = false;
                }
                
                System.out.print("SwampFreeCooridors:");
                for (Integer[] outer : cooridors) {
                    for(Integer inner : outer) {
                      System.out.print(inner + ",");
                    }
                  }
                System.out.println("");
                
                //find largest Cooridor
                int indexOfLargestCor = 1000;
                int size = 0;
                int indexOfSecondLargestCor = 1000;
                
                for (int i = 0; i < cooridors.size(); i++){
                    //System.out.println("i: " + i);
                    if (size <= (cooridors.get(i)[1] - cooridors.get(i)[0])){
                        size = cooridors.get(i)[1] - cooridors.get(i)[0];
                        indexOfSecondLargestCor = indexOfLargestCor;
                        indexOfLargestCor = i;
                    }
                }
                
                System.out.println("indexOfLargestCor: "  + indexOfLargestCor);
                
                //get right rowY
                float center = ((cooridors.get(indexOfLargestCor)[1] - cooridors.get(indexOfLargestCor)[0]) / 2);
                    
                System.out.println(center);
                
                float rowY;
                rowY = cooridors.get(indexOfLargestCor)[0] + center;
                
                
                
                if (rowY < 12 ) {
                    rowY = (int) (rowY + 0.5);
                }else if(rowY > 12){
                    rowY = (int) Math.floor(rowY);
                }
                
                System.out.println("rowY: " + rowY);
                            
                result = getSecondMoveSub(indexOfLargestCor, gameState, cooridors, (int) rowY);

                 if (result == null) {
                     result = getSecondMoveSub(indexOfSecondLargestCor, gameState, cooridors, (int) rowY);
                 }

            return result;
        }
        
	private static void printPossibleMoves(List<Move> possibleMoves){
		System.out.println("Possible Moves:");
		int i=1;
		for(Move move : possibleMoves){
			System.out.println(i + ": " + "x=" + move.getX() + ", y=" + move.getY());
			i++;
		}
		System.out.println();
	}
        
        private static ArrayList<Field> cloneList(ArrayList<Field> list) {
            ArrayList<Field> clone = new ArrayList<Field>(list.size());
            for(Field item: list) {
                clone.add(item);
            }
            return clone;
        }
        
        private static Move getSecondMoveSub (int corIndex, GameState gameState, ArrayList<Integer[]> cooridors, int rowY){
            boolean movefound = false;
                    
                ArrayList<Move> findMostMiddle = new ArrayList();
                
                 for (int i = 0; i < gameState.getPossibleMoves().size() && !movefound; i++) {
                     //System.out.println("lastloop" + i);
                        //if in largest kooridor
                        if (gameState.getPossibleMoves().get(i).getY() <= cooridors.get(corIndex)[1] && gameState.getPossibleMoves().get(i).getY() >= cooridors.get(corIndex)[0]) {
                       //     System.out.println("con 1: " + gameState.getPossibleMoves().get(i).getX() + "," + gameState.getPossibleMoves().get(i).getY());
                            //if in rowX of first Move
                            if (gameState.getPossibleMoves().get(i).getX() == gameState.getLastMove().getX()) {
                           //     System.out.println("con 2");
                                //if +4 || -4 far away on X
                                if ((gameState.getPossibleMoves().get(i).getY() >= (gameState.getLastMove().getY() + 4)) || (gameState.getPossibleMoves().get(i).getY() <= (gameState.getLastMove().getY() - 4))){
                         //           System.out.println("con 3: " + gameState.getPossibleMoves().get(i).getX() + "," + gameState.getPossibleMoves().get(i).getY());
                                    findMostMiddle.add(new Move(gameState.getPossibleMoves().get(i).getX() , gameState.getPossibleMoves().get(i).getY()));
                                    
                                    //movefound = true;
                                }
                            }
                        }
                    }
             int best = 1000; 
             int diffOfBest = 1000;   
             int diffOcc = 1000;
             
             for (int i = 0; i < findMostMiddle.size(); i++ ){
                 //System.out.println("diff of best: "+i + "CorrdsY: "+ findMostMiddle.get(i).getY());
                 diffOcc = (int) Math.abs(rowY - findMostMiddle.get(i).getY());
                 System.out.println(diffOcc);
                 if (diffOcc < diffOfBest) {
                     diffOfBest = diffOcc;
                   //  System.out.println(findMostMiddle.get(i).getX()+ "," +findMostMiddle.get(i).getY()+ ",diffOfBest:  " +diffOfBest);
                     best = i;
                 }
             }
             
            Move  result = null;
             
            if(best != 1000){   
                 result = new Move(findMostMiddle.get(best).getX(), findMostMiddle.get(best).getY());
             }
            return result;
        }
	
}





