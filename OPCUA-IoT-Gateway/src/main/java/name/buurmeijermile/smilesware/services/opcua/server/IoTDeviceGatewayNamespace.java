/* 
 * The MIT License
 *
 * Copyright 2019 Milé Buurmeijer <mbuurmei at netscape.net>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package name.buurmeijermile.smilesware.services.opcua.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.buurmeijermile.smilesware.services.opcua.datasource.IoTDeviceBackendController;
import name.buurmeijermile.smilesware.services.opcua.datasource.TaskEvent;
import name.buurmeijermile.smilesware.services.opcua.datasource.TaskEventListener;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Controller;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.RemoteControllerTwin;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Parameter;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Sensuator;
import name.buurmeijermile.smilesware.services.opcua.main.Configuration;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.sdk.server.Lifecycle;
import org.eclipse.milo.opcua.sdk.server.dtd.DataTypeDictionaryManager;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ProgressEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public class IoTDeviceGatewayNamespace extends ManagedNamespaceWithLifecycle implements TaskEventListener {
    // class variables
    private static final Logger LOGGER = Logger.getLogger(IoTDeviceGatewayNamespace.class.getName());

    // instance variables
    private final SubscriptionModel subscriptionModel;
    private final OpcUaServer server;
    //private final RemoteControllerTwin deviceControllerTwin;
    private final IoTDeviceBackendController dataBackendController;
    private final RestrictedAccessFilter restrictedAccessFilter;
//    private List<UaVariableNode> variableNodes = null;
    private volatile Thread eventThread;
    private volatile boolean keepPostingEvents = true;
    private DataTypeDictionaryManager dictionaryManager;
    private List<UaFolderNode> controllerFolderNodes = new ArrayList<>();

    /**
     * The intended namespace for the OPC UA server.
     *
     * @param server the OPC UA server
     * @param anIoTBackendController the back end controller that exposes its object and properties in this namespace
     * @param configuration the configuration for this namespace
     */
    public IoTDeviceGatewayNamespace(OpcUaServer server, IoTDeviceBackendController anIoTBackendController, Configuration configuration) {

        super(server, configuration.getNamespace()); // name space from configuration
        // store parameters
        this.server = server;
        this.dataBackendController = anIoTBackendController;
        
        // create a subscription model for this server
        this.subscriptionModel = new SubscriptionModel(server, this);

        this.dictionaryManager = new DataTypeDictionaryManager(getNodeContext(), configuration.getNamespace());

        this.getLifecycleManager().addLifecycle(dictionaryManager);
        this.getLifecycleManager().addLifecycle(subscriptionModel);

        this.getLifecycleManager().addStartupTask(this::onStartup);

        this.getLifecycleManager().addLifecycle(new Lifecycle() {
            @Override
            public void startup() {
                startBogusEventNotifier();
            }

            @Override
            public void shutdown() {
                try {
                    keepPostingEvents = false;
                    eventThread.interrupt();
                    eventThread.join();
                } catch (InterruptedException ignored) {
                    // ignored
                }
            }
        });

        this.restrictedAccessFilter = new RestrictedAccessFilter(identity -> {
            if ( configuration.getAdminUser().equals(identity)) {
                return AccessLevel.READ_WRITE;
            } else {
                return AccessLevel.READ_ONLY;
            }
        });

//        this.variableNodes = new ArrayList<>();       
        this.dataBackendController.subscribeToTaskEvents(this);
    }
    
    protected void startBogusEventNotifier() {
        // Set the EventNotifier bit on Server Node for Events.
        UaNode serverNode = getServer()
            .getAddressSpaceManager()
            .getManagedNode(Identifiers.Server)
            .orElse(null);

        if (serverNode instanceof ServerTypeNode) {
            ((ServerTypeNode) serverNode).setEventNotifier(ubyte(1));
        }
    }
    
    protected void onStartup() {
        for (RemoteControllerTwin aTwin : this.dataBackendController.getRemoteControllerTwinList()) {
            this.addDevicesToNameSpace(aTwin);
        }
    }
    
    private void addDevicesToNameSpace( RemoteControllerTwin aTwin) {
        Controller controller = aTwin.getController();
        String controllerNodeIDString = controller.getName() + "/" + controller.getId() ;
        UaFolderNode controllerFolder = new UaFolderNode(
                this.getNodeContext(),
                newNodeId(controllerNodeIDString),
                newQualifiedName( controllerNodeIDString),
                LocalizedText.english( controllerNodeIDString)
        );
        // set event notifier bit off this node for events
        controllerFolder.setEventNotifier(ubyte(1));
        // add this to list of controller folder nodes
        this.controllerFolderNodes.add( controllerFolder);
        // add controller folder to nodes
        this.getNodeManager().addNode(controllerFolder);
        // and into the folder structure under root/objects by adding a reference to it
        controllerFolder.addReference(new Reference(
                controllerFolder.getNodeId(),
                Identifiers.Organizes,
                Identifiers.ObjectsFolder.expanded(),
                false
        ));
        // add remote command method to this folder
        this.addRemoteControlMethodNode( controller, controllerFolder);
        // add controller status node to this folder
        this.addStatusNode( controller, controllerFolder);
        // add all devices from this controller
        for (Sensuator sensuator : controller.getSensuators()) {
            // create device folder per found device
            String deviceNodeIDString = sensuator.getName() + "/" + sensuator.getId();
            UaFolderNode deviceFolder = new UaFolderNode(
                    this.getNodeContext(),
                    newNodeId( deviceNodeIDString),
                    newQualifiedName( deviceNodeIDString),
                    LocalizedText.english(deviceNodeIDString)
            );
            // add node to nodes
            this.getNodeManager().addNode(deviceFolder);
            // and into the folder structure under the controller folder
            controllerFolder.addOrganizes(deviceFolder);
            // create 'type' property under device
            String typeNodeIDString = controller.getName() + "/" + controller.getId() + "/" + sensuator.getName()+"/" + sensuator.getId() + "/type";
            UaVariableNode typeNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId(typeNodeIDString))
                .setAccessLevel(AccessLevel.READ_ONLY)
                .setUserAccessLevel(AccessLevel.READ_ONLY)
                .setBrowseName(newQualifiedName("type"))
                .setDisplayName(LocalizedText.english("type"))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataType)
                .build();
            // set type id as static value 
            typeNode.setValue(new DataValue( new Variant(sensuator.getType())));
            // add to namespace
            getNodeManager().addNode(typeNode);
            deviceFolder.addOrganizes(typeNode);
            // add dynamic properties to the device
            for (Parameter parameter : sensuator.getParameters()) {
                // first create setpoint parameter node for setting the value of the remote object's parameter
                String setNodeIdString = controller.getName() + "/"+ controller.getId() + "/" + sensuator.getName()+"/" + sensuator.getId() + "/set/" + parameter.getName();
                String setNodeName = "set-" + parameter.getName();
                UaVariableNode propertySetNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(newNodeId(setNodeIdString))
                    .setAccessLevel(AccessLevel.READ_WRITE)
                    .setUserAccessLevel(AccessLevel.READ_WRITE)
                    .setBrowseName(newQualifiedName(setNodeName))
                    .setDisplayName(LocalizedText.english(setNodeName))
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();
                // then variable node that exposes actual value of the parameter from the remote device
                String getNodeIdString = controller.getName() + "/" + controller.getId() + "/" + sensuator.getName()+"/" + sensuator.getId() + "/get/" + parameter.getName();
                String getNodeName = "get-" + parameter.getName();
                UaVariableNode propertyGetNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(newNodeId(getNodeIdString))
                    .setAccessLevel(AccessLevel.READ_ONLY)
                    .setUserAccessLevel(AccessLevel.READ_ONLY)
                    .setBrowseName(newQualifiedName(getNodeName))
                    .setDisplayName(LocalizedText.english(getNodeName))
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();
                // create a proper initial value
                DataValue initialDataValue;
                switch (parameter.getName()) {
                    case "direction": {
                        propertySetNode.setDataType(Identifiers.String);
                        initialDataValue = new DataValue( new Variant("null"));
                        break;
                    }
                    case "percentage":
                    case "degrees":
                    case "speed":
                    {
                        // set value
                        propertySetNode.setDataType(Identifiers.Double);
                        initialDataValue = new DataValue( new Variant(0.0));
                        break;
                    }
                    default: {
                        initialDataValue = new DataValue( new Variant( null));
                        LOGGER.log( Level.WARNING, "Parameter type setting not implemented yet");
                        break;
                    }
                }
                // set the initial value
                propertySetNode.setValue(initialDataValue);
                propertyGetNode.setValue(initialDataValue);
                // add listener to value changes of this node when an OPC UA client write to this set-node
                propertySetNode.addAttributeObserver( dataBackendController);
                // add node to Parameter remote object so that is value can be set of this get-node
                parameter.setUaVariableNode( propertyGetNode);
                // add to namespace with respective references
                getNodeManager().addNode(propertySetNode);
                deviceFolder.addOrganizes(propertySetNode);
                getNodeManager().addNode(propertyGetNode);
                deviceFolder.addOrganizes(propertyGetNode);
            }
        }
    }

    private void addStatusNode(Controller controller, UaFolderNode controllerFolder) {
        String parentFolderName = controllerFolder.getBrowseName().getName();
        try {
            // first create setpoint parameter node for setting the value of the remote object's parameter
            String strippedStateTopic = controller.getStateTopic();
            strippedStateTopic = strippedStateTopic.substring( strippedStateTopic.indexOf('/'));
            String setNodeIdString = strippedStateTopic;
            String setNodeName = "state";
            UaVariableNode controllerStatusNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId(setNodeIdString))
                .setAccessLevel(AccessLevel.READ_ONLY)
                .setUserAccessLevel(AccessLevel.READ_ONLY)
                .setBrowseName(newQualifiedName(setNodeName))
                .setDisplayName(LocalizedText.english(setNodeName))
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();
            controllerStatusNode.setDataType(Identifiers.String);
            // create a proper initial value
            DataValue initialDataValue = new DataValue( new Variant("null"));
            // set the initial value
            controllerStatusNode.setValue(initialDataValue);
            // add listener to value changes of this node when an OPC UA client write to this set-node
            controllerStatusNode.addAttributeObserver( dataBackendController);
            // add node to Parameter remote object so that is value can be set of this get-node
            controller.setUaVariableNode( controllerStatusNode);
            // add to namespace with respective references
            getNodeManager().addNode(controllerStatusNode);
            controllerFolder.addOrganizes(controllerStatusNode);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.SEVERE, "number format wrong: " + ex.getMessage(), ex);
        }
    }
    
    private void addRemoteControlMethodNode( Controller controller, UaFolderNode controllerFolder) {
        String parentFolderName = controllerFolder.getBrowseName().getName();
        try {
            // bulld the method node
            UaMethodNode methodNode = UaMethodNode.builder(this.getNodeContext())
                    .setNodeId(newNodeId(parentFolderName + "/remote-control(x)"))
                    .setBrowseName(newQualifiedName("remote-control(x)"))
                    .setDisplayName(new LocalizedText(null, "remote-control(x)"))
                    .setDescription(
                            LocalizedText.english("Remote command to an attached controller"))
                    .build();
            // add an invocation handler point towards the control method and the actual class that can be 'controlled'
            RemoteControlMethod remoteControlMethod = new RemoteControlMethod(methodNode, this, this.dataBackendController, controller);
            // set the method input and output properties and the created invocation handler
            methodNode.setInputArguments(remoteControlMethod.getInputArguments());
            methodNode.setOutputArguments(remoteControlMethod.getOutputArguments());
            methodNode.setInvocationHandler(remoteControlMethod);
            // add the method node to the namespace
            this.getNodeManager().addNode(methodNode);
            // and add a reference to the controller parent folder node refering to the method node   
            methodNode.addReference(new Reference(
                    methodNode.getNodeId(), // source nodeid
                    Identifiers.HasComponent, // type
                    controllerFolder.getNodeId().expanded(), // target nodeid
                    false
            ));
        } catch (NumberFormatException ex) {
            Logger.getLogger(IoTDeviceGatewayNamespace.class.getName()).log(Level.SEVERE, "number format wrong: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        this.subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public void receiveTaskEvent(TaskEvent aTaskEvent) {
        System.out.println("Receive task event: " + aTaskEvent.getName());
        String originatingControllerName = aTaskEvent.getController().getName() +"/"+  aTaskEvent.getController().getId();
        System.out.println("OriginatingControllerName: " + originatingControllerName);
        for (UaFolderNode folderNode: this.controllerFolderNodes) {
            // check if this folder has samen name as the originating controller name
            if (folderNode.getNodeId().getIdentifier().toString().contentEquals(originatingControllerName)) {
                // create OPC UA Event and publish
                System.out.println("About to publish an task event");
                publishEvent( folderNode, aTaskEvent.getName(), originatingControllerName);
            }
        }
    }
    
    public void publishEvent( UaNode folderNode, String taskName, String controllerName)  {
        String eventName = controllerName+"/"+taskName;
        try {
            String eventNodeId = "DropOffEvent";
            ProgressEventTypeNode eventNode = (ProgressEventTypeNode) getServer().getEventFactory().createEvent(
                    newNodeId(eventNodeId),
                    Identifiers.ProgressEventType
            );
            
            eventNode.setProgress(UShort.valueOf(100));
            eventNode.setContext( eventName);
            eventNode.setBrowseName(new QualifiedName(1, eventName));
            eventNode.setDisplayName(LocalizedText.english( eventName));
            eventNode.setEventId(ByteString.of(new byte[]{0, 1, 2, 3}));
            eventNode.setEventType(Identifiers.ProgressEventType);
            eventNode.setSourceNode(folderNode.getNodeId());
            eventNode.setSourceName(folderNode.getDisplayName().getText());
            eventNode.setTime(DateTime.now());
            eventNode.setReceiveTime(DateTime.NULL_VALUE);
            eventNode.setMessage(LocalizedText.english("task-ready event message!"));
            eventNode.setSeverity(ushort(1));
            System.out.println("Just before posting the event node");
            //noinspection UnstableApiUsage
            getServer().getEventBus().post(eventNode);
            
            eventNode.delete();
        } catch (UaException ex) {
            Logger.getLogger(IoTDeviceGatewayNamespace.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
