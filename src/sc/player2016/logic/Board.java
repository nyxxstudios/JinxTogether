package sc.player2016.logic;

import java.util.ArrayList;
import java.util.List;

import sc.player2016.logic.Jinx.FieldColor;
import sc.plugin2016.GameState;
import sc.plugin2016.Move;

public class Board {
    Field[][] fields = new Field[24][24];

    int pointsByJinx = 0;
    int pointsByOpponent = 0;

    //save start and end field of the 'max graphs' (graphs that are worth 
    //the most points) of jinx and the opponent. Set in updateMove/undoMove.
    //Read in evaluateBoard
    Field startOfJinxGraph;
    Field endOfJinxGraph;
    Field startOfOpponentGraph;
    Field endOfOpponentGraph;

    //needed in pointsWithField as temporary storage. If points have changed,
    //then also change the start/end fields of the graph (above) with the help of
    //these variables
    private Field startJinxField, endJinxField, startOpponentField, endOpponentField;


    public Board(GameState gameStateAtBeginning){
            initFields(gameStateAtBeginning);
    }

    public Field getField(int x, int y){
            return fields[x] [y];
    }

    public void updateBoard(Field move, boolean isJinxMove){
            FieldColor fieldColor;
            boolean isPlayingVertical;

            if(isJinxMove){
                    fieldColor = FieldColor.JINX;
                    isPlayingVertical = Jinx.jinxIsPlayingVertical;
            }else{
                    fieldColor = FieldColor.OPPONENT; 
                    isPlayingVertical = !Jinx.jinxIsPlayingVertical;
            }

            int x = move.getX();
            int y = move.getY();

            //setFieldColor
            getField(x, y).setFieldColor(fieldColor);

            //check for new connections--------------------------
            final int[][] possibleFields = {
                            {-2, -1}, {-2, 1},
                            {-1, -2}, {-1, 2},
                            { 1, -2}, { 1, 2},
                            { 2, -1}, { 2, 1}
            };
            for(int[] f : possibleFields){
                    int pX = x+f[0];
                    int pY = y+f[1];

                    //if 'possible-Connection-Field' is a real field (not out of the board)
                    if(pX >= 0 && pX < 24 && pY >= 0 && pY < 24){

                            //if 'possible-Connection-Field' has fieldColor
                            if(getField(pX, pY).getFieldColor() == fieldColor){

                                    //Check for connection of opponent (FieldColor other)
                                    if(connectionIsPossibleBetween(new int[]{x,y}, new int[]{pX, pY})){

                                            //add connection to both fields
                                            getField(x, y).addConnectionTo(getField(pX, pY));
                                            getField(pX, pY).addConnectionTo(getField(x, y));

                                            int pointsWithThisField = pointsWithField(getField(x, y), isPlayingVertical);
                                            if (isJinxMove){
                                                    if(pointsByJinx < pointsWithThisField){
                                                            pointsByJinx = pointsWithThisField;
                                                            startOfJinxGraph = startJinxField;
                                                            endOfJinxGraph = endJinxField;
//								System.out.println("New score for jinx = " + pointsByJinx);
//                                                            System.out.println("startJinx = " + startOfJinxGraph + "  endJinx = " + endOfJinxGraph + " cause of updateMove");
                                                    }
                                            }else{
                                                    if(pointsByOpponent < pointsWithThisField){
                                                            pointsByOpponent = pointsWithThisField;
                                                            startOfOpponentGraph = startOpponentField;
                                                            endOfOpponentGraph = endOpponentField;
//                                                            System.out.println("startOpponent = " + startOfOpponentGraph + "  endOpponent = " + endOfOpponentGraph + " cause of upadteMove");
//								System.out.println("New score for opponent = " + pointsByOpponent);
                                                    }
                                            }
//						System.out.println("Connection added between (" + x + "," + y + ") and (" + pX + "," + pY + ")");
                                    }else{
//						System.out.println("Connection not possible cause of other connection");
                                    }
                            }
                    }
            }//end check for new connections---------------------

    }

    public void undoMove(Field move, boolean isJinxMove){
            int x = move.getX();
            int y = move.getY();
            FieldColor fieldColor = move.getFieldColor();

            final int[][] possibleFields = {
                            {-2, -1}, {-2, 1},
                            {-1, -2}, {-1, 2},
                            { 1, -2}, { 1, 2},
                            { 2, -1}, { 2, 1}
            };

            //get all connections
            ArrayList<Field> allConnections = new ArrayList<Field>();
            for(Field c : move.getConnections()){
                allConnections.add(c);
            }

            //figure out if the field that becomes removed is in the graph that is worth most points
            boolean fieldThatBecomesRemovedIsInGraphWhichIsWorthMostPoints = false;
            if(allConnections.size() > 0){//fields that have no connection don't have to be checked later on 
                                          //(important for start- and endField, without this they stay null in the beginning)
                if(isJinxMove){
                    //if field (x, y) is part of the graph that is worth most points for jinx
                    if(pointsWithField(move, Jinx.jinxIsPlayingVertical) == pointsByJinx){
                            fieldThatBecomesRemovedIsInGraphWhichIsWorthMostPoints = true;
                    }
                }else{
                    //if field (x, y) is part of the graph that is worth most points for the opponent
                    if(pointsWithField(move, !Jinx.jinxIsPlayingVertical) == pointsByOpponent){
                            fieldThatBecomesRemovedIsInGraphWhichIsWorthMostPoints = true;
                    }
                }
            }

            //remove all connections and the color
            for(Field c : allConnections){
                    getField(x,y).removeConnectionTo(c);
                    c.removeConnectionTo(getField(x,y));
            }
            //NOT ALWAYS BLACK (BORDER l. initBoard())!
            getField(x, y).setFieldColor(FieldColor.BLACK);

            //recalculate points (and start/end field)
            //if the field (x,y) was in the graph that was worth most points
            if(fieldThatBecomesRemovedIsInGraphWhichIsWorthMostPoints){
                    int pointsAfterRemoving = -1;//so startFieldAfterRemoving 
                                                //and endFieldAfterRemoving don't stay null
                    Field startFieldAfterRemoving = null, endFieldAfterRemoving = null;
                    int p;
                    Field startField, endField;
                    for(Field c : allConnections){

                            if(isJinxMove){
                                //calculate p, but also 
                                //startJinxField and endJinxField
                                p = pointsWithField(c, Jinx.jinxIsPlayingVertical);
                                startField = startJinxField;
                                endField = endJinxField;
                            }else{
                                //calculate p, but also 
                                //startOpponentField and endOpponentField
                                p = pointsWithField(c, !Jinx.jinxIsPlayingVertical);
                                startField = startOpponentField;
                                endField = endOpponentField;
                            }
                            if(p > pointsAfterRemoving){
                                    pointsAfterRemoving = p;
                                    startFieldAfterRemoving = startField;
                                    endFieldAfterRemoving = endField;
                            }
                    }
                    if(isJinxMove){
                            pointsByJinx = pointsAfterRemoving;
                            startOfJinxGraph = startFieldAfterRemoving;
                            endOfJinxGraph = endFieldAfterRemoving;
//				System.out.println("pointsByJinx = " + pointsByJinx + " cause of undoMove");
//                            System.out.println("startJinx = " + startOfJinxGraph + "  endJinx = " + endOfJinxGraph + " cause of undoMove");
                    }else{
                            pointsByOpponent = pointsAfterRemoving;
                            startOfOpponentGraph = startFieldAfterRemoving;
                            endOfOpponentGraph = endFieldAfterRemoving;
//				System.out.println("pointsByOpponent = " + pointsByOpponent + " cause of undoMove");
//                            System.out.println("startOpponent = " + startOfOpponentGraph + "  endOpponent = " + endOfOpponentGraph + " cause of undoMove");
                    }
            }

            
    }

    public float evaluateBoardPosition(){
            //Evaluates the current state of the board.
            //The higher the result is, the better is the situation for jinx
            float result;


            //First (since 0.01) evaluation method: points by jinx/points by opponent
//            if(pointsByOpponent != 0){
//                    result = pointsByJinx/(float)(pointsByOpponent);
////                        result = -pointsByOpponent;
//            }else{
//                    result = pointsByJinx*1.1f;
//            }
            
            //so 16:8 is worse than 17:9 
            if(pointsByJinx > pointsByOpponent){
                result = 1.1f * pointsByJinx - pointsByOpponent;
                
            }else if(pointsByJinx == pointsByOpponent){
                result = pointsByJinx - pointsByOpponent;
                
            }else{
                result = pointsByJinx - 1.1f * pointsByOpponent;
            }
            
            
            /*v1,h1,v2,h2 the higher the better for horizontal player:
             => the smaller h the better
             => the higher v the better
             - v1, h1 are more important than v2 and h2
            
            version 1: (f * v1 + v2) / (f * h1 + h2)
            version 2: v1 / h1
               
                
            */
            
            
//                if(pointsByOpponent != 0){
//			result = 1/(float)(pointsByOpponent);
//		}else{
//			result = 1.1f;
//		}
            return result;
    }

    //important part of the Jinx AI. Returns all 'good' moves
    //that can be done (returning all possible moves would be too much
    //to calculate in a senseful depth)
    public ArrayList<Field> preselectMoves(Field lastMove, Field secondLastMove){

            int x = lastMove.getX();
            int y = lastMove.getY();

            ArrayList<Field> result = new ArrayList<Field>();

            final int[][] goodFields = {                                    {0, -4},
                                                                    {-1, -3}, {0, -3}, {1, -3}, 
                                              {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
                            {-3, -1}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {3, -1},
                            {-4,0}, {-3,  0}, {-2,  0}, {-1,  0},          {1,  0}, {2,  0}, {3,  0}, {4,0},
                            {-3,  1}, {-2,  1}, {-1,  1}, {0,  1}, {1,  1}, {2,  1}, {3,  1},
                                              {-2,  2}, {-1,  2}, {0,  2}, {1,  2}, {2,  2},
                                                                    {-1,  3}, {0,  3}, {1,  3},
                                                                    {0, 4}
            };

//            final int[][] goodFields2 = {                                    {0, -4},
//                                                                    {-1, -3}, {0, -3}, {1, -3}, 
//                                              {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
//                            {-3, -1}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {3, -1},
//                            {-4,0}, {-3,  0}, {-2,  0}, {-1,  0},          {1,  0}, {2,  0}, {3,  0}, {4,0},
//                            {-3,  1}, {-2,  1}, {-1,  1}, {0,  1}, {1,  1}, {2,  1}, {3,  1},
//                                              {-2,  2}, {-1,  2}, {0,  2}, {1,  2}, {2,  2},
//                                                                    {-1,  3}, {0,  3}, {1,  3},
//                                                                    {0, 4}
//            };

//                final int[][] goodFields = {
//                                                              {0, -3}, 
//                                                    {-1, -2}, {0, -2}, {1, -2}, 
//                                          {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1},
//				{-3,  0}, {-2,  0}, {-1, 0},          {1,  0}, {2,  0}, {3,  0},
//                                          {-2,  1}, {-1,  1}, {0,  1}, {1,  1}, {2,  1},
//                                                    {-1,  2}, {0,  2}, {1,  2}, 
//                                                              {0,  3}
//		};

//		final int[][] goodFields = {
//				{-3, -1}, {-3,  0}, {-3,  1},
//				{3,  -1}, { 3,  0}, { 3,  2},
//				{-2, -2}, {-2, -1}, {-2,  0}, {-2,  1}, {-2,  2}, 
//				{ 2, -2}, { 2, -1}, { 2,  0}, { 2,  1}, { 2,  2},
//				{-1, -3}, {-1, -2}, {-1, -1}, {-1,  0}, {-1,  1}, {-1,  2}, {-1,  3},
//				{ 1, -3}, { 1, -2}, { 1, -1}, { 1,  0}, { 1,  1}, { 1,  2}, { 1,  3},
//				{ 0, -3}, { 0, -2}, { 0, -1}, 		    { 0,  1}, { 0,  2}, { 0,  3},
//		};

            int pX, pY;
            //second last move was a move by the current player
            final int[][] goodFieldsFromSecondLastMove = {                    
                                            {0, -4},	
                    {-3, -3},                                       {3, -3},
                                    {-1, -2},       {1, -2}, 
                            {-2, -1},                       {2, -1},
            {-4, 0},                                                        {4, 0},
                            {-2,  1},                       {2,  1},
                                    {-1,  2},       {1,  2}, 
                    {-3,  3},                                       {3,  3},
                                            {0,  4}
            };

            //get fields around lastMove
            for(int[] f : goodFields){
                    pX = x+f[0];
                    pY = y+f[1];
                    if(pX >= 0 && pX < 24 && pY >= 0 && pY < 24){
                            if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                                    result.add(getField(pX,pY));
                            }
                    }else{
//				System.out.println("Out of Bounds!\n"
//						+ "lastField (" + x + ", " + y + "); f = (" + pX + ", " + pY + ")") ;	
                    }
            }

            //get fields around secondLastMove
            if(secondLastMove != null){//if it is at least turn 3
                    x = secondLastMove.getX();
                    y = secondLastMove.getY();

                    for(int[] f : goodFieldsFromSecondLastMove){
                            pX = x+f[0];
                            pY = y+f[1];
                            if(pX >= 0 && pX < 24 && pY >= 0 && pY < 24){
                                    if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                                            if(!result.contains(getField(pX,pY))){
                                                    result.add(getField(pX,pY));
                                            }
                                    }
                            }else{
    //				System.out.println("Out of Bounds!\n"
    //						+ "lastField (" + x + ", " + y + "); f = (" + pX + ", " + pY + ")") ;	
                            }
                    }
            }
            assert(result.size() > 0);
            return result;
    }

    //checks if any other connection (by opponent or even by jinx) 
    //prevents a new connection between f1 and f2
    private boolean connectionIsPossibleBetween(int[] f1, int[] f2){
            //ASSERT f1 and f2 have 'knight-distance' (chess)

            int x1, y1, x2, y2;

            if(f1[0] > f2[0]){
                    //change f1 and f2
                    x1 = f2[0];
                    y1 = f2[1];
                    x2 = f1[0];
                    y2 = f1[1];
            }else{
                    x1 = f1[0];
                    y1 = f1[1];
                    x2 = f2[0];
                    y2 = f2[1];
            }

            final int[][][] connectionsToCheckInCase1 = {
                            {{1, 1}, {0, -1}},
                            {{1, 1}, {2, -1}},
                            {{1, 1}, {3,  0}},

                            {{0, 1}, {1, -1}},
                            {{0, 1}, {2,  0}},

                            {{2, 2}, {1,  0}},
                            {{1, 2}, {2,  0}},
                            {{0, 2}, {1,  0}},
                            {{-1, 1},{1,  0}},
            };
            final int[][][] connectionsToCheckInCase2 = {
                            {{1, 0}, {-1, -1}},
                            {{1, 0}, {0, -2}},
                            {{1, 0}, {2,  -2}},

                            {{2, 0}, {0, -1}},
                            {{2, 0}, {1,  -2}},

                            {{0, 1}, {1, -1}},
                            {{1, 1}, {0, -1}},
                            {{2, 1}, {1, -1}},
                            {{3, 0}, {1, -1}},
            };
            final int[][][] connectionsToCheckInCase3 = {
                            {{0, 1}, {1, -1}},
                            {{0, 1}, {2,  0}},
                            {{0, 1}, {2,  2}},

                            {{0, 2}, {1, 0}},
                            {{0, 2}, {2, 1}},

                            {{-1, 0}, {1, 1}},
                            {{-1, 1}, {1, 0}},
                            {{-1, 2}, {1, 1}},
                            {{ 0, 3}, {1, 1}},
            };
            final int[][][] connectionsToCheckInCase4 = {
                            {{1, -1}, {-1,  0}},
                            {{1, -1}, {-1, -2}},
                            {{1, -1}, { 0, -3}},

                            {{1, 0}, {-1, -1}},
                            {{1, 0}, { 0, -2}},

                            {{2, -2}, {0, -1}},
                            {{2, -1}, {0, -2}},
                            {{2,  0}, {0, -1}},
                            {{1,  1}, {0, -1}},
            };


            /*distinguish between 4 cases of the relation of f1 (x1, y1) and f2 (x2, y2). 
             * x1 < x2 is true per definition:
             */
            final int[][][] connectionsToCheck;
            if(x1+2 == x2 && y1+1 == y2){
                    connectionsToCheck = connectionsToCheckInCase1;

            }else if(x1+2 == x2 && y1-1 == y2){
                    connectionsToCheck = connectionsToCheckInCase2;

            }else if(x1+1 == x2 && y1+2 == y2){
                    connectionsToCheck = connectionsToCheckInCase3;

            }else if(x1+1 == x2 && y1-2 == y2){
                    connectionsToCheck = connectionsToCheckInCase4;

            }else{
                    connectionsToCheck = null;//make eclipse happy
                    System.out.println("ERROR, wrong arguments f1 and f2 passed! They are not in 'knight-distance'");
                    assert(false);
            }

            //check connections
            for(int[][] c : connectionsToCheck){
                    int xP1 = x1+c[0][0];
                    int yP1 = y1+c[0][1];
                    int xP2 = x1+c[1][0];
                    int yP2 = y1+c[1][1];

                    //if P1 and P2 are on the field
                    if(xP1 >= 0 && xP1 < 24 && yP1 >= 0 && yP1 < 24 && 
                       xP2 >= 0 && xP2 < 24 && yP2 >= 0 && yP2 < 24){

                        if(getField(xP1, yP1).isConnectedWith(xP2, yP2)){
                                return false;
                        }
                    }

            }
            return true;
    }

    private Field minXField, minYField, maxXField, maxYField;
    private ArrayList<Field> alreadyVisited = new ArrayList<Field>();
    private int pointsWithField(Field f, boolean isPlayingVertical){
        minXField = f; 
        maxXField = f;
        minYField = f;
        maxYField = f;

        //try to comment these two lines (Jinx is playing really bad after,
        //but the depth increases by 2 ! => pointsWithField is really 
        //performance intensive
        alreadyVisited.clear();
        alreadyVisited.add(f);

        calcPoints(f);

        if(isPlayingVertical){
            if(Jinx.jinxIsPlayingVertical){//move by jinx (vertical)
                startJinxField = minYField;
                endJinxField = maxYField;
            }else{                        //move by opponent (vertical)
                startOpponentField = minYField;
                endOpponentField = maxYField;
            }
            return maxYField.getY() - minYField.getY();
        }else{
            if(Jinx.jinxIsPlayingVertical){//move by opponent (horizontal)
                startOpponentField = minXField;
                endOpponentField = maxXField;

            }else{                         //move by jinx (horizontal)
                startJinxField = minXField;
                endJinxField = maxXField;
            }
            return maxXField.getX() - minXField.getX();
        }
    }

    private void calcPoints(Field f){
        ArrayList<Field> connections = f.getConnections();
        for(Field c : connections){
            if(!alreadyVisited.contains(c)){
                if(c.getX() < minXField.getX())minXField = c;
                if(c.getX() > maxXField.getX())maxXField = c;
                if(c.getY() < minYField.getY())minYField = c;
                if(c.getY() > maxYField.getY())maxYField = c;

                alreadyVisited.add(c);
                calcPoints(c);
            }
        }
    }
//        
    private void initFields(GameState gameState){
            List<Move> possibleMoves = gameState.getPossibleMoves();

            //every field is green per default
            for(int row=0; row<24; row++){
                    for(int col=0; col<24; col++){
                            fields[row][col] = new Field(row, col);
                            fields[row][col].setFieldColor(FieldColor.GREEN);
                    }
            }

            //every possible field becomes black
            for(Move move : possibleMoves){
                    fields[move.getX()][move.getY()].setFieldColor(FieldColor.BLACK);
            }


            //EVERY BORDER FIELD BY THE OPPONENT IS GREEN AND EVERY 
            //BORDER FIELD BY JINX IS BLACK WHITH THIS METHOD
    }
        
    int getNumberOfSetFields() {
        int result = 0;
        for(int row=0; row<24; row++){
            for(int col=0; col<24; col++){
                if(fields[col][row].getFieldColor() == FieldColor.JINX ||
                   fields[col][row].getFieldColor() == FieldColor.OPPONENT){
                    result++;
                }
            }
        }
        return result;
    }
}
