package sc.player2016.logic;

import java.util.ArrayList;

public class Field{
	//green is default in order to increase speed in initBoard()
	private Jinx.FieldColor fieldColor = Jinx.FieldColor.GREEN;
	
	private int x, y;
	
	private ArrayList<Field> connections = new ArrayList<Field>();
	
	public Field(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public void addConnectionTo(Field f){
		connections.add(f);
	}
	
	public void removeConnectionTo(Field f){
		boolean isRemoved = false;
                connections.remove(f);
//		for(Field field : connections){
//			if(field.equals(f)){
//				connections.remove(field);
//				isRemoved = true;
//				break;
//			}
//		}
//		if(!isRemoved){
//			System.out.println("No connection from (" + x + ", " + y + ") to (" + f.getX() + ", " + f.getY() + ") " + " exists!");
//		}
	}
	
        public void removeAllConnections(){
            //also remove connections TO this field
            for(Field c : connections){
                c.removeConnectionTo(this);
            }
            connections.clear();
        }
                
	public ArrayList<Field> getConnections(){
		return connections;
	}
	
	//-----------------probably just works for the SAME object (ame reference) 
	public boolean isConnectedWith(Field field){
		for(Field f : connections){
			if(f.equals(field))
				return true;
		}
		return false;
	}
	
	public boolean isConnectedWith(int fieldX, int fieldY){
            for(Field f : connections){
                if(f.x == fieldX && f.y == fieldY)
                    return true;
            }
            return false;
	}
	
	public void setFieldColor(Jinx.FieldColor fieldColor){
		this.fieldColor = fieldColor;
	}
	
	public Jinx.FieldColor getFieldColor(){
		return fieldColor;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
        
        @Override
        public String toString(){
            return "(" + x + "|" + y + ")";
        }
	
}
