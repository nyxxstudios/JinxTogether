/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sc.player2016.logic;

import java.util.ArrayList;
import sc.player2016.logic.Jinx.FieldColor;

/**
 *
 * @author Jonas
 */
public class Preselector {
    
    //to calc average number of moves returned
    private static float numberOfTotalMovesReturned;
    private static int numberOfCalls = 0;
    
    public static float averageNumberOfMoves(){
        return numberOfTotalMovesReturned/numberOfCalls;
    }
    
    //all fields are written from verts point of view (x, y).
    //to use them for hor just see tham as (y, x) notated fields
    private static final int[][] goodFieldsFromOwnMin = {                    
                                                {0, -4},	
                   {-3, -3},                                              {3, -3},
//                                            {-1, -3},         {1, -3},
    //                                      {-1, -2},         {1, -2},      //already added before
                             {-2, -1},                           {2, -1},
          {-4, 0},                                                                {4, 0},
    };
    private static final int[][] goodFieldsFromOwnMax = {                    
          {-4, 0},                                                                {4, 0},
                            {-2,  1},                            {2,  1},
//                                      {-1,  2},         {1,  2},  //already added before
//                                        {-1,  3},         {1,  3},
                   {-3,  3},                                              {3,  3}, 
                                                {0,  4}
    };

    private static final int[][] goodFieldsFromEveryOwnGraphMin = {                    
                                    {-1, -2},         {1, -2},      
                             {-2, -1},                           {2, -1}
    };
    private static final int[][] goodFieldsFromEveryOwnGraphMax = {                    
              {-2,  1},                    {2,  1},
                      {-1,  2},    {1,  2},
    };
      
    private static final int[][] goodFieldsReactToOpponentMove = {
    //                                                {-1,-3}, { 0,-3}, { 1,-3}, 
                              {-3,-3},                                            { 3,-3},
                                      {-2,-1}, {-1,-1}, { 0,-1}, { 1,-1}, { 2,-1},
                      {-4, 0},        {-2, 0}, {-1, 0},          { 1, 0}, { 2, 0},                { 4, 0},
                                      {-2, 1}, {-1, 1}, { 0, 1}, { 1, 1}, { 2, 1},
                              {-3, 3},                                            { 3, 3},
    //                                                {-1, 3}, { 0, 3}, { 1, 3},
    //                                                         { 0, 4}
    };
    //important part of the Jinx AI. Returns all 'good' moves
    //that can be done (returning all possible moves would be too much
    //to calculate in a useful depth)
    public static ArrayList<Field> preselectMoves(Field lastMove, boolean isVertical, Board board){
        ArrayList<Field> result = new ArrayList<>();
        Field notCurrentPlayerMin, notCurrentPlayerMax, help;//notCurrentPlayer (can be jinx!)
        ArrayList<Graph> graphsByCurrentPlayer, graphsByNotCurrentPlayer;
        int x, y, pX, pY;
        FieldColor vertLightColor, horLightColor, black = FieldColor.BLACK;
        
        if(isVertical){//preselect for vertical player
            
            if(Jinx.jinxIsPlayingVertical){
                graphsByCurrentPlayer = board.graphsByJinx;
                graphsByNotCurrentPlayer = board.graphsByOpponent;
                vertLightColor = FieldColor.LIGHT_JINX;
            }else{
                graphsByCurrentPlayer = board.graphsByOpponent;
                graphsByNotCurrentPlayer = board.graphsByJinx;
                vertLightColor = FieldColor.LIGHT_OPPONENT;
            }

            //add (maximum) 8 fields for each graph (possible connections to other graphs)
            for(Graph g : graphsByCurrentPlayer){
//                if(g.hasJustOneField())break;
                //add fields to result for minYField
                x = g.getMinYField().getX();
                y = g.getMinYField().getY();
                for(int[] f : goodFieldsFromEveryOwnGraphMin){
                    pX = x+f[0];
                    pY = y+f[1];
                    if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                        if((board.getField(pX,pY).getFieldColor() == black ||
                                board.getField(pX,pY).getFieldColor() == vertLightColor)){
                            if(!result.contains(board.getField(pX,pY))){
                                    result.add(board.getField(pX,pY));
                            }
                        }
                    }
                }
                //add fields to result for maxYField
                x = g.getMaxYField().getX();
                y = g.getMaxYField().getY();
                for(int[] f : goodFieldsFromEveryOwnGraphMax){
                    pX = x+f[0];
                    pY = y+f[1];
                    if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                        if((board.getField(pX,pY).getFieldColor() == black ||
                                board.getField(pX,pY).getFieldColor() == vertLightColor)){
                            if(!result.contains(board.getField(pX,pY))){
                                    result.add(board.getField(pX,pY));
                            }
                        }
                    }
                }
            }

            //add fields for start and end field of own graph
            //get fields from minY
            x = graphsByCurrentPlayer.get(0).getMinYField().getX();
            y = graphsByCurrentPlayer.get(0).getMinYField().getY();
            for(int[] f : goodFieldsFromOwnMin){
                pX = x+f[0];
                pY = y+f[1];
                if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                    if((board.getField(pX,pY).getFieldColor() == black ||
                            board.getField(pX,pY).getFieldColor() == vertLightColor)){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }
            //get fields from maxY
            x = graphsByCurrentPlayer.get(0).getMaxYField().getX();
            y = graphsByCurrentPlayer.get(0).getMaxYField().getY();
            for(int[] f : goodFieldsFromOwnMax){
                pX = x+f[0];
                pY = y+f[1];
                if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                    if((board.getField(pX,pY).getFieldColor() == black ||
                            board.getField(pX,pY).getFieldColor() == vertLightColor)){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
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
                    if((board.getField(pX,pY).getFieldColor() == black ||
                            board.getField(pX,pY).getFieldColor() == vertLightColor)){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }   
            
            //add (maximum) 2 fields for each graph by opponent (endfields +- 4)
            for(Graph g : graphsByNotCurrentPlayer){
            
                //add fields from start and end of notCurrentPlayer
                if(g.getMinXField().getX() - 4 > 0){
                    help = board.getField(g.getMinXField().getX() - 4, g.getMinXField().getY());
                    if((help.getFieldColor() == black || help.getFieldColor() == vertLightColor)){
                        if(!result.contains(help)){
                            result.add(help);
                        }
                    }
                }
                if(g.getMaxXField().getX() + 4 < 23){
                    help = board.getField(g.getMaxXField().getX() + 4, g.getMaxXField().getY());
                    if((help.getFieldColor() == black || help.getFieldColor() == vertLightColor)){
                        if(!result.contains(help)){
                            result.add(help);
                        }
                    }
                }
            }
            
        }else{//preselect for horizontal player
            
            if(Jinx.jinxIsPlayingVertical){
                graphsByCurrentPlayer = board.graphsByOpponent;
                graphsByNotCurrentPlayer = board.graphsByJinx;
                horLightColor = FieldColor.LIGHT_OPPONENT;
            }else{
                graphsByCurrentPlayer = board.graphsByJinx;
                graphsByNotCurrentPlayer = board.graphsByOpponent;
                horLightColor = FieldColor.LIGHT_JINX;
            }

            //add (maximum) 8 fields for each graph (possible connections to other graphs)
            for(Graph g : graphsByCurrentPlayer){
//                if(g.hasJustOneField())break;
                //add two fields to result for minXField
                x = g.getMinXField().getX();
                y = g.getMinXField().getY();
                for(int[] f : goodFieldsFromEveryOwnGraphMin){
                    pX = x+f[1];//coordinates changed, because goodFieldsFromEveryOwnGraphMin 
                    pY = y+f[0];//are written from verts point of view (l. above)
                    if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                        if((board.getField(pX,pY).getFieldColor() == black ||
                                board.getField(pX,pY).getFieldColor() == horLightColor)){
                            if(!result.contains(board.getField(pX,pY))){
                                    result.add(board.getField(pX,pY));
                            }
                        }
                    }
                }
                //add fields to result for maxXField
                x = g.getMaxXField().getX();
                y = g.getMaxXField().getY();
                for(int[] f : goodFieldsFromEveryOwnGraphMax){
                    pX = x+f[1];//coordinates changed (l. above)
                    pY = y+f[0];
                    if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                        if((board.getField(pX,pY).getFieldColor() == black ||
                                board.getField(pX,pY).getFieldColor() == horLightColor)){
                            if(!result.contains(board.getField(pX,pY))){
                                    result.add(board.getField(pX,pY));
                            }
                        }
                    }
                }
            }

            //add fields for start and end field of own graph
            //get fields from minX
            x = graphsByCurrentPlayer.get(0).getMinXField().getX();
            y = graphsByCurrentPlayer.get(0).getMinXField().getY();
            for(int[] f : goodFieldsFromOwnMin){
                pX = x+f[1];//coordinates changed (l. above)
                pY = y+f[0];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if((board.getField(pX,pY).getFieldColor() == black ||
                            board.getField(pX,pY).getFieldColor() == horLightColor)){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }
            //get fields from maxX
            x = graphsByCurrentPlayer.get(0).getMaxXField().getX();
            y = graphsByCurrentPlayer.get(0).getMaxXField().getY();
            for(int[] f : goodFieldsFromOwnMax){
                pX = x+f[1];//coordinates changed (l. above)
                pY = y+f[0];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if((board.getField(pX,pY).getFieldColor() == black ||
                            board.getField(pX,pY).getFieldColor() == horLightColor)){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }

            //add fields from lastMove (opponent)
            x = lastMove.getX();
            y = lastMove.getY();
            for(int[] f : goodFieldsReactToOpponentMove){
                pX = x+f[1];//coordinates changed (l. above)
                pY = y+f[0];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if((board.getField(pX,pY).getFieldColor() == black ||
                            board.getField(pX,pY).getFieldColor() == horLightColor)){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }   
            
            //add (maximum) 2 fields for each graph by not current (endfields +- 4)
            for(Graph g : graphsByNotCurrentPlayer){
            
                //add fields from start and end of notCurrentPlayer (vertical)
                if(g.getMinYField().getY() - 4 > 0){
                    help = board.getField(g.getMinYField().getX(), g.getMinYField().getY() - 4);
                    if((help.getFieldColor() == black || help.getFieldColor() == horLightColor)){
                        if(!result.contains(help)){
                            result.add(help);
                        }
                    }
                }
                if(g.getMaxYField().getY() + 4 < 23){
                    help = board.getField(g.getMaxYField().getX(), g.getMaxYField().getY() + 4);
                    if((help.getFieldColor() == black || help.getFieldColor() == horLightColor)){
                        if(!result.contains(help)){
                            result.add(help);
                        }
                    }
                }
            }
        }
        numberOfTotalMovesReturned += result.size();
        numberOfCalls++;
        return result;
    }
    
    
}
