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
    public static float evaluateBoardPosition(int pointsByJinx, int pointsByOpponent){
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

    static float evaluateCurrentConflictzone(Field startOfJinxGraph, Field endOfJinxGraph,
            Field startOfOpponentGraph, Field endOfOpponentGraph){
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
            
//  ---------------- TODO! ---------------------
//            if(startOfJinxGraph == endOfJinxGraph || startOfOpponentGraph == endOfOpponentGraph)
            
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
    
    private static float getSquaredDistance(Field a, Field b){
        return (float) (Math.pow(Math.abs(b.getX() - a.getX()), 2) + 
                Math.pow(Math.abs(b.getY() - a.getY()), 2));
    }
    
}
