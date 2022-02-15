/* 
 * The MIT License
 *
 * Copyright 2018 Milé Buurmeijer <mbuurmei at netscape.net>.
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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import name.buurmeijermile.smilesware.services.opcua.datasource.IoTDeviceBackendController;
import name.buurmeijermile.smilesware.services.opcua.datasource.RemoteControllerCommand;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Controller;

public class RemoteControlMethod extends AbstractMethodInvocationHandler {

    private final Controller controller;
    private final IoTDeviceGatewayNamespace namespaceManager;
    private final IoTDeviceBackendController iotDeviceBackendController;
    
    // create the input argument
    public static final Argument COMMAND = new Argument(
        "control command",
        Identifiers.String,
        ValueRanks.Scalar,
        null,
        new LocalizedText("A control command number")
    );
    // create the output argument
    public static final Argument COMMANDRESULT = new Argument(
        "result",
        Identifiers.String,
        ValueRanks.Scalar,
        null,
        new LocalizedText("The result of the control command")
    );
    
    public RemoteControlMethod( UaMethodNode aMethodNode, IoTDeviceGatewayNamespace parentNamespaceManager, IoTDeviceBackendController aIoTDeviceBackendController, Controller aController) {
        super( aMethodNode);
        this.controller = aController; // the digital twin of the controller inside the IoT device
        this.namespaceManager = parentNamespaceManager;
        this.iotDeviceBackendController = aIoTDeviceBackendController; // the back end controller of this OPC UA server controller the remote IoT device
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{ COMMAND};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{ COMMANDRESULT};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        Logger.getLogger( RemoteControlMethod.class.getName()).log(Level.FINE, "Invoking remote control method of objectId={}", invocationContext.getObjectId());

        String command = inputValues[0].getValue().toString();
        String commandResult = "";
        
        if (command.equals("#TE")) {
            String controllerName = this.controller.getName() + "/" + this.controller.getId();
            namespaceManager.publishEvent(invocationContext.getMethodNode(), "test-task-1", controllerName);
            commandResult = "Test event requested";
        } else {
            RemoteControllerCommand controllerCommand = new RemoteControllerCommand( this.controller, command);

            Logger.getLogger(RemoteControlMethod.class.getName()).log(Level.FINE, "control(" + command + ")");
            Logger.getLogger(RemoteControlMethod.class.getName()).log(Level.FINE, "Invoking control() method of Object '" + invocationContext.getMethodNode().getBrowseName().getName() + "'");

            commandResult = this.iotDeviceBackendController.receiveRemoteControlCommand( controllerCommand);
        }
        

        return new Variant[]{new Variant(commandResult)};   
    }
}