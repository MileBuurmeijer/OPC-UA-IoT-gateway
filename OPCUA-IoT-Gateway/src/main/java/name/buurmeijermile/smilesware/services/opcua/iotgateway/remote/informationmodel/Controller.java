package name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "command-topic",
    "state-topic",
    "name",
    "id",
    "sensuators"
})
@Generated("jsonschema2pojo")
public class Controller {
    
    public static String STATEKEYWORD = "controller-state";

    @JsonProperty("command-topic")
    private String commandTopic;
    @JsonProperty("state-topic")
    private String stateTopic;
    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("sensuators")
    private List<Sensuator> sensuators = null;
    @JsonIgnore
    private UaVariableNode propertyNode;

    @JsonProperty("command-topic")
    public String getCommandTopic() {
        return commandTopic;
    }

    @JsonProperty("command-topic")
    public void setCommandTopic(String commandTopic) {
        this.commandTopic = commandTopic;
    }

    @JsonProperty("state-topic")
    public String getStateTopic() {
        return stateTopic;
    }

    @JsonProperty("state-topic")
    public void setStateTopic(String stateTopic) {
        this.stateTopic = stateTopic;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("sensuators")
    public List<Sensuator> getSensuators() {
        return sensuators;
    }

    @JsonProperty("sensuators")
    public void setDevices(List<Sensuator> sensuators) {
        this.sensuators = sensuators;
    }

    @JsonIgnore
    public void setUaVariableNode(UaVariableNode controllerStatusNode) {
        this.propertyNode = controllerStatusNode;
    }
    
    @JsonIgnore
    public UaVariableNode getUaVariableNode() {
        return this.propertyNode;
    }
}
