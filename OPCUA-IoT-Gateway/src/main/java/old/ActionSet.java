/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package old;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.Parameter;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.Sensuator;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

/**
 * Encapsulated a set of actions for each device/component that a remote controller can execute. 
 * I.e. a the individual servos controlling the joints of a robot arm or the motors of a conveyor belt
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class ActionSet {

    private String actionName;
    private final Map<Sensuator, Parameter> actionMap = new HashMap<>();
    private final SetValuedMap<Sensuator, Parameter> actionMap2 = new HashSetValuedHashMap<>();
    private Duration pause; // TODO: refactor into subclass of commandaction for each class of actions
    
    public ActionSet(String aActionName) {
        this.actionName = aActionName;
    }
    
    public ActionSet() {
        
    }
    
    public void addPauseAction( Duration aPause) {
        pause = aPause;
    }
    
    public boolean hasPauseAction() {
        return pause != null;
    }
    
    public Duration getPause() {
        return pause;
    }
        
    public void addParameter( Sensuator sensuator, Parameter parameter) {
        this.actionMap.put(sensuator, parameter);
        this.actionMap2.put(sensuator, parameter);
    }

    public Set<Parameter> getParameters( Sensuator targetSensuator) {
        return this.actionMap2.get(targetSensuator);
    }
    
    public Set<Sensuator> getSensuators() {
        return this.actionMap2.keySet();
    }
    
    public void clear() {
        actionMap2.clear();
    }

    /**
     * @return the actionName
     */
    public String getActionName() {
        return actionName;
    }
}
