/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.datasource;

import java.util.ArrayList;
import java.util.List;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.ActionCommand;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class TempTask {
    
    private final int controllerId;
    private final String name;
    private final int number;
    private List<ActionCommand> actionCommandSetList = new ArrayList<>();
    
    public TempTask( String aName, int aNumber, int aControllerId) {
        controllerId = aControllerId;
        name= aName;
        number = aNumber;
    }
    
    public TempTask( String aStateName, int aControllerId) {
        controllerId = aControllerId;
        if (aStateName != null && aStateName.contains(".")) {
            String stateName = convertStateNameToParts( aStateName, 1);
            number = getNumber( stateName); // get number part of name
            name = stateName.substring(0, stateName.indexOf("[")); // strip number part of name
        } else {
            name ="error";
            number = -1;
        }
    }
    
    private int getNumber( String aNumberedString) {
        if (aNumberedString == null || aNumberedString.length()==0 || aNumberedString.indexOf("[") == 0) {
            return 0;
        }
        String [] parts = aNumberedString.split("\\[");
        if (parts.length == 2) {
            int rightIndex = parts[1].length()-1;
            String numberString = parts[1].substring(0, rightIndex);
            return Integer.parseInt(numberString);
        } else {
            return 0;
        }
    }

    private String convertStateNameToParts( String stateName, int partNumber) {
        if (partNumber<1 || partNumber>2 || stateName == null || stateName.indexOf(".") == 0) {
            return null;
        }
        String [] parts = stateName.split("\\.");
        // check if two parts
        if (parts.length == 2) {
            return parts[partNumber-1];
        } else {
            return null;
        }
    }

    public void addCommandAction( ActionCommand aCommandAction) {
        this.actionCommandSetList.add(aCommandAction);
    }
    
    public void removeCommandAction( ActionCommand aCommandAction) {
        this.actionCommandSetList.remove( aCommandAction);
    }
    
    public void clear() {
        this.actionCommandSetList.clear();
    }
    
    public List<ActionCommand> getActions() {
        return this.actionCommandSetList;
    }

//    public ActionSet getCommandAction( int actionNumber) {
//        if ( actionNumber < this.actionCommandSetList.size()) {
//            return this.actionCommandSetList.get(actionNumber);
//        } else {
//            return null;
//        }
//    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the number
     */
    public int getNumber() {
        return number;
    }
    
    public String toString() {
        return name + number;
    }
    
//    public static void main( String [] args) {
//        TempTask command = new TempTask("commando[1].action[2]");
//        System.out.println("Commando name=" + command.getName());
//        System.out.println("Commando number=" + command.getNumber());
//    }

    /**
     * @return the controllerId
     */
    public int getControllerId() {
        return controllerId;
    }
    
    @Override
    public boolean equals( Object object) {
        boolean result = false;
        if ( object instanceof TempTask) {
            TempTask anotherTask = (TempTask) object;
            result = anotherTask.getName().contentEquals( this.name);
            result = result && anotherTask.getNumber()==this.number;
            result = result && anotherTask.getControllerId() == this.controllerId;
        }
        return result;
    }
    
}
