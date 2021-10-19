/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class StatusUpdateProperty {
    
    private final Parameter parameter;
    
    public StatusUpdateProperty( Parameter aParameter) {
        this.parameter = aParameter;
    }

    /**
     * @return the propertyName
     */
    public String getName() {
        return this.parameter.getName();
    }

    /**
     * @return the propertyTopic
     */
    public String getGetTopic() {
        return this.parameter.getGetTopic();
    }
    /**
     * @return the propertyTopic
     */
    public String getSetTopic() {
        return this.parameter.getSetTopic();
    }
}
