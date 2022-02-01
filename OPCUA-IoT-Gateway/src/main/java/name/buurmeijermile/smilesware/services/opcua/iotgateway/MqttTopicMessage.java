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
public class MqttTopicMessage {
    private final String topic;
    private final String message;
    
    MqttTopicMessage( String aTopic, String aMessage) {
        this.topic = aTopic;
        this.message = aMessage;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }
}
