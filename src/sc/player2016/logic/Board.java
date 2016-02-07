package sc.player2016.logic;

import java.util.ArrayList;
import java.util.List;

import sc.player2016.logic.Jinx.FieldColor;
import sc.plugin2016.GameState;
import sc.plugin2016.Move;

public class Board {
    Field[][] fields = new Field[24][24];

    int pointsByJinx = -1;
    int pointsByOpponent = -1;

    //necessary for pointCalculation
    ArrayList<Field> fieldsByJinx = new ArrayList<>();
    ArrayList<Field> fieldsByOpponent = new ArrayList<>();
    
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
                    fieldsByJinx.add(move);
                    if(pointsByJinx == -1){
                        pointsByJinx = 0;
                        startOfJinxGraph = move;
                        endOfJinxGraph = move;
                    }
            }else{
                    fieldColor = FieldColor.OPPONENT; 
                    isPlayingVertical = !Jinx.jinxIsPlayingVertical;
                    fieldsByOpponent.add(move);
                    if(pointsByOpponent == -1){
                        pointsByOpponent = 0;
                        startOfOpponentGraph = move;
                        endOfOpponentGraph = move;
                    }
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

                                            //current way, slightly faster
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
                                            
                                            //alternative way, slightly slower
//                                            if(isJinxMove){
//                                                pointsByJinx = calcPointsByPlayer(true);
//                                                
//                                            }else{
//                                                 pointsByOpponent = calcPointsByPlayer(false);
//                                            }
//						System.out.println("Connection added between (" + x + "," + y + ") and (" + pX + "," + pY + ")");
                                    }else{
//						System.out.println("Connection not possible cause of other connection");
                                    }
                            }
                    }
            }//end check for new connections---------------------

    }

    public void undoMove(Field move, boolean isJinxMove){
        
            if(isJinxMove){
                fieldsByJinx.remove(move);
            }else{
                fieldsByOpponent.remove(move);
            }

            boolean connectionsWereGreaterZero = false;
            while(move.getConnections().size() > 0){
                connectionsWereGreaterZero = true;
                
                //order of these two statements is important!
                move.getConnections().get(0).removeConnectionTo(move);
                move.removeConnectionTo(move.getConnections().get(0));
            }
            
            //NOT ALWAYS BLACK (BORDER l. initBoard())!
            move.setFieldColor(FieldColor.BLACK);

            //recalculate points (and start/end field)
            //if the field had at least 1 connection
            if(connectionsWereGreaterZero){
                if(isJinxMove){
                    pointsByJinx = calcPointsByPlayer(true);

                }else{
                     pointsByOpponent = calcPointsByPlayer(false);
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

    float evaluateCurrentConflictzone(){
        //find conflictzone and set the 2 relevant points
        //(reduce every conflictzone to a conflict in the down-left corner:
        //vertical player tries to reach the bottom and horizontal tries 
        //to reach the left border. This is necessary to use the 'hand-calculated'
        //formulas for the coordinate system; down-left corner of the board is 
        //defined as coordinate origin (0|0))
        Field pV;//vertical (playing) point of conflict zone (translated to the down-left corner equivalent)
        Field pH;//horizontal (playing) point of conflict zone (translated to the down-left corner equivalent)
        
        //helper vars for setting pV and pH
        float minSquaredDistance;
        float help;
        
        
        
        //the translation to pV and pH is tricky because of two aspects:
        //1. translate in another corner (down-left)
        //2. translate in another coordinate system (origin is in down-left corner now
        //   -> (0|23) transforms to (0|0))
        //That is why e. g. when the conflictzone is in the up-left corner,
        //we can just copy the points and have the right points in the down-left 
        //corner in the new coordinate system
        if(Jinx.jinxIsPlayingVertical){
            //set first to startJinx to startOpponent (conflictzone: up-left)
            minSquaredDistance = getSquaredDistance(startOfJinxGraph, startOfOpponentGraph);
            pV = startOfJinxGraph;
            pH = startOfOpponentGraph;
            System.out.println("vert up-left");
            
            //check startJinx to endOpponent (conflictzone: up-right)
            help = getSquaredDistance(startOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - startOfJinxGraph.getX(),    startOfJinxGraph.getY());
                pH = new Field(23 - endOfOpponentGraph.getX(),  endOfOpponentGraph.getY());
                System.out.println("vert up-right");
            }

            //check endJinx to startOpponent (conflictzone: down-left)
            help = getSquaredDistance(endOfJinxGraph, startOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(endOfJinxGraph.getX(),       23 - endOfJinxGraph.getY());
                pH = new Field(startOfOpponentGraph.getX(), 23 - startOfOpponentGraph.getY());
                System.out.println("vert down-left");
            }

            //check endJinx to endOpponent (conflictzone: down-right)
            help = getSquaredDistance(endOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - endOfJinxGraph.getX(),     23 - endOfJinxGraph.getY());
                pH = new Field(23 - endOfOpponentGraph.getX(), 23 - endOfOpponentGraph.getY());
                System.out.println("vert down-right");
            }
            
            
        }else{//jinx is playing horizontal
            //set first to startJinx to startOpponent (conflictzone: up-left)
            minSquaredDistance = getSquaredDistance(startOfJinxGraph, startOfOpponentGraph);
            pV = startOfOpponentGraph;
            pH = startOfJinxGraph;
            System.out.println("hor up-left");
        
            //check startJinx to endOpponent (conflictzone: down-left)
            help = getSquaredDistance(startOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(endOfOpponentGraph.getX(), 23 - endOfOpponentGraph.getY());
                pH = new Field(startOfJinxGraph.getX(),   23 - startOfJinxGraph.getY());
                System.out.println("hor down-left");
                
            }

            //check endJinx to startOpponent (conflictzone: up-right)
            help = getSquaredDistance(endOfJinxGraph, startOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - startOfOpponentGraph.getX(), startOfOpponentGraph.getY());
                pH = new Field(23 - endOfJinxGraph.getX(),       endOfJinxGraph.getY());
                System.out.println("hor up-right");
            }

            //check endJinx to endOpponent (conflictzone: down-right)
            help = getSquaredDistance(endOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - endOfOpponentGraph.getX(),  23 - endOfOpponentGraph.getY());
                pH = new Field(23 - endOfJinxGraph.getX(),      23 - endOfJinxGraph.getY());
                System.out.println("hor down-right");
            }
        }

        System.out.println("pV: " + pV + " pH: " + pH);
        
        return 0;
    }
    
    float getSquaredDistance(Field a, Field b){
        return (float) (Math.pow(Math.abs(b.getX() - a.getX()), 2) + 
                Math.pow(Math.abs(b.getY() - a.getY()), 2));
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
    private ArrayList<Field> fieldsToVisit = new ArrayList<>();
    private Field minXField, minYField, maxXField, maxYField;
    private Field minXFieldWithThisGraph, minYFieldWithThisGraph, 
            maxXFieldWithThisGraph, maxYFieldWithThisGraph;
    private Field help;
    int calcPointsByPlayer(boolean isJinx){
        
        if(isJinx){
            //copy fieldsByJinx
            for(Field f : fieldsByJinx){ fieldsToVisit.add(f); }
        }else{
            //copy fieldsByOpponent
            for(Field f : fieldsByOpponent){ fieldsToVisit.add(f); }
        }
        
        if(fieldsToVisit.size() > 0){
            minXField = fieldsToVisit.get(0); 
            maxXField = fieldsToVisit.get(0);
            minYField = fieldsToVisit.get(0);
            maxYField = fieldsToVisit.get(0);
        }else{
            minXField = null; 
            maxXField = null;
            minYField = null;
            maxYField = null;
            return 0;
        }
        
        //iterate over EACH different graph
        while(fieldsToVisit.size() > 0){
            help = fieldsToVisit.get(0);
            minXFieldWithThisGraph = help;
            minYFieldWithThisGraph = help;
            maxXFieldWithThisGraph = help;
            maxYFieldWithThisGraph = help;
            fieldsToVisit.remove(0);
            calcPoints2(help);
            
            if(isJinx == Jinx.jinxIsPlayingVertical){//player is playing vertical
                if(maxYFieldWithThisGraph.getY() - minYFieldWithThisGraph.getY() 
                        > maxYField.getY() - minYField.getY()){
                    maxYField = maxYFieldWithThisGraph;
                    minYField = minYFieldWithThisGraph;
                }
            }else{//player is playing horizontal
                if(maxXFieldWithThisGraph.getX() - minXFieldWithThisGraph.getX() 
                        > maxXField.getX() - minXField.getX()){
                    maxXField = maxXFieldWithThisGraph;
                    minXField = minXFieldWithThisGraph;
                }
            }
        }
        
        if(isJinx){
            if(Jinx.jinxIsPlayingVertical){//move by jinx (vertical)
//                startJinxField = minYField;
//                endJinxField = maxYField;
                startOfJinxGraph = minYField;
                endOfJinxGraph = maxYField;
                return maxYField.getY() - minYField.getY();
            }else{                        //move by jinx (horizontal)
//                startJinxField = minXField;
//                endJinxField = maxXField;
                startOfJinxGraph = minXField;
                endOfJinxGraph= maxXField;
                
                return maxYField.getX() - minYField.getX();
            }
            
        }else{
            if(Jinx.jinxIsPlayingVertical){//move by opponent (horizontal)
//                startOpponentField = minXField;
//                endOpponentField = maxXField;
                startOfOpponentGraph = minXField;
                endOfOpponentGraph = maxXField;
                return maxXField.getX() - minXField.getX();

            }else{                         //move by opponent (vertical)
//                startOpponentField = minYField;
//                endOpponentField = maxYField;
                startOfOpponentGraph = minYField;
                endOfOpponentGraph = maxYField;
                return maxXField.getY() - minXField.getY();
            }
        }
    }
    
    private void calcPoints2(Field f){
        ArrayList<Field> connections = f.getConnections();
        for(Field c : connections){
            if(fieldsToVisit.contains(c)){
                if(c.getX() < minXFieldWithThisGraph.getX())minXFieldWithThisGraph = c;
                if(c.getX() > maxXFieldWithThisGraph.getX())maxXFieldWithThisGraph = c;
                if(c.getY() < minYFieldWithThisGraph.getY())minYFieldWithThisGraph = c;
                if(c.getY() > maxYFieldWithThisGraph.getY())maxYFieldWithThisGraph = c;

                fieldsToVisit.remove(c);
                calcPoints2(c);
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
