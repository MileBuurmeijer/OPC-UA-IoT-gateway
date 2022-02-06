/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.datasource;

import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Controller;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class TaskEvent {
    
    private final String name;
    private final Controller controller;
    
    public TaskEvent( String aName, Controller aController) {
        name=aName;
        controller = aController;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the controller
     */
    public Controller getController() {
        return controller;
    }
}
