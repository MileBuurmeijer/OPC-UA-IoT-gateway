/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.iotgateway;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


/**
 * Mqtt client
 *
 * @author mbuurmei
 */
public class IoTDeviceMqttClient implements MqttCallback {

    private final int qos = 0; // guaranteed message delivery not needed
//    private final String broker = "tcp://test.mosquitto.org:1883";
    private final String broker = "tcp://192.168.68.82:1883";
    private final String clientId = "SmilesMqttController";
    private final String username = "mbuurmei";
    private final String password = "!@#$mosQ";
    private final MemoryPersistence persistence = new MemoryPersistence();
    private MqttClient mqttClient;
    private final List<MqttMessageListener> listeners = new ArrayList<>();
    
    public IoTDeviceMqttClient() {
        try {
            // create the mqtt client
            mqttClient = new MqttClient(broker, clientId, persistence);
        } catch (MqttException ex) {
            Logger.getLogger(MqttClient.class.getName()).log(Level.SEVERE, "Could not create the MQTT client", ex);
        }
    }
    
    public void addMqttMessageListener( MqttMessageListener aListener) {
        this.listeners.add(aListener);
    }
    
    public void removeMqttMessageListener( MqttMessageListener aListener) {
        this.listeners.remove(aListener);
    }

    public void initMqtt() {
        try {
            // set the callback handler to this object
            mqttClient.setCallback( this);
            // set the connection options
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName( username);
            connOpts.setPassword( password.toCharArray());
            // connect to broker
            System.out.println("Connecting to broker: " + broker);
            mqttClient.connect( connOpts);
            if (this.mqttClient.isConnected()) {
                System.out.println("Mqtt client connected");
            } else {
                System.out.println("Mqtt client not connected");
            }
        } catch (MqttException ex) {
            Logger.getLogger(MqttClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void publishMessage( String topic, String messageToSend) {
        try {
//            System.out.println("Publishing to topic: " + topic);
//            System.out.println("Publishing message: " + messageToSend);
            MqttMessage mqttMessage = new MqttMessage(messageToSend.getBytes());
            mqttMessage.setQos(qos);
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException ex) {
            Logger.getLogger(MqttClient.class.getName()).log(Level.SEVERE, "mqtt publish issue", ex);
        }
    }
    
    public void createSubscription( String topic) {
        try {
            mqttClient.subscribe( topic);
        } catch (MqttException me) {
            Logger.getLogger(MqttClient.class.getName()).log(Level.SEVERE, "mqtt subscribe issue", me);
        }
    }
    
    public void stop() {
        if (this.mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                System.out.println("Disconnected");
            } catch (MqttException ex) {
                Logger.getLogger(IoTDeviceMqttClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void sleep(int milliSleepTime) {
        try {
            Thread.sleep( milliSleepTime);
        } catch (InterruptedException ex) {
            Logger.getLogger(MqttClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String receivedMessage = message.toString().trim();
        System.out.println(topic + "=" + receivedMessage);
        for (MqttMessageListener listener : listeners) {
            MqttTopicMessage mqttTopicMessage = new MqttTopicMessage( topic, receivedMessage);
            listener.receiveMqttMessage(mqttTopicMessage);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
//        System.out.println("Delivery completed");
    }
    
    public static void main(String [] args) {
        IoTDeviceMqttClient client = new IoTDeviceMqttClient();
        client.initMqtt();
        client.stop();
    }
}
