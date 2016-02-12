/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sc.player2016.logic;

import java.util.ArrayList;

/**
 * 
 * @author Jonas
 */
public class Graph {
    private ArrayList<Field> fields = new ArrayList<>();
    private Field minXField;
    private Field maxXField;
    private Field minYField;
    private Field maxYField;
    
    public Field getMinXField() {
        return minXField;
    }

    public Field getMaxXField() {
        return maxXField;
    }

    public Field getMinYField() {
        return minYField;
    }

    public Field getMaxYField() {
        return maxYField;
    }
    
    
    public Graph(Field firstField){
        fields.add(firstField);
        minXField = firstField;
        minYField = firstField;
        maxXField = firstField;
        maxYField = firstField;
    }
    
    //make sure that f is NOT the first field in fields!
    public void addField(Field f){
        fields.add(f);
        if      (f.getX() < minXField.getX()) minXField = f;
        else if (f.getX() > maxXField.getX()) maxXField = f;
        if      (f.getY() < minYField.getY()) minYField = f;
        else if (f.getY() > maxYField.getY()) maxYField = f;
    }
    
    //make sure that fields.size() > 2 ! Otherwise delete whole graph!
    //returns new graphs (this stays a graph, just reduced)
    public ArrayList<Graph> removeField(Field f){
        ArrayList<Graph> result = new ArrayList<>();
        
        ArrayList<Field> connections = new ArrayList<>();
        for(Field field : f.getConnections()){
            connections.add(field);
        }
        fields.remove(f);
        f.removeAllConnections();
        if(connections.size() == 1){//check if split of this graph (into 2 or
                                    //more) is not necessary
            
            if(minXField == f || maxXField == f || minYField == f || maxYField == f){
                recalculateMinAndMaxFields();
            }
        }else{//check for split of this graph into 2 or more
            for(int i=0; i<connections.size(); i++){
                //if connections.get(i) is still in origin graph (this),
                //build a new graph with connections.get(i).
                //connections.get(0) is always in origin graph, because nothing 
                //was removed before
                if(i==0 || fields.contains(connections.get(i))){                  
                    result.add(splitFromThis(connections.get(i)));
                }
            }
            //this (origin graph) CANNOT be used any more at the moment, 
            //because minX, ... are wrong. So set it to result.get(0); also
            //reset minX, ...
            minXField = result.get(0).fields.get(0);
            maxXField = result.get(0).fields.get(0);
            minYField = result.get(0).fields.get(0);
            maxYField = result.get(0).fields.get(0);
            this.addGraph(result.get(0));
            result.remove(0);
        }
        return result;
    }
    
    public boolean containsField(Field f){
        return fields.contains(f);
    }
    
    public int getPoints(boolean isVertical){
        if(isVertical){
            return maxYField.getY() - minYField.getY();
        }else{
            return maxXField.getX() - minXField.getX();
        }
    }
    
    private void recalculateMinAndMaxFields(){
        Field f = fields.get(0);
        minXField = f;
        maxXField = f;
        minYField = f;
        maxYField = f;
        for(int i=1; i<fields.size(); i++){
            f = fields.get(i);
            if      (f.getX() < minXField.getX()) minXField = f;
            else if (f.getX() > maxXField.getX()) maxXField = f;
            if      (f.getY() < minYField.getY()) minYField = f;
            else if (f.getY() > maxYField.getY()) maxYField = f;
        }
    }
    
    //splits all fields, that are still (after removing one field;
    //this method is called from removeField) in the same graph like 
    //startFieldOfPartGraph from the origin graph (this). 
    private Graph result;
    private Graph splitFromThis(Field startFieldOfPartGraph){
        result = new Graph(startFieldOfPartGraph);
        addAllFieldsInSameGraph(startFieldOfPartGraph);
        fields.remove(startFieldOfPartGraph);
        return result;
    }
    private void addAllFieldsInSameGraph(Field f){
        for(Field c : f.getConnections()){
            if(!result.containsField(c)){
                result.addField(c);
                fields.remove(c);//delete from this
                addAllFieldsInSameGraph(c);
            }
        }
    }

    public void addGraph(Graph g) {
        for(Field f : g.fields){
            addField(f);
        }
    }

    @Override
    public String toString() {
        String result = "[";
        for(int i=0; i<fields.size()-1; i++){
            result += fields.get(i) + ", ";
        }
        if(fields.size() > 0)
            result += fields.get(fields.size()-1) + "]";
        else
            result += "]";
        return result;
    }

}
