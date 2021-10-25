
package name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects;

import java.util.List;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "controller-commands"
})
@Generated("jsonschema2pojo")
public class RemoteCommand {

    @JsonProperty("controller-commands")
    private List<ControllerCommand> controllerCommands = null;

    @JsonProperty("controller-commands")
    public List<ControllerCommand> getControllerCommands() {
        return controllerCommands;
    }

    @JsonProperty("controller-commands")
    public void setControllerCommands(List<ControllerCommand> controllerCommands) {
        this.controllerCommands = controllerCommands;
    }

}
