/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.datasource;

import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.Controller;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class RemoteControleCommand {
    private final Controller controller;
    private final String command;
    
    public RemoteControleCommand( Controller aController, String aCommand) {
        this.controller = aController;
        this.command = aCommand;
    }

    /**
     * @return the controller
     */
    public Controller getController() {
        return controller;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }
}
