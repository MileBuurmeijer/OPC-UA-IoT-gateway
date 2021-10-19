/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.main;

import java.util.logging.Level;
import java.util.logging.Logger;
import name.buurmeijermile.smilesware.services.opcua.datasource.IoTDeviceBackendController;
import name.buurmeijermile.smilesware.services.opcua.server.IoTDeviceGatewayNamespace;
import name.buurmeijermile.smilesware.services.opcua.server.OPCUAServer;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    private OPCUAServer opcuaServer = null;
    private Configuration configuration;
    private IoTDeviceBackendController ioTDeviceBackendController = new IoTDeviceBackendController();
    
    public void init() {
        // initialize the iot device backend controller
        ioTDeviceBackendController.init();
    }
    
    public void processCommandlineArguments( String [] args) {
        try {
            // parse command line arguments into a configuration object
            configuration = Configuration.getConfiguration();
//            configuration.processCommandLine( args);
//            // print version info
//            logger.log(Level.INFO, "Version: " + configuration.getAppName() + " | " + configuration.getVersion());
        } catch ( Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception thrown in processing arguments", ex);
        }
    }    
    
    public void startupServer() {
        try {
            opcuaServer = new OPCUAServer( configuration);
            // check if a server is created
            if (opcuaServer != null) {
                LOGGER.log(Level.INFO, "OPC UA Server about to start");
                // start the OPC UA server
                opcuaServer.startup().get();
                LOGGER.log(Level.INFO, "OPC UA Server started, creating shutdown hook");
                // create proper shutdown hook
//                final CompletableFuture<Void> future = new CompletableFuture<>();
//                Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));
//                future.get();
                LOGGER.log(Level.INFO, "OPC UA Server about to add the namespace");
                // create the namespace for this OPCUA server
                IoTDeviceGatewayNamespace deviceNamespace = 
                        new IoTDeviceGatewayNamespace( 
                                opcuaServer.getServer(), 
                                ioTDeviceBackendController, 
                                Configuration.getConfiguration());
                deviceNamespace.startup();
                LOGGER.log(Level.INFO, "Device namespace added");
            } else {
                LOGGER.log(Level.SEVERE, "OPC UA Server not initialized");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
        
    public static void main(String[] args) {
        MainController mainController = new MainController();
        mainController.init();
        // always initialize the configuration with the commandline arguments first
        mainController.processCommandlineArguments( args);
        mainController.startupServer();
    }

}
