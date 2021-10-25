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

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.buurmeijermile.smilesware.services.opcua.datasource.IoTDeviceBackendController;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.Controller;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.Device;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.DeviceControllerTwin;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects.Parameter;
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
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class IoTDeviceGatewayNamespace extends ManagedNamespaceWithLifecycle {
    // class variables
    private static final Logger LOGGER = Logger.getLogger(IoTDeviceGatewayNamespace.class.getName());

    // instance variables
    private final SubscriptionModel subscriptionModel;
    private final OpcUaServer server;
    //private final DeviceControllerTwin deviceControllerTwin;
    private final IoTDeviceBackendController dataBackendController;
    private final RestrictedAccessFilter restrictedAccessFilter;
    private List<UaVariableNode> variableNodes = null;
    private volatile Thread eventThread;
    private volatile boolean keepPostingEvents = true;
    private DataTypeDictionaryManager dictionaryManager;

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
//        this.deviceControllerTwin = anIoTBackendController.getControllerTwin();
        
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

        this.variableNodes = new ArrayList<>();       
    }
    
    protected void startBogusEventNotifier() {
        // do nothing
    }

    protected void onStartup() {
        for (DeviceControllerTwin aTwin : this.dataBackendController.getControllerTwins()) {
            this.addDevicesToNameSpace(aTwin);
        }
    }
    
    private void addDevicesToNameSpace( DeviceControllerTwin aTwin) {
        Controller controller = aTwin.getController();
        String controllerNodeIDString = controller.getName() + "/" + controller.getId() ;
        UaFolderNode controllerFolder = new UaFolderNode(
                this.getNodeContext(),
                newNodeId(controllerNodeIDString),
                newQualifiedName( controllerNodeIDString),
                LocalizedText.english( controllerNodeIDString)
        );
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
        // add all devices from this controller
        for (Device device : controller.getDevices()) {
            String deviceNodeIDString = device.getName() + "/" + device.getId();
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
            // create type property under device
            String typeNodeIDString = controller.getName() + "/" + controller.getId() + "/" + device.getName()+"/" + device.getId() + "/type";
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
            typeNode.setValue(new DataValue( new Variant(device.getType())));
            // add to namespace
            getNodeManager().addNode(typeNode);
            deviceFolder.addOrganizes(typeNode);
            // add dynamic properties to the device
            for (Parameter parameter : device.getParameters()) {
                // first create setpoint parameter node for setting the value of the remote object's parameter
                String setNodeIdString = controller.getName() + "/"+ controller.getId() + "/" + device.getName()+"/" + device.getId() + "/set/" + parameter.getName();
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
                String getNodeIdString = controller.getName() + "/" + controller.getId() + "/" + device.getName()+"/" + device.getId() + "/get/" + parameter.getName();
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
    
    // todo: update this merge from another project (OPC UA Player)
    private void addRemoteControlMethodNode( Controller controller, UaFolderNode controllerFolder) {
        String parentFolderName = controllerFolder.getBrowseName().getName();
        try {
//            // create a "PlayerControl" folder and add it to the node manager
//            NodeId remoteControlNodeId = this.newNodeId(parentFolderName);
//            UaFolderNode remoteControlFolderNode = new UaFolderNode(
//                    this.getNodeContext(),
//                    remoteControlNodeId,
//                    this.newQualifiedName(parentFolderName),
//                    LocalizedText.english(parentFolderName)
//            );
//            // add this method node to servers node map
//            this.getNodeManager().addNode(remoteControlFolderNode);
//            // and into the folder structure under root/objects by adding a reference to it
//            remoteControlFolderNode.addReference(new Reference(
//                    remoteControlFolderNode.getNodeId(),
//                    Identifiers.Organizes,
//                    Identifiers.ObjectsFolder.expanded(),
//                    false
//            ));
            // bulld the method node
            UaMethodNode methodNode = UaMethodNode.builder(this.getNodeContext())
                    .setNodeId(newNodeId(parentFolderName + "/remote-control(x)"))
                    .setBrowseName(newQualifiedName("remote-control(x)"))
                    .setDisplayName(new LocalizedText(null, "remote-control(x)"))
                    .setDescription(
                            LocalizedText.english("Remote command to an attached controller"))
                    .build();
            // add an invocation handler point towards the control method and the actual class that can be 'controlled'
            RemoteControlMethod remoteControlMethod = new RemoteControlMethod(methodNode, this.dataBackendController, controller);
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
//            // add in same folder a variable node that shows the current state
//            String nodeName = "RemoteCommand";
//            // create variable node
//            UaVariableNode runStateVariableNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
//                .setNodeId(newNodeId(parentFolderName + "/" + nodeName))
//                .setAccessLevel(AccessLevel.READ_ONLY)
//                .setUserAccessLevel(AccessLevel.READ_ONLY)
//                .setBrowseName(newQualifiedName(nodeName))
//                .setDisplayName(LocalizedText.english(nodeName))
//                .setDataType(Identifiers.String)
//                .setTypeDefinition(Identifiers.BaseDataVariableType)
//                .build();
//            // make this varable node known to data backend controller so that it can updates to the runstate into this node
//            //this.dataController.setRunStateUANode(runStateVariableNode);
//            // add node to server mapRunState"
//            this.getNodeManager().addNode(runStateVariableNode);
//            // add node to this player folder
//            controllerFolder.addOrganizes(runStateVariableNode);
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
}
