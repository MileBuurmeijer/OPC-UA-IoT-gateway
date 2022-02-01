/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.DeviceDiscoveryListener;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.MqttController;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.RemoteControllerCommandMessage;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.StatusUpdateListener;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.RemoteControllerTwin;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Controller;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Sensuator;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Parameter;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.ActionCommand;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.Task;
import name.buurmeijermile.smilesware.services.opcua.utils.Waiter;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class IoTDeviceBackendController implements DeviceDiscoveryListener, StatusUpdateListener, AttributeObserver, Runnable {

    private static final Logger LOGGER = Logger.getLogger( IoTDeviceBackendController.class.getName());
    
    private final MqttController mqttController;
    private final ObjectMapper mapper = new ObjectMapper();

    private RemoteControllerTwin controllerTwin;
    private List<RemoteControllerTwin> remoteControllerTwinList = new ArrayList<>();
    private final Map<String, Parameter> remoteControllerProperties = new HashMap<>();
    private final ZoneOffset zoneOffset;
    private List<RemoteControllerCommand> receivedRemoteControllerCommands = new ArrayList<>();
    private TaskController taskController;
    
    public IoTDeviceBackendController() {
        ZonedDateTime timezoneDateTime = ZonedDateTime.now(); // only used to retrieve platform timezone
        this.zoneOffset = ZoneOffset.from( timezoneDateTime); // timezone offset of runtime platform
        this.mqttController = new MqttController();
    }
    
    public void init() {
        // subscribe to mqtt messages
        this.mqttController.addStatusUpdateListener(this);
        this.mqttController.addDeviceDiscoveryListener(this);
        mqttController.init();
        mqttController.start();
        // wait for the remote IoT controller twins to appear (those will occur when iotdevices report their information models to the MqttController)
        LOGGER.log( Level.INFO, "IoTDeviceBackendController waiting for controllers to report information models");
        Waiter.waitADuration( Duration.ofSeconds(20)); // wait 20 seconds for them to respond
        LOGGER.log( Level.INFO, "IoTDeviceBackendController waiting passed");
        // gather all properties of the reported IoT controllers
        this.initRemoteControllerProperties();
        // subscribe to all relevant update topics in the information model of the remote device
        this.mqttController.createStatusUpdateSubscriptions(remoteControllerProperties.keySet());
        LOGGER.log( Level.INFO, "IoTDeviceBackendController initialized");
        // start seperate thread for the active runtime work of this controller
        Thread backendThreadhread = new Thread(this);
        backendThreadhread.start();
        // create task controller for dealing with tasks and action for the remote controller (and the learning procedure)
        taskController = new TaskController( this);
        // initialize it with its capability set of tasks that it understands
        taskController.initialize();
        // and its operational thread
        Thread taskControllerThread = new Thread( taskController);
        taskControllerThread.start();
    }
    
    public void initRemoteControllerProperties() {
        LOGGER.log( Level.INFO, "Initializing remote controller properties");
        // per devicetwin that reported itself
        for ( RemoteControllerTwin aRemoteControllerTwin : this.getRemoteControllerTwinList()) {
            // get the underlying controller object
            Controller controller = aRemoteControllerTwin.getController();
            // create a state parameter for in the variable properties
            String controllerStateTopic = controller.getStateTopic();
            String controllerCommandTopic = controller.getCommandTopic();
            Parameter stateParameter = new Parameter();
            stateParameter.setName(Controller.STATEKEYWORD);
            stateParameter.setGetTopic(controllerStateTopic);
            stateParameter.setSetTopic(controllerCommandTopic);
            LOGGER.log( Level.INFO, "Adding parameter[" + stateParameter.getName() + "] with getTopic[" + stateParameter.getGetTopic() + "] and setTopic[" + stateParameter.getSetTopic() + "]");
            this.remoteControllerProperties.put(controllerStateTopic, stateParameter);
            // iterate through all devices from this controller
            for (Sensuator sensuator : controller.getSensuators()) {
                // add dynamic properties to the device
                for (Parameter parameter : sensuator.getParameters()) {
                    // add both the get and set properties so that this map
                    // can be use to lookup the get and set topics
                    this.remoteControllerProperties.put( parameter.getGetTopic(), parameter);
                    this.remoteControllerProperties.put( parameter.getSetTopic(), parameter);
                    LOGGER.log( Level.INFO, "Adding parameter[" + parameter.getName() + "] with getTopic[" + parameter.getGetTopic() + "] and setTopic[" + parameter.getSetTopic() + "]");
                }
            }
        }
        LOGGER.log( Level.INFO, "Remote Controller properties initialized");
    }

    /**
     * This back end controller listens to device discovery events to build up the OPC UA model
     * for this OPCU UA server as well as being able to interact with the remote device controller.
     * @param aTwin 
     */
    @Override
    public void receiveDeviceDiscoveryEvent(RemoteControllerTwin aTwin) {
        LOGGER.log( Level.INFO, "DeviceControllerTwin received");
        this.getRemoteControllerTwinList().add(aTwin);
    }

    @Override
    public void receiveUpdate(String topic, String value) {
        // find the property belonging to this topic, as used at subscribing to these topics
        LOGGER.log( Level.INFO, "Received update: topic[" + topic + "]=" + value);
        Parameter parameter = this.remoteControllerProperties.get(topic);
        UaVariableNode uaVariableNode;
        if (parameter != null) {
            // check if it is status of controller instead of real parameters of sensors / actuators
            if (parameter.getName().contentEquals(Controller.STATEKEYWORD)) {
                // get the controller of which state we received an state update
                Controller controller = this.getControllerByTopic(topic);
                if (controller != null) {
                    // update the the corresponding OPC UA variable node in the namespace of the OPC UA Server
                    uaVariableNode = controller.getUaVariableNode();
                    // process the message to so that the received controller state is captured
                    this.processRemoteControllerState( topic, value);
                } else {
                    uaVariableNode = null;
                }
            } else {
                uaVariableNode=parameter.getUaVariableNode();
            }
            // get the UA variable to set its value
            if (uaVariableNode != null) {
                Variant variant = new Variant( value); // TODO set proper data type
                LocalDateTime timestamp = LocalDateTime.now();
                DateTime now = this.getUaDateTime();
                DataValue dataValue = new DataValue( variant, StatusCode.GOOD, now, now);
                uaVariableNode.setValue( dataValue);
                LOGGER.log( Level.INFO, "Parameter " + parameter.getGetTopic() + " gets value " + dataValue + " set");
            } else {
                LOGGER.log( Level.WARNING, "Controller state not found belonging to this topic " +  topic);
            }
        } else {
            // check the non parametric topics that this back end controller is subscribed to
            LOGGER.log( Level.WARNING, "Parameter not found belonging to this topic " +  topic);
        }
    }
    
    private void processRemoteControllerState(String topic, String value) {
        this.taskController.processMessage(topic, value);
    }

    public String receiveRemoteControlCommand( RemoteControllerCommand aControllerCommand) {
        this.receivedRemoteControllerCommands.add( aControllerCommand);
        return "command request received";
    }
    
    @Override
    public void run() {
        while (true) {
            if (!this.receivedRemoteControllerCommands.isEmpty()) {
                for (RemoteControllerCommand aControllerCommand :  receivedRemoteControllerCommands) {
                    processRemoteControllerCommand( aControllerCommand);
                }
                this.receivedRemoteControllerCommands.clear();
            }
            Waiter.waitMilliSeconds(300);
        }
    }
    
    private void processRemoteControllerCommand( RemoteControllerCommand aControllerCommand) {
        Controller controller = aControllerCommand.getController();
        String command = aControllerCommand.getCommand();
        int requestTaskResult = 0;
        switch (command) {
            case "1": { 
                requestTaskResult = this.taskController.requestTask( "move-item-1");
                break;
            }
            case "2": {
                requestTaskResult = this.taskController.requestTask( "move-item-2");
                break;
            }
            case "3": {
                requestTaskResult = this.taskController.requestTask( "move-item-3");
                break;
            }
            default: {
                LOGGER.log( Level.WARNING, "This command is not understood yet");
            }
        }
        LOGGER.log(Level.INFO, "Task request result = {0}", requestTaskResult);
    }
        
    public void sendRemoteCommand( ActionCommand anActionCommand) {
        String jsonCommand = "";
        try {
            // prep action command for setting the state of the remote controller
            anActionCommand.setSet("state");
            // map the controller-state part of the action command
            jsonCommand = mapper.writeValueAsString( anActionCommand);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(IoTDeviceBackendController.class.getName()).log(Level.SEVERE, null, ex);
        }
        LOGGER.log(Level.INFO, "jsonCommand = {0}", jsonCommand);
        if (!jsonCommand.contentEquals("")) {
            // find the controller from the remote information model objects
            Controller aController = this.getControllerById( anActionCommand.getControllerState().getControllerId());
            if (aController != null) {
                // create a remote controller command meesage
                RemoteControllerCommandMessage commandMessage = new RemoteControllerCommandMessage( aController.getCommandTopic(), jsonCommand);
                // and ask mqtt controller to send it
                this.mqttController.sendRequest(commandMessage);
                LOGGER.log(Level.INFO, "jsonCommand send to mqtt controller");
            } else {
                LOGGER.log(Level.WARNING, "Controller not found to send message to");
            }
        } else {
            LOGGER.log( Level.WARNING, "Empty string after JSON object mapping");
        }
    }

    private Controller getControllerByTopic( String topic) {
        for ( RemoteControllerTwin aDeviceTwin : this.getRemoteControllerTwinList()) {
            // get the underlying controller object
            Controller controller = aDeviceTwin.getController();
            if (controller.getStateTopic().contentEquals(topic)) {
                return controller;
            }
        }
        return null;
    }

    private Controller getControllerById( String controllerId) {
        for (RemoteControllerTwin aTwin : this.getRemoteControllerTwinList()) {
            Controller controller = aTwin.getController();
            if (controller.getId().contentEquals(controllerId)) {
                return controller;
            }
        }
        return null;
    }

    private DateTime getUaDateTime() {
        LocalDateTime timestamp = LocalDateTime.now();
        // converts the todays LocalDateTime timestamp to an OPC UA DateTime timestamp (tedious steps) with millisecond precision
        // first get UTC seconds
        long javaUtcMilliSeconds = timestamp.toEpochSecond( this.zoneOffset);
        // then multiply by 1000 to get number of milliseconds since epoch
        javaUtcMilliSeconds = javaUtcMilliSeconds * 1000;
        // then calculate milliseconds based on internal nano seconds
        long javaMillisFraction = timestamp.getNano() / 1000_000;
        // the add these values to get the utc in milliseconds
        long javaUtcMilli = javaUtcMilliSeconds + javaMillisFraction;
        // create java Date out of this utc in millseconds since epoch
        Date javaDate = new Date(javaUtcMilli);
        // create java date out of that
        DateTime dateTime = new DateTime(javaDate);
        return dateTime;
    }

    /**
     * @return the remoteControllerProperties
     */
    public Map<String, Parameter> getRemoteControllerProperties() {
        return remoteControllerProperties;
    }

    @Override
    public void attributeChanged(UaNode nodeId, AttributeId attributeId, Object value) {
        LOGGER.log( Level.INFO, "attribute observer called with nodeId=" + nodeId + ", attributedId=" + attributeId + ", value" + value);
        Parameter parameter = this.deriveParameter( nodeId);
        if (parameter != null) {
            String valueString = ((DataValue) value).getValue().getValue().toString();
            String setTopic = parameter.getSetTopic();
            LOGGER.log( Level.INFO, "About to publish to topic " + setTopic + " and value " + valueString);
            this.mqttController.publishMessage( setTopic, valueString);
        } else {
            LOGGER.log( Level.WARNING, "Attribute not found between parameters in created information model of this OPC UA server");
        }
    }
    
    private Parameter deriveParameter( UaNode aNode) {
        // get identifier of this node
        String nodeIdentifier = aNode.getNodeId().getIdentifier().toString();
        // find in map with topic parameter entries the one that matches with the node identifier
        for (Entry entry : this.remoteControllerProperties.entrySet()) {
            String topic = (String) entry.getKey();
            if (topic.endsWith( nodeIdentifier)) {
                Parameter parameter = (Parameter) entry.getValue();
                return parameter;
            }
        }
        return null;
    }

    /**
     * @return the remoteControllerTwinList
     */
    public List<RemoteControllerTwin> getRemoteControllerTwinList() {
        return remoteControllerTwinList;
    }
}
