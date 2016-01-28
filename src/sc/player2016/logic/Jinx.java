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
	
	private static final int DEPTH = 4;
	
	private static boolean isFirstMove = true;
	
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
//		System.out.println("*** Es wurde ein Zug angefordert");
//		System.out.println("Round = " + gameState.getRound());
//		System.out.println("Last move x = " + gameState.getLastMove().getX() + 
//				"  y = " + gameState.getLastMove().getY());
//		System.out.println("Turn = " + gameState.getTurn());
		
		System.out.println("Round " + gameState.getRound() + "  ----------------------");
		
                
//                //test insert move
//                ArrayList<Field> l = new ArrayList<Field>();
//                ArrayList<Float> l2 = new ArrayList<Float>();
//                insertMoveIn(l, l2, new Field(10,0) , 10);
//                insertMoveIn(l, l2, new Field(5,0) , 5);
//                insertMoveIn(l, l2, new Field(8,0) , 8);
//                insertMoveIn(l, l2, new Field(2,0) , 2);
//                insertMoveIn(l, l2, new Field(70,0) , 70);
//                System.out.println("List: " + l);
		Move selection;

		if(gameState.getTurn() == 0){
			jinxIsPlayingVertical = true;
			
			//initialize board
			board = new Board(gameState);
			selection = getFirstMoveOfGameJep();
		}else{
			if(gameState.getTurn() == 1){
				jinxIsPlayingVertical = false;
				
				//initialize board
				Move firstMove = gameState.getLastMove();
				board = new Board(gameState);
				board.getField(firstMove.getX(), firstMove.getY()).setFieldColor(FieldColor.OPPONENT);
			}
			
			//update board (add move (and sometimes connection) by opponent)
			board.updateBoard(board.getField(gameState.getLastMove().getX(), gameState.getLastMove().getY()), false);
//			List<Move> possibleMoves = gameState.getPossibleMoves();
			
//			System.out.println("*** sende zug: ");
//			selection = possibleMoves.get(rand.nextInt(possibleMoves
//					.size()));
//			System.out.println("*** setze Strommast auf x="
//					+ selection.getX() + ", y="
//					+ selection.getY());
//			Field nextMove = calcBestMove(board.getField(gameState.getLastMove().getX(), gameState.getLastMove().getY()), lastMoveByJinx, DEPTH);
                        Field nextMove = calcBestMoveIterative(board.getField(gameState.getLastMove().getX(), gameState.getLastMove().getY()),lastMoveByJinx, 1500);
			selection = new Move(nextMove.getX(), nextMove.getY());
		}

		//update board (add move (and sometimes connection) by jinx)
		board.updateBoard(board.getField(selection.getX(), selection.getY()), true);
				
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
		ArrayList<Field> possibleMoves = board.getPossibleMoves(lastMove, secondLastMove);
		
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
                    
                    System.out.println("Depth " + depth);
                    
                    int i=0;
                    for(Field move : possibleMoves){
                        i++;
			board.updateBoard(move, true);
			float value = min(depth-1, maxValue, Float.MAX_VALUE, move, lastMove, depth);
			board.undoMove(move, true);
			if(value > maxValue){
                            maxValue = value;
                            result = move;
                            System.out.println("----------1. Move " + move + " with max = " + value + " ---------");
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
                            System.out.println("\nDepth = " + depth + "  " + i + "/" + possibleMoves.size() + " moves");
                            break;
                        }
                    }
                    //possibleMoves are now resorted for faster search with depth++
                    //This sort hopefully leads to much more cutoffs in the alpha-beta-search
                    possibleMoves = cloneList(sortedMoves);
                    
                    
                    System.out.println("Possible moves: " + possibleMoves);
                    
                    System.out.println("Best move: " + result + " with " + maxValue);
                    System.out.println("--------end of depth = " + depth + " ------");
                    System.out.println();
                    
                    
                }//end of 'increase depth by 1 loop'    
                System.out.println("Zeit: " + (System.currentTimeMillis() - startTime)/(float)1000);
                System.out.println("Number of cutoffs = " + numberOfCutoffs);
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
		ArrayList<Field> possibleMoves = board.getPossibleMoves(lastMove, secondLastMove);
		float maxValue = alpha; //minimum that jinx can reach (found in previous nodes)
		for(Field move : possibleMoves){
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
		ArrayList<Field> possibleMoves = board.getPossibleMoves(lastMove, secondLastMove);
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
	//the positions of the green fields (maxNumberOfGreenFields = 3x3 + 2x2 + 2x2 + 1 = 18)
	private static Move getFirstMoveOfGame(){
		Move result = null;
		
		//in jinx 0.01 the first move will be on a field (x,y) with 10 <= x <= 14
		// and 10 <= y <= 14 .
		for(int row = 10; row<=14 && result == null; row++){
			for(int col=10; col<=14 && result == null; col++){
				if(board.getField(row, col).getFieldColor() == FieldColor.BLACK)
					result = new Move(row, col);
			}
		}
		
		return result;
	}
	
        private static Move getFirstMoveOfGameJep(){
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

                int [] horiz = {8, 15};
                int [] vertic = {5, 18};

                switch (gameState.getStartPlayerColor()) {
                    case BLUE : 
                        //do nothing default;
                        break;
                    case RED :
                        horiz[0] = 5; 
                        horiz[1] = 18;
                        vertic[0] = 8;
                        vertic[1] = 15;
                        break;
                }  

                firstMoves1.addAll(gameState.getPossibleMoves());

                System.out.println("Debug1");

        //Level 1
                // tests if Move is in Field of Interest(the Middel)
                for (int i = (firstMoves1.size() - 1); i >= 0; i--) {
                    if (firstMoves1.get(i).getX() >= horiz[0] && horiz[1] >= firstMoves1.get(i).getX()){
                        if (firstMoves1.get(i).getY() >= vertic[0] && vertic[1] >= firstMoves1.get(i).getY()) {
                            firstMoves2.add(firstMoves1.get(i));
                        }
                    }
                System.out.println(i);
                }
        // Level 2    
                System.out.println("Debug2");

                int [][] neighborhood = {
                    {0, -1}, {1, -1}, {1,0}, {1,1}, {0,1}, {-1,1}, {-1, 0}, {-1,-1}
                };

                for (int i = (firstMoves2.size() -1) ; i >= 0;  i--) { 
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
                System.out.println("Debug3");


                Move result = firstMoves3.get(rand.nextInt(firstMoves3.size()));;

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
	
}




