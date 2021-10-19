/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.datasource;

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
import name.buurmeijermile.smilesware.services.opcua.iotgateway.StatusUpdateListener;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.Controller;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.Device;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.DeviceControllerTwin;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.Parameter;
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
public class IoTDeviceBackendController implements DeviceDiscoveryListener, StatusUpdateListener, AttributeObserver {

    private static final Logger LOGGER = Logger.getLogger( IoTDeviceBackendController.class.getName());
    
    private final MqttController mqttController;
    private DeviceControllerTwin controllerTwin;
    private List<DeviceControllerTwin> controllerTwinList = new ArrayList<>();
    private final Map<String, Parameter> variableProperties = new HashMap<>();
    private final ZoneOffset zoneOffset;
    
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
        // wait for the device controller twins to appear (those will occur when iotdevices report their information models to the MqttController)
        LOGGER.log( Level.INFO, "IoTDeviceBackendController waiting for controllers to report information models");
        Waiter.waitADuration( Duration.ofSeconds(20)); // wait 20 seconds for them to respond
        LOGGER.log( Level.INFO, "IoTDeviceBackendController waiting passed");
        // gather all properties of all reported devices
        this.initVariableProperties();
        // subscribe to all relevant update topics in the information model of thre remote device
        this.mqttController.createStatusUpdateSubscriptions(variableProperties.keySet());
        LOGGER.log( Level.INFO, "IoTDeviceBackendController initialized");
    }

    @Override
    public void receiveDeviceDiscoveryEvent(DeviceControllerTwin aTwin) {
        LOGGER.log( Level.INFO, "DeviceControllerTwin received");
        this.controllerTwinList.add(aTwin);
    }
    
    public void initVariableProperties() {
        LOGGER.log( Level.INFO, "Initializing variable properties");
        for ( DeviceControllerTwin aDeviceTwin : this.controllerTwinList) {
            Controller controller = aDeviceTwin.getController();
            // iterate through all devices from this controller
            for (Device device : controller.getDevices()) {
                // add dynamic properties to the device
                for (Parameter parameter : device.getParameters()) {
                    // add both the get and set properties so that this map
                    // can be use to lookup get and set topics
                    this.variableProperties.put( parameter.getGetTopic(), parameter);
                    this.variableProperties.put( parameter.getSetTopic(), parameter);
                    LOGGER.log( Level.INFO, "Adding parameter[" + parameter.getName() + "] with getTopic[" + parameter.getGetTopic() + "] and setTopic[" + parameter.getSetTopic() + "]");
                }
            }
        }
        LOGGER.log( Level.INFO, "Variable properties initialized");
    }
    
    public void setPropertyValue( String topic, double value) {
        
    }
    
    public List<DeviceControllerTwin> getControllerTwins() {
        return this.controllerTwinList;
    }

    @Override
    public void receiveUpdate(String topic, String value) {
        // find the property belonging to this topic, as used at subscribing to these topics
        LOGGER.log( Level.INFO, "Received update: topic[" + topic + "]=" + value);
        Parameter parameter = this.variableProperties.get(topic);
        if (parameter != null) {
            // get the UA variable to set its value
            UaVariableNode uaVariableNode = parameter.getUaVariableNode();
            Variant variant = new Variant( value); // TODO set proper data type
            LocalDateTime timestamp = LocalDateTime.now();
            DataValue dataValue = new DataValue( variant, StatusCode.GOOD, this.getUaDateTime(), this.getUaDateTime());
            uaVariableNode.setValue( dataValue);
            LOGGER.log( Level.INFO, "Parameter " + parameter.getGetTopic() + " gets value " + dataValue + " set");
        } else {
            LOGGER.log( Level.WARNING, "Parameter not found belonging to this topic " +  topic);
        }
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
     * @return the variableProperties
     */
    public Map<String, Parameter> getVariableProperties() {
        return variableProperties;
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
        for (Entry entry : this.variableProperties.entrySet()) {
            String topic = (String) entry.getKey();
            if (topic.endsWith( nodeIdentifier)) {
                Parameter parameter = (Parameter) entry.getValue();
                return parameter;
            }
        }
        return null;
    }
}
