/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.iotgateway;

import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.RemoteControllerTwin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.buurmeijermile.smilesware.services.opcua.utils.Waiter;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class MqttController implements MqttMessageListener, Runnable{
    
    private static final Logger LOGGER = Logger.getLogger( MqttController.class.getName());

    private final IoTDeviceMqttClient mqttClient;
    private final String commandTopic = "IntelligentIndustryExperience/controller/set/command";
    private final String informationModelTopic = "IntelligentIndustryExperience/controller/get/informationModel";
    public final RemoteControllerCommandMessage getInformationModelCommand = new RemoteControllerCommandMessage(commandTopic, "{ \"get\": \"model\" } "); // any JSON Object would do for now
    private final Queue<MqttTopicMessage> messageQueue = new LinkedList<>();
//    private RemoteControllerTwin controllerTwin = null;
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean running = false;
    private List<StatusUpdateListener> statusUpdateListeners = new ArrayList<>();
    private List<DeviceDiscoveryListener> deviceDiscoveryListeners = new ArrayList<>();
    
    public MqttController() {
        this.mqttClient = new IoTDeviceMqttClient();
        this.mqttClient.initMqtt();
    }
    
    public void addStatusUpdateListener( StatusUpdateListener aStatusUpdateListener) {
        this.statusUpdateListeners.add( aStatusUpdateListener);
    }
    
    public void addDeviceDiscoveryListener( DeviceDiscoveryListener aDeviceDiscoveryListener) {
        this.deviceDiscoveryListeners.add(aDeviceDiscoveryListener);
    }
    
    public void init() {
        LOGGER.log(Level.INFO, "Initializing mqttController");
        this.mqttClient.addMqttMessageListener( this);
        this.mqttClient.createSubscription( informationModelTopic);
    }
    
    public void sendControllerCommand( String aCommand) {
        this.mqttClient.publishMessage( commandTopic, aCommand);
    }
    
    public void createStatusUpdateSubscriptions( Set<String> topics) {
        LOGGER.log( Level.INFO, "Subscription topics:" + topics);
        topics.stream().forEach(topic -> this.mqttClient.createSubscription( topic));
    }
    
    public void sendRequest( RemoteControllerCommandMessage command) {
        this.mqttClient.publishMessage(
                command.getTopic(), 
                command.getCommand()
        );
    }
    
    private void processMessage( MqttTopicMessage mqttMessage) {
        LOGGER.log( Level.INFO, "MqttMessage received:" + mqttMessage);
        String messageTopic = mqttMessage.getTopic();
        switch ( messageTopic) {
            case informationModelTopic: {
                String jsonInformationModel = mqttMessage.getMessage();
                LOGGER.log( Level.INFO, "json formatted information model received from target device:");
                LOGGER.log( Level.INFO, jsonInformationModel);
                try {
                    RemoteControllerTwin controllerTwin = mapper.readValue(jsonInformationModel, RemoteControllerTwin.class);
                    LOGGER.log( Level.INFO, "Twin created for remote iot device controller");
                    for (DeviceDiscoveryListener listener : this.deviceDiscoveryListeners ) {
                        listener.receiveDeviceDiscoveryEvent( controllerTwin);
                    }
                } catch (JsonProcessingException ex) {
                    LOGGER.log(Level.SEVERE, "Error processing the received information model", ex);
                }
                break;
            }
            default: {
                this.statusUpdateListeners.forEach( 
                        listener -> {
                            listener.receiveUpdate( messageTopic, mqttMessage.getMessage());
                        }
                );
                break;
            }
        }
    }
    
    @Override
    public void run() {
        // first request information model from controllers that are listening to this topic
        this.sendRequest( getInformationModelCommand);
        LOGGER.log(Level.INFO, "Information model requested");
        // then process the message queue for ever
        while (true) {
            MqttTopicMessage message = this.messageQueue.poll();
            if (message != null) {
                processMessage(message);
            }
            Waiter.waitMilliSeconds(10); // wait 10 milliseconds
        }
    }
    
    public void start() {
        running = true;
        Thread thread = new Thread( this);
        thread.start();
    }

    @Override
    public void receiveMqttMessage(MqttTopicMessage message) {
        // just add it to the message queue so that mqttclient can continue and
        // this MqttController thread can process the messages in its own pace
        LOGGER.log(Level.INFO, "Message received adding it to the queue");
        LocalDateTime currentTime = LocalDateTime.now();
        Duration deltaTime = Duration.ZERO;
        // check if message fits in the queue (and work with timout)
        while (!this.messageQueue.offer(message) && deltaTime.compareTo(Duration.ofMillis(1000)) > 0 ) {
            deltaTime = Duration.between(currentTime, LocalDateTime.now());
        }
    }
    
    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @param running the running to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
    
    public void stop() {
        this.running = false;
    }

    public void publishMessage(String setTopic, Object value) {
        this.mqttClient.publishMessage( setTopic, value.toString());
    }
}
