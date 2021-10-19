/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.iotgateway;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class Command {
    private final String command;
    private final String topic;
    
    public Command( String aTopic, String aJsonCommandString) {
        command = aJsonCommandString;
        topic = aTopic;
    }
    
    public String getCommand() {
        return command;
    }

    public String getTopic() {
        return topic;
    }
}

//TODO: add command validation rules
