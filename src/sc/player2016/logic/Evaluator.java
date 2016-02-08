/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sc.player2016.logic;

/**
 *
 * @author Jonas
 */
public class Evaluator {
    public static float evaluateBoardPosition(Field startOfJinxGraph, Field endOfJinxGraph,
            Field startOfOpponentGraph, Field endOfOpponentGraph, boolean isVertsMove, int pointsByJinx, int pointsByOpponent){
            //Evaluates the current state of the board.
            //The higher the result is, the better is the situation for jinx
            float result;


            //First (since 0.01) evaluation method: points by jinx/points by opponent
            if(pointsByOpponent != 0){
                    result = pointsByJinx/(float)(pointsByOpponent);
//                        result = -pointsByOpponent;
            }else{
                    result = pointsByJinx*1.1f;
            }
            
            //so 16:8 is worse than 17:9 
//            if(pointsByJinx > pointsByOpponent){
//                result = 1.1f * pointsByJinx - pointsByOpponent;
//                
//            }else if(pointsByJinx == pointsByOpponent){
//                result = pointsByJinx - pointsByOpponent;
//                
//            }else{
//                result = pointsByJinx - 1.1f * pointsByOpponent;
//            }
            
            
//            result += evaluateCurrentConflictzone(startOfJinxGraph, endOfJinxGraph, 
//                    startOfOpponentGraph, endOfOpponentGraph, isVertsMove)?0.1:-0.1;
//            
            
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

    private static Field pV;//vertical (playing) point of conflict zone (translated to the down-left corner equivalent)
    private static Field pH;//horizontal (playing) point of conflict zone (translated to the down-left corner equivalent)
    static boolean evaluateCurrentConflictzone(Field startOfJinxGraph, Field endOfJinxGraph,
            Field startOfOpponentGraph, Field endOfOpponentGraph, boolean isVertsMove){
        //find conflictzone and set the 2 relevant points
        //(reduce every conflictzone to a conflict in the down-left corner:
        //vertical player tries to reach the bottom and horizontal tries 
        //to reach the left border. This is necessary to use the 'hand-calculated'
        //formulas for the coordinate system; down-left corner of the board is 
        //defined as coordinate origin (0|0))
        
        calcPVAndPH(startOfJinxGraph, endOfJinxGraph, 
                startOfOpponentGraph, endOfOpponentGraph);
        
        //IT IS ALWAYS VERTS MOVE, MAKES THINGS A LOT EASIER
        if(!isVertsMove){
            Field f = pV;
            pV = new Field(pH.getY(), pH.getX());
            pH = new Field(f.getY(), f.getX());
        }
        //now it is verts move ;)
        
        System.out.println("pV = " + pV + " and pH = " + pH);
        
        
        
//        System.out.println("Vert is faster: " +
//                verticalLineIsFaster(0.5f, new Field(5,8), 
//                        2f, new Field(7,2), true));
        /*
        There are four possible lines for each player:
        v0(x) = -0.5 * x + 0.5 * xV + yV
        v1(x) = -2   * x + 2   * xV + yV
        v2(x) = 2    * x - 2   * xV + yV
        v3(x) = 0.5  * x - 0.5 * xV + yV
        for the vertical player (v0 would be best, v3 worst) and
        
        h0(x) = -2   * x + 2   * xH + yH
        h1(x) = -0.5 * x + 0.5 * xH + yH
        h2(x) =  0.5 * x - 0.5 * xH + yH
        h3(x) =  2   * x - 2   * xH + yH
        for the horizontal player (h0 would be best, h3 worst).
        
        This function now figures out which is the best line for the
        CURRENT player that the other cannot beat (be faster at the intersection point
        with any of his lines lines). If there is no such line it figures out 
        wich is the best line for the other player. 
        There can be 3 different results:
        1) Current player has a line that beats all opponent lines
        2) Other player has a line that beats all lines of current player
        3) Both players have a line that cannot be beaten by the opponent
           -> parallel
        
        */
        
        int minNotBeatableLineByOpponent = -1;
        int minNotBeatableLineByPlayer = -1;
        
        //find minNotBeatableLineByPlayer (is always playing vertical, look above)
        for(int i=0; i<4; i++){
//            System.out.println("i = " + i);
            if(findBeaterLineFor(i, true) == -1){
                minNotBeatableLineByPlayer = i;
                System.out.println("Not beatable line = " + i);
                break;
            }
        }
        
        //find minNotBeatableLineByOpponent (is playing horizontal)
        for(int i=0; i<4; i++){
//            System.out.println("i = " + i);
            if(findBeaterLineFor(i, false) == -1){
                minNotBeatableLineByOpponent = i;
                System.out.println("Not beatable line opponent = " + i);
                break;
            }
        }
        
        //TODO: Improve, rethink!
        if(minNotBeatableLineByOpponent != -1 && minNotBeatableLineByPlayer != -1){
            //the found best lines are parallel
            float pPlayer = calcPointsWithLine(minNotBeatableLineByPlayer, pV, true);
            System.out.println("pPlayer = " + pPlayer);
            
            float pOpponent = calcPointsWithLine(minNotBeatableLineByOpponent, pH, false);
            System.out.println("pOpponent = " + pOpponent);
            return pPlayer > pOpponent;
        }else if(minNotBeatableLineByPlayer != -1){
            float pPlayer = calcPointsWithLine(minNotBeatableLineByPlayer, pV, true);
            System.out.println("pPlayer = " + pPlayer);
            return pPlayer>=0;
            
        }else if(minNotBeatableLineByOpponent != -1){
            float pOpponent = calcPointsWithLine(minNotBeatableLineByOpponent, pH, false);
            System.out.println("pOpponent = " + pOpponent);
            return pOpponent>=0;
            
        }else{
            
            //should not be possible
            assert(false);
            return false;
        }
    }
    
    private static void calcPVAndPH(Field startOfJinxGraph, Field endOfJinxGraph,
            Field startOfOpponentGraph, Field endOfOpponentGraph){
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
//            System.out.println("vert up-left");
            
//  ---------------- TODO! ---------------------
//            if(startOfJinxGraph == endOfJinxGraph || startOfOpponentGraph == endOfOpponentGraph)
            
            //check startJinx to endOpponent (conflictzone: up-right)
            help = getSquaredDistance(startOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - startOfJinxGraph.getX(),    startOfJinxGraph.getY());
                pH = new Field(23 - endOfOpponentGraph.getX(),  endOfOpponentGraph.getY());
//                System.out.println("vert up-right");
            }

            //check endJinx to startOpponent (conflictzone: down-left)
            help = getSquaredDistance(endOfJinxGraph, startOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(endOfJinxGraph.getX(),       23 - endOfJinxGraph.getY());
                pH = new Field(startOfOpponentGraph.getX(), 23 - startOfOpponentGraph.getY());
//                System.out.println("vert down-left");
            }

            //check endJinx to endOpponent (conflictzone: down-right)
            help = getSquaredDistance(endOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - endOfJinxGraph.getX(),     23 - endOfJinxGraph.getY());
                pH = new Field(23 - endOfOpponentGraph.getX(), 23 - endOfOpponentGraph.getY());
//                System.out.println("vert down-right");
            }
            
            
        }else{//jinx is playing horizontal
            //set first to startJinx to startOpponent (conflictzone: up-left)
            minSquaredDistance = getSquaredDistance(startOfJinxGraph, startOfOpponentGraph);
            pV = startOfOpponentGraph;
            pH = startOfJinxGraph;
//            System.out.println("hor up-left");
        
            //check startJinx to endOpponent (conflictzone: down-left)
            help = getSquaredDistance(startOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(endOfOpponentGraph.getX(), 23 - endOfOpponentGraph.getY());
                pH = new Field(startOfJinxGraph.getX(),   23 - startOfJinxGraph.getY());
//                System.out.println("hor down-left");
                
            }

            //check endJinx to startOpponent (conflictzone: up-right)
            help = getSquaredDistance(endOfJinxGraph, startOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - startOfOpponentGraph.getX(), startOfOpponentGraph.getY());
                pH = new Field(23 - endOfJinxGraph.getX(),       endOfJinxGraph.getY());
//                System.out.println("hor up-right");
            }

            //check endJinx to endOpponent (conflictzone: down-right)
            help = getSquaredDistance(endOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - endOfOpponentGraph.getX(),  23 - endOfOpponentGraph.getY());
                pH = new Field(23 - endOfJinxGraph.getX(),      23 - endOfJinxGraph.getY());
//                System.out.println("hor down-right");
            }
        }

//        System.out.println("pV: " + pV + " pH: " + pH);
        
    }
    
    private static final float[] mV = {
        -0.5f, -2, 2, 0.5f
    };
    private static final float[] mH = {
        -2, -0.5f, 0.5f, 2
    };
    
    private static int findBeaterLineFor(int i, boolean isVertLine){
        /*
        assert(i>=0 && i<4)
        There are four possible lines for each player:
        v0(x) = -0.5 * x + 0.5 * xV + yV
        v1(x) = -2   * x + 2   * xV + yV
        v2(x) = 2    * x - 2   * xV + yV
        v3(x) = 0.5  * x - 0.5 * xV + yV
        for the vertical player (v0 would be best, v3 worst) and
        
        h0(x) = -2   * x + 2   * xH + yH
        h1(x) = -0.5 * x + 0.5 * xH + yH
        h2(x) =  0.5 * x - 0.5 * xH + yH
        h3(x) =  2   * x - 2   * xH + yH
        for the horizontal player (h0 would be best, h3 worst).
        */
        
        //TODO: parallel lines
        
        if(isVertLine){
//            System.out.println("player tries against 0");
            if(-1 == verticalLineIsFaster(mV[i], pV, mH[0], pH))
                return 0;
//            System.out.println("player tries against 1");
            if(-1 == verticalLineIsFaster(mV[i], pV, mH[1], pH))
                return 1;
//            System.out.println("player tries against 2");
            if(-1 == verticalLineIsFaster(mV[i], pV, mH[2], pH))
                return 2;
//            System.out.println("player tries against 3");
            if(-1 == verticalLineIsFaster(mV[i], pV, mH[3], pH))
                return 3;
//            System.out.println("player beats opponent");
            return -1; // vI beats every 'h-line'
        }else{
//            System.out.println("opponent tries against 0 + m = " + mH[i]);
            if(1 == verticalLineIsFaster(mV[0], pV, mH[i], pH))
                return 0;
//            System.out.println("opponent tries against 1");
            if(1 == verticalLineIsFaster(mV[1], pV, mH[i], pH))
                return 1;
//            System.out.println("opponent tries against 2");
            if(1 == verticalLineIsFaster(mV[2], pV, mH[i], pH))
                return 2;
//            System.out.println("opponent tries against 3");
            if(1 == verticalLineIsFaster(mV[3], pV, mH[i], pH))
                return 3;
//            System.out.println("opponent beats player");
            return -1; // hI beats every 'v-line'
        }
    }
    
    /*figures out, which line would win (be faster at the intersection
      of both). both lines are defined with m and b this way:
      v(x) = mV * x + bV
      h(x) = mH * x + bH
      
      It is the turn of one line, so this line is 2^2 + 1^1 = 5 ahead
      (in squared distance; everything is measured in squared distance to
      avoid calculation which the square root which is performance intensive)
      
      If you do the math you get the following formulas for the intersection point
      (sX | sY):
      sX = (bH - bV) / (mV - mH)  
      sY = mV * (bH - bV)/(mV - mH) + bV
            for mV - mH != 0; otherwise the lines are parallel
    
      This method first has to check if one line intersects the (already existing)
      other graph (it is assumed that the vert graph goes staright upwards from
      fOfVertLine and the hor graph goes straight rightwards from fOfHorLine).
      If so, the other player's line is obviously faster (if both, return 0)
      Formular to check:
      if(h(xV) = mH * xV + bH >= yV) -> hor line intersects vert graph
      if((yH - bV) / mV       >= xH) -> vert line intersects hor graph
    
      If no graph is intersected, then the intersection point of v (vert line)
      and h (hor line) is calculated, the distances to the end points are compared 
      and the faster player is returned (1 -> vert is faster; 
      false -> hor is faster)
      If h and v are parallel, then 
    */
    private static int verticalLineIsFaster(float mV, Field fOfVertLine,
            float mH, Field fOfHorLine){
        float bV = fOfVertLine.getY() - mV * fOfVertLine.getX();
        float bH = fOfHorLine.getY() - mH * fOfHorLine.getX();
        
        //check for intersection of graph
        boolean vertLineIntersectsHorGraph = false;
        boolean horLineIntersectsVertGraph = false;
        if((fOfHorLine.getY() - bV) / mV >= fOfHorLine.getX()){
//            System.out.println("vert line intersects hor graph");
            vertLineIntersectsHorGraph = true;
        }
        if(mH * fOfVertLine.getX() + bH >= fOfVertLine.getY()){
//            System.out.println("hor line intersects vert graph");
            horLineIntersectsVertGraph = true;
        }
        if(horLineIntersectsVertGraph && vertLineIntersectsHorGraph)
            return 0;
        
        else if(horLineIntersectsVertGraph){
            return 1;
            
        }else if(vertLineIntersectsHorGraph){
            return -1;
        }
        
        //check for parallelism
        if(mV == mH){
//            System.out.println("lines are parallel");
           return 0; 
        }
        
        //calc intersection point
        float sX = (bH - bV) / (mV - mH);
        float sY = mV * (bH - bV)/(mV - mH) + bV;
        
        //check if S is in the 'relevant zone':
        //1. it has to be on the board (sX > 0 && sY > 0; equal to 0 means that
        //it cannot be a intersection point, because it is only playable for one)
        //2. it has to be 'below' fOfVertLine and fOfHorLine, not on the irrelevant
        //part of the board 'behind' the conflictzone, e. g. in (23|23).
        //So sY < fOfVertLine.getY() && sX < fOfHorLine.getX()
        if(sX <= 0 || sY <= 0 || sY >= fOfVertLine.getY() || sX >= fOfHorLine.getX()){
//            System.out.println("intersection point is not in relevant area);
            return 0;//equivalent to parallelism for further calculations
        }
        
//        System.out.println("Intersection point: (" + sX + "|" + sY + ")");
        
        //calc distances
//        float distFVertToS = getSquaredDistance(fOfVertLine, sX, sY);
//        float distFHorToS  = getSquaredDistance(fOfHorLine, sX, sY);
        
        
        float deltaXVert = Math.abs(sX - fOfVertLine.getX());
        float deltaYVert = Math.abs(sY - fOfVertLine.getY());
        int neededMovesVert = (int) Math.ceil(Math.min(deltaXVert, deltaYVert));
//        System.out.println("needed moves vert: " + neededMovesVert);
        float deltaXHor = Math.abs(sX - fOfHorLine.getX());
        float deltaYHor = Math.abs(sY - fOfHorLine.getY());
        int neededMovesHor = (int) Math.ceil(Math.min(deltaXHor, deltaYHor));
//        System.out.println("needed moves hor: " + neededMovesHor);
        
        //It is always verts turn (look at beginning of evaluateCurrentConflictzone)
        if(neededMovesVert <= neededMovesHor)
            return 1;
        else
            return -1;
    }
    
    private static float calcPointsWithLine(int index, Field f, boolean isVert){
        if(isVert){
            //calc intersection point with x axis -> 'Nullstelle'
            return (mV[index] * f.getX() - f.getY()) / mV[index];
        }else{
            //calc intersection point with y axis -> 'Y-Achsenabschnitt'
            return f.getY() - mH[index] * f.getX();
        }
    }
    
    private static float getSquaredDistance(Field a, Field b){
        return (float) (Math.pow(Math.abs(b.getX() - a.getX()), 2) + 
                Math.pow(Math.abs(b.getY() - a.getY()), 2));
    }
    
    private static float getSquaredDistance(Field a, float bX, float bY){
        return (float) (Math.pow(Math.abs(bX - a.getX()), 2) + 
                Math.pow(Math.abs(bY - a.getY()), 2));
    }
    
    private static float getDistance(Field a, float bX, float bY){
        return (float) Math.sqrt(Math.pow(Math.abs(bX - a.getX()), 2) + 
                Math.pow(Math.abs(bY - a.getY()), 2));
    }
}
