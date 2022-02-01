
package name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate;

import java.util.List;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "controller-name",
    "controller-id",
    "state-name",
    "sensuators"
})
@Generated("jsonschema2pojo")
public class ControllerState {

    @JsonProperty("controller-name")
    private String controllerName;
    @JsonProperty("controller-id")
    private String controllerId;
    @JsonProperty("state-name")
    private String stateName;
    @JsonProperty("sensuators")
    private List<Sensuator> sensuators = null;

    @JsonProperty("controller-name")
    public String getControllerName() {
        return controllerName;
    }

    @JsonProperty("controller-name")
    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    @JsonProperty("controller-id")
    public String getControllerId() {
        return controllerId;
    }

    @JsonProperty("controller-id")
    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }

    @JsonProperty("state-name")
    public String getStateName() {
        return stateName;
    }

    @JsonProperty("state-name")
    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    @JsonProperty("sensuators")
    public List<Sensuator> getSensuators() {
        return sensuators;
    }

    @JsonProperty("sensuators")
    public void setSensuators(List<Sensuator> sensuators) {
        this.sensuators = sensuators;
    }
}
