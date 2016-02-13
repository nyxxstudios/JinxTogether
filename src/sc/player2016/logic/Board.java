package sc.player2016.logic;

import java.util.ArrayList;
import java.util.List;

import sc.player2016.logic.Jinx.FieldColor;
import sc.plugin2016.GameState;
import sc.plugin2016.Move;

public class Board {
    Field[][] fields = new Field[24][24];
//
//    int pointsByJinx = -1;
//    int pointsByOpponent = -1;
    
    //vertical player starts the game
    //set in updateMove and undoMove, read in evaluateCurrentConfliczone
    boolean isJinxTurn = Jinx.jinxIsPlayingVertical;
    
    //graphs by jinx sorted by points (first graph has most)
    ArrayList<Graph> graphsByJinx = new ArrayList<>();
    //graphs by opponent sorted by points (first graph has most)
    ArrayList<Graph> graphsByOpponent = new ArrayList<>();

    public Board(GameState gameStateAtBeginning){
            initFields(gameStateAtBeginning);
    }

    public Field getField(int x, int y){
            return fields[x] [y];
    }

    public void updateBoard(Field move, boolean isJinxMove){
            FieldColor fieldColor;
            
            if(isJinxMove){
                    fieldColor = FieldColor.JINX;
            }else{
                    fieldColor = FieldColor.OPPONENT; 
            }

            int x = move.getX();
            int y = move.getY();

            //setFieldColor
            move.setFieldColor(fieldColor);

            //check for new connections--------------------------
            final int[][] possibleFields = {
                            {-2, -1}, {-2, 1},
                            {-1, -2}, {-1, 2},
                            { 1, -2}, { 1, 2},
                            { 2, -1}, { 2, 1}
            };
            boolean isFirstConnectionAlreadyAdded = false;
            int indexOfMainGraph = 0;//0 just to make netbeans happy
            ArrayList<Graph> graphsByCurrentPlayer = isJinxMove?graphsByJinx:graphsByOpponent;
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
                            move.addConnectionTo(getField(pX, pY));
                            getField(pX, pY).addConnectionTo(move);

                            //if this connection is the first one
                            if(!isFirstConnectionAlreadyAdded){
//                                            System.out.println("first conn: " + getField(pX, pY));
//                                            System.out.println("actual field = " + getField(x,y));
//                                            System.out.println("graphsByJinx = " + graphsByJinx);
                                for(int i=0; i<graphsByCurrentPlayer.size(); i++){
                                    if(graphsByCurrentPlayer.get(i).containsField(getField(pX, pY))){
                                        graphsByCurrentPlayer.get(i).addField(move);
                                        indexOfMainGraph = moveGraphDownToRightPosition(i, isJinxMove);

                                        break;
                                    }
                                }
                                isFirstConnectionAlreadyAdded = true;
                            }else{
//                                            System.out.println("second conn: " + getField(pX, pY));
//                                            System.out.println("actual field = " + getField(x,y));
//                                            System.out.println("graphsByJinx = " + graphsByJinx);
                                 for(int i=0; i<graphsByCurrentPlayer.size(); i++){
                                     if(graphsByCurrentPlayer.get(i).containsField(getField(pX, pY))){
                                        if(i != indexOfMainGraph){//if both connectionfields were in the same graph before
                                            graphsByCurrentPlayer.get(indexOfMainGraph)
                                                    .addGraph(graphsByCurrentPlayer.get(i));
                                            graphsByCurrentPlayer.remove(i);
                                            if(indexOfMainGraph > i){ indexOfMainGraph--; }
                                            indexOfMainGraph = moveGraphDownToRightPosition(indexOfMainGraph, isJinxMove);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }//end check for new connections---------------------

            if(!isFirstConnectionAlreadyAdded){//no connections added at all
                graphsByCurrentPlayer.add(new Graph(move));
            }
//            System.out.println("graphsByJinx: " + graphsByJinx);
            
            isJinxTurn = !isJinxTurn;
            
    }

    public void undoMove(Field move, boolean isJinxMove){
        
            ArrayList<Graph> graphsByCurrentPlayer = isJinxMove?graphsByJinx:graphsByOpponent;
            
            //NOT ALWAYS BLACK (BORDER l. initBoard())!
            move.setFieldColor(FieldColor.BLACK);

            //recalculate graphs
            //if the field had at least 1 connection
            if(move.getConnections().size() > 0){
                for(int i=0; i<graphsByCurrentPlayer.size(); i++){
                     if(graphsByCurrentPlayer.get(i).containsField(move)){
                         
                         ArrayList<Graph> newGraphs = graphsByCurrentPlayer.get(i).removeField(move);
                         
//                         System.out.println("splitted Graphs = " + splittedGraphs);
                         moveGraphUpToRightPosition(i, isJinxMove);
                         for(Graph g : newGraphs){
                             graphsByCurrentPlayer.add(g);
                             moveGraphDownToRightPosition(graphsByCurrentPlayer.size()-1, isJinxMove);
                         }
//                         System.out.println("graphsByCurrentPlayer = " + graphsByCurrentPlayer);
                         break;
                     }    
                }
            }else{
                for(int i=graphsByCurrentPlayer.size()-1; i>= 0; i--){
                    if(graphsByCurrentPlayer.get(i).containsField(move)){
                        graphsByCurrentPlayer.remove(i);
                        break;
                    }
                }
            }
            isJinxTurn = !isJinxTurn;
    }

    public float evaluateBoardPosition(){
        if(Jinx.jinxIsPlayingVertical){
            return Evaluator.evaluateBoardPosition(graphsByJinx, graphsByOpponent, 
                    isJinxTurn, graphsByJinx.get(0).getPoints(Jinx.jinxIsPlayingVertical),
                    graphsByOpponent.get(0).getPoints(!Jinx.jinxIsPlayingVertical));
        }else{
            return Evaluator.evaluateBoardPosition(graphsByOpponent, graphsByJinx, 
                    !isJinxTurn, graphsByJinx.get(0).getPoints(Jinx.jinxIsPlayingVertical),
                    graphsByOpponent.get(0).getPoints(!Jinx.jinxIsPlayingVertical));
        }
    }
    
    //important part of the Jinx AI. Returns all 'good' moves
    //that can be done (returning all possible moves would be too much
    //to calculate in a senseful depth)
    public ArrayList<Field> preselectMovesOld(Field lastMove, Field secondLastMove){

            int x = lastMove.getX();
            int y = lastMove.getY();

            ArrayList<Field> result = new ArrayList<Field>();

            final int[][] goodFields = { { 0,-4},
                                {-1,-3}, { 0,-3}, { 1,-3}, 
                       {-2,-2}, {-1,-2}, { 0,-2}, { 1,-2}, { 2,-2},
              {-3,-1}, {-2,-1}, {-1,-1}, { 0,-1}, { 1,-1}, { 2,-1}, { 3,-1},
     {-4, 0}, {-3, 0}, {-2, 0}, {-1, 0},          { 1, 0}, { 2, 0}, { 3, 0}, { 4, 0},
              {-3, 1}, {-2, 1}, {-1, 1}, { 0, 1}, { 1, 1}, { 2, 1}, { 3, 1},
                       {-2, 2}, {-1, 2}, { 0, 2}, { 1, 2}, { 2, 2},
                                {-1, 3}, { 0, 3}, { 1, 3},
                                         { 0, 4}
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

    //important part of the Jinx AI. Returns all 'good' moves
    //that can be done (returning all possible moves would be too much
    //to calculate in a senseful depth)
    public ArrayList<Field> preselectMoves(Field lastMove, boolean isVertical){
        ArrayList<Field> result = new ArrayList<>();
        Field help;
        ArrayList<Graph> graphsByCurrentPlayer;
        int x, y, pX, pY;
            
        if(isVertical){//preselect for vertical player
            
            if(Jinx.jinxIsPlayingVertical){
                graphsByCurrentPlayer = graphsByJinx;
            }else{
                graphsByCurrentPlayer = graphsByOpponent;
            }

            final int[][] goodFieldsFromOwnMinY = {                    
                                                      {0, -4},	
                         {-3, -3},                                              {3, -3},
    //                                      {-1, -2},         {1, -2},      //already added before
                                   {-2, -1},                           {2, -1},
                {-4, 0},                                                                {4, 0},
            };

            final int[][] goodFieldsFromOwnMaxY = {                    
                {-4, 0},                                                                {4, 0},
                                  {-2,  1},                            {2,  1},
    //                                      {-1,  2},         {1,  2},  //already added before
                         {-3,  3},                                              {3,  3}, 
                                                      {0,  4}
            };

            final int[][] goodFieldsReactToOpponentMove = {
    //                                                {-1,-3}, { 0,-3}, { 1,-3}, 
                                    {-3,-3},                                            { 3,-3},
                                            {-2,-1}, {-1,-1}, { 0,-1}, { 1,-1}, { 2,-1},
                            {-4, 0},                 {-1, 0},          { 1, 0},                 { 4, 0},
                                            {-2, 1}, {-1, 1}, { 0, 1}, { 1, 1}, { 2, 1},
                                    {-3, 3},                                            { 3, 3},
    //                                                {-1, 3}, { 0, 3}, { 1, 3},
    //                                                         { 0, 4}
            };

            //add (maximum) 4 fields for each graph (possible connections to other graphs)
            for(Graph g : graphsByCurrentPlayer){
//                if(g.hasJustOneField())break;
                //add two fields to result for minYField
                if(g.getMinYField().getY() - 2 >= 0){
                    if(g.getMinYField().getX() - 1 > 0){
                        help = fields[g.getMinYField().getX()-1][g.getMinYField().getY()-2];
                        if(help.getFieldColor() == FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                    if(g.getMinYField().getX() + 1 < 23){
                        help = fields[g.getMinYField().getX()+1][g.getMinYField().getY()-2];
                        if(help.getFieldColor() == FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                }
                //add two fields to result for maxYField
                if(g.getMaxYField().getY() + 2 <= 23){
                    if(g.getMaxYField().getX() - 1 > 0){
                        help = fields[g.getMaxYField().getX()-1][g.getMaxYField().getY()+2];
                        if(help.getFieldColor() == FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                    if(g.getMaxYField().getX() + 1 < 23){
                        help = fields[g.getMaxYField().getX()+1][g.getMaxYField().getY()+2];
                        if(help.getFieldColor() == FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                }
            }

            //add fields for start and end field of own graph
            //get fields from minY
            x = graphsByCurrentPlayer.get(0).getMinYField().getX();
            y = graphsByCurrentPlayer.get(0).getMinYField().getY();
            for(int[] f : goodFieldsFromOwnMinY){
                pX = x+f[0];
                pY = y+f[1];
                if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                    if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                        if(!result.contains(getField(pX,pY))){
                                result.add(getField(pX,pY));
                        }
                    }
                }
            }
            //get fields from maxY
            x = graphsByCurrentPlayer.get(0).getMaxYField().getX();
            y = graphsByCurrentPlayer.get(0).getMaxYField().getY();
            for(int[] f : goodFieldsFromOwnMaxY){
                pX = x+f[0];
                pY = y+f[1];
                if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                    if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                        if(!result.contains(getField(pX,pY))){
                                result.add(getField(pX,pY));
                        }
                    }
                }
            }

            //add fields from lastMove (opponent)
            x = lastMove.getX();
            y = lastMove.getY();
            for(int[] f : goodFieldsReactToOpponentMove){
                pX = x+f[0];
                pY = y+f[1];
                if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                    if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                        if(!result.contains(getField(pX,pY))){
                                result.add(getField(pX,pY));
                        }
                    }
                }
            }   

            return result;
            
        }else{//preselect for horizontal player
            
            if(Jinx.jinxIsPlayingVertical){
                graphsByCurrentPlayer = graphsByOpponent;
            }else{
                graphsByCurrentPlayer = graphsByJinx;
            }

            final int[][] goodFieldsFromOwnMinX = {                    
                                                  {0, -4},	
                        {-3, -3},                                   
                                        {-1, -2},      

                {-4, 0},                                             

                                        {-1,  2},      
                        {-3,  3},                                      
                                                  {0,  4}
                };

            final int[][] goodFieldsFromOwnMaxX = {                    
                {0, -4},	
                                    {3, -3},
                      {1, -2}, 

                                            {4, 0},

                      {1,  2}, 
                                    {3,  3},
                {0,  4}
            };

            final int[][] goodFieldsReactToOpponentMove = {
                                            { 0,-4},
                        {-3, -3},                                   { 3,-3},
                                   {-1,-2},          { 1,-2},
                                   {-1,-1}, { 0,-1}, { 1,-1}, 
                                   {-1, 0},          { 1, 0},
                                   {-1, 1}, { 0, 1}, { 1, 1},
                                   {-1, 2},          { 1, 2},
                        {-3, 3},                                    { 3, 3},
                                            { 0, 4}
            };

            //add (maximum) 4 fields for each graph (possible connections to other graphs)
            for(Graph g : graphsByCurrentPlayer){
//                if(g.hasJustOneField())break;
                //add two fields to result for minXField
                if(g.getMinXField().getX() - 2 >= 0){
                    if(g.getMinXField().getY() - 1 > 0){
                        help = fields[g.getMinXField().getX()-2][g.getMinXField().getY()-1];
                        if(help.getFieldColor() == FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                    if(g.getMinXField().getY() + 1 < 23){
                        help = fields[g.getMinXField().getX()-2][g.getMinXField().getY()+1];
                        if(help.getFieldColor() == FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                }
                //add two fields to result for maxXField
                if(g.getMaxXField().getX() + 2 <= 23){
                    if(g.getMaxXField().getY() - 1 > 0){
                        help = fields[g.getMaxXField().getX()+2][g.getMaxXField().getY()-1];
                        if(help.getFieldColor() == FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                    if(g.getMaxXField().getY() + 1 < 23){
                        help = fields[g.getMaxXField().getX()+2][g.getMaxXField().getY()+1];
                        if(help.getFieldColor() == FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                }
            }

            //add fields for start and end field of own graph
            //get fields from minX
            x = graphsByCurrentPlayer.get(0).getMinXField().getX();
            y = graphsByCurrentPlayer.get(0).getMinXField().getY();
            for(int[] f : goodFieldsFromOwnMinX){
                pX = x+f[0];
                pY = y+f[1];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                        if(!result.contains(getField(pX,pY))){
                                result.add(getField(pX,pY));
                        }
                    }
                }
            }
            //get fields from maxY
            x = graphsByCurrentPlayer.get(0).getMaxYField().getX();
            y = graphsByCurrentPlayer.get(0).getMaxYField().getY();
            for(int[] f : goodFieldsFromOwnMaxX){
                pX = x+f[0];
                pY = y+f[1];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                        if(!result.contains(getField(pX,pY))){
                                result.add(getField(pX,pY));
                        }
                    }
                }
            }

            //add fields from lastMove (opponent)
            x = lastMove.getX();
            y = lastMove.getY();
            for(int[] f : goodFieldsReactToOpponentMove){
                pX = x+f[0];
                pY = y+f[1];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                        if(!result.contains(getField(pX,pY))){
                                result.add(getField(pX,pY));
                        }
                    }
                }
            }   

            return result;
        }
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
    
    private int moveGraphDownToRightPosition(int index, boolean isJinx){
        
        //move graph downwards (smaller indices, element 0 is worth most points)
        //in graphsByCurrentPlayer until it is at the right position
        if(isJinx){
            Graph g = graphsByJinx.get(index);
            graphsByJinx.remove(index);
            int points = g.getPoints(Jinx.jinxIsPlayingVertical);
            for(int i=index-1; i>=0; i--){
                if(graphsByJinx.get(i).getPoints(Jinx.jinxIsPlayingVertical) >= points){
                    graphsByJinx.add(i+1, g);
                    return i+1;
                }
            }
            graphsByJinx.add(0, g);
            return 0;
            
        }else{//is opponent
            Graph g = graphsByOpponent.get(index);
            graphsByOpponent.remove(index);
            int points = g.getPoints(!Jinx.jinxIsPlayingVertical);
            for(int i=index-1; i>=0; i--){
                if(graphsByOpponent.get(i).getPoints(!Jinx.jinxIsPlayingVertical) >= points){
                    graphsByOpponent.add(i+1, g);
                    return i+1;
                }
            }
            graphsByOpponent.add(0, g);
            return 0;
        }
//        return index;
    }
    
    private void moveGraphUpToRightPosition(int index, boolean isJinx){
        
        //move graph upwards (bigger indices, element 0 is worth most points)
        //in graphsByCurrentPlayer until it is at the right position
        if(isJinx){
            Graph g = graphsByJinx.get(index);
            graphsByJinx.remove(index);
            int points = g.getPoints(Jinx.jinxIsPlayingVertical);
            for(int i=index; i<graphsByJinx.size(); i++){
                if(graphsByJinx.get(i).getPoints(Jinx.jinxIsPlayingVertical) <= points){
                    graphsByJinx.add(i, g);
                    return;
                }
            }
            graphsByJinx.add(graphsByJinx.size(), g);
            
        }else{//is opponent
            Graph g = graphsByOpponent.get(index);
            graphsByOpponent.remove(index);
            int points = g.getPoints(!Jinx.jinxIsPlayingVertical);
            for(int i=index; i<graphsByOpponent.size(); i++){
                if(graphsByOpponent.get(i).getPoints(!Jinx.jinxIsPlayingVertical) <= points){
                    graphsByOpponent.add(i, g);
                    return;
                }
            }
            graphsByOpponent.add(graphsByOpponent.size(), g);
        }
    }
    
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
