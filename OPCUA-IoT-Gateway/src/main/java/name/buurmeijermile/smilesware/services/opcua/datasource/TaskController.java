/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.datasource;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.UnknownHostException;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.RemoteControllerCommandMessage;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.ControllerState;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.ActionCommand;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.Capability;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.ControllerCapabilities;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.Task;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.Controller;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.RemoteControllerTwin;
import name.buurmeijermile.smilesware.services.opcua.utils.Waiter;

/**
 * The task controller controls the execution of tasks that are a sequence of actions set. 
 * An action set can be executed by the remote controller and results in a combined state of 
 * the controlled object (i.e. robot arm). Through the OPC UA server tasks can be commanded
 * so that this task controller is triggered to execute the task and its contained 
 * action sets.
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class TaskController implements Runnable{

    private static final Logger LOGGER = Logger.getLogger(TaskController.class.getName());
    private static final int DELAYBETWEENACTIONS = 2000; // TODO: make configurable later, e.g. pause action
    
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<TempTask> definedTasksList = new ArrayList<>();
    private final List<RemoteControllerCommandMessage> taskLearnList = new ArrayList<>(); // tasks that the task controller learned
    private final List<Task> taskExecutionList = new ArrayList<>();
    private final IoTDeviceBackendController ioTDeviceBackendController;
    private final CapabilitiesStore capabilityStore = new CapabilitiesStore();
    private ControllerCapabilities controllerCapabilities;
    private RemoteControllerTwin defaultRemoteControllerTwin;
    private final List<String> requestedTasksList = new ArrayList<>();
    private final List<TaskEventListener> taskEventListeners = new ArrayList<>();
    private boolean running = false;
    
    public TaskController( IoTDeviceBackendController anIoTDeviceBackendController) {
        this.ioTDeviceBackendController = anIoTDeviceBackendController;
        // TODO: move the config file with capabilities to configurator class
    }
    
    public void initialize() {
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Logger.getLogger(TaskController.class.getName()).log(Level.SEVERE, "hostname not found", ex);
        }
        if (hostname.contains("mbuurmei")) {
            this.controllerCapabilities = this.capabilityStore.readCapabilities( new File("/home/mbuurmei/NetBeansProjects/OPC-UA-IoT-gateway/OPCUA-IoT-Gateway/src/main/java/capabilities-example.json"));
            this.defaultRemoteControllerTwin = this.capabilityStore.readRemoteControllerTwin( new File("/home/mbuurmei/NetBeansProjects/OPC-UA-IoT-gateway/OPCUA-IoT-Gateway/src/main/java/informationModel.json"));
        } else {
            this.controllerCapabilities = this.capabilityStore.readCapabilities( new File("/app/resources/capabilities-example.json"));
            this.defaultRemoteControllerTwin = this.capabilityStore.readRemoteControllerTwin( new File("/app/resources/informationModel.json"));
        }
    }
    
    public void addTaskEventListener( TaskEventListener aTaskEventListener) {
        this.taskEventListeners.add(aTaskEventListener);
    }
    
    public int requestTask( String taskName) {
        // find a capability with this taskname
        Capability matchedCapability = this.controllerCapabilities.getCapabilities().stream()
                .filter( capability ->  capability.getTask().getName().contentEquals(taskName))
                .findAny()
                .orElse( null);
        // if matched add to requested task list for later execution
        if ( matchedCapability != null) {
            // clear list first
            this.taskExecutionList.clear();
            // then add the task within the matched capability
            this.taskExecutionList.add( matchedCapability.getTask());
            return 0; // flag recipient that request was know and scheduled for execution
        } else {
            return -1; // flag recipient that request was not understood
        }       
    }
    
    /**
     * The learning interaction point for the task controller. It basically can respond to
     * task learning messages from the remote controller (receiving 'controller-states').
     * @param aRemoteControllerCommandMessage 
     */
    public void receiveRemoteControllerMessage( RemoteControllerCommandMessage aRemoteControllerCommandMessage) {
        this.taskLearnList.add(aRemoteControllerCommandMessage);
    }
    
    private void processTaskLists(){
        // check if there something to do: first learn aspects
        if (!taskLearnList.isEmpty()) {
            // get first remote controller command
            Optional<RemoteControllerCommandMessage> firstMessage = taskLearnList.stream().findFirst();
            if (firstMessage.isPresent()) {
                RemoteControllerCommandMessage actualMessage = firstMessage.get();
                processMessage( actualMessage.getTopic(), actualMessage.getCommand());
            }
        }
        // secondly task exection requests
        if (!this.taskExecutionList.isEmpty()) {
            Task task = this.taskExecutionList.stream().findFirst().get();
            if (task!=null) {
                this.executeTask(task);
                this.taskExecutionList.remove(task);
            }
        }
    }

    public void processMessage( String topic, String value) {
        // create JSON object from the value string
        JSONObject jsonObject = new JSONObject( value);
        // get the controller-state key from the overall json object
        JSONObject controllerObject = jsonObject.getJSONObject("controller-state");
        // check if this key was present
        if (controllerObject != null) {
            processControllerState( value);  // process this controller state
        }
    }
    
    public void processControllerState( String jsonStateCommand) {
        try {
            // map the json to the Java POJO's using a Jackson mapper and the controller state domain classes
            ActionCommand actionCommand = mapper.readValue( jsonStateCommand, ActionCommand.class);
            ControllerState controllerState = actionCommand.getControllerState();
            LOGGER.log( Level.INFO, "Controller state message mapped to POJOs");
            // get the controller Id
            String controllerId = controllerState.getControllerId();
            int controllerNumber = Integer.parseInt(controllerId);
            // get the state name, containing controller and 
            String stateName = controllerState.getStateName();
            // create a task out of the received state command
            TempTask aTask = new TempTask( stateName, controllerNumber);
            // check if this task is already present
            int indexOfTask = this.definedTasksList.indexOf( aTask);
            if (indexOfTask >= 0) {
                // OK, task is present: get this task and add action command to it
                TempTask theTask = this.definedTasksList.get(indexOfTask);
                theTask.addCommandAction(actionCommand);
            } else {
                // OK, it is a new task: add action command to it and at to list of defined tasks
                 aTask.addCommandAction(actionCommand);
                this.definedTasksList.add(aTask);
            }
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, "Mapper failed to map jsonStateCommand", ex);
        }
    }
    
    /**
     * Execute a task for a controller. The task consists of one or more action commands 
     * (e.g. moves or grip actions). An action command contains actions for the sensuators 
     * of a remote controller that together make a action, e.g. motor 1 to 30 degrees, motor 3 to 
     * 70 degrees, ...
     * @param aTask 
     */
    private void executeTask( Task aTask) {
        // iterate through the underlying action commands
        for (ActionCommand anActionCommand : aTask.getActionCommands()) {
            // first check if there is a pause action set to execute
            Waiter.waitADuration( Duration.ofSeconds(10)); // add 10 seconds between commands, TODO: make this smarter
            this.executeActionCommand( anActionCommand);
        }
        // flag to task event listener that task is ready
        // first get the controller of the task that was just executed
        ActionCommand anActionCommand = aTask.getActionCommands().get(0);
        if (anActionCommand != null) {
            // get controller id
            String controllerId = anActionCommand.getControllerState().getControllerId();
            // find controller with this id from information models from remote controllers
            Controller controller = this.ioTDeviceBackendController.getControllerById( controllerId);
            // create task event with the controller
            TaskEvent taskReadyEvent = new TaskEvent( "task-ready", controller);
            // send to listeners
            this.taskEventListeners.forEach(action -> action.receiveTaskEvent(taskReadyEvent)); 
        } else {
            LOGGER.log( Level.WARNING, "First action command not found in executed task?!");
        }
    }
        
    /**
     * The remote controller can receive action commands: it can receive
     * a set of actions or set-points for parameters of the controllable 
     * parts of the remote controller . This method builds up
     * that action message. 
     * @param controllerId set for which remote controller this message is
     * @param anActionCommand the action set to reflect in the message
     */
    private void executeActionCommand( ActionCommand anActionCommand) {
        // execute the action command by sending it to the remote controller
        // ask the back end controller to send this message (back end controller only knows the mqtt controller)
        this.ioTDeviceBackendController.sendRemoteCommand( anActionCommand);
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            this.processTaskLists();
            Waiter.waitMilliSeconds( 100); // set response frequency to 10Hz
        }
    }

    List<Capability> getDefinedCapabilities() {
        return this.controllerCapabilities.getCapabilities();
    }

    /**
     * @return the defaultRemoteControllerTwin
     */
    public RemoteControllerTwin getDefaultRemoteControllerTwin() {
        return defaultRemoteControllerTwin;
    }
}