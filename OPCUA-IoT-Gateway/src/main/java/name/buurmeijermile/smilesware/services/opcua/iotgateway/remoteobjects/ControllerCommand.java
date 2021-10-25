
package name.buurmeijermile.smilesware.services.opcua.iotgateway.remoteobjects;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "controlled-object"
})
@Generated("jsonschema2pojo")
public class ControllerCommand {

    @JsonProperty("controlled-object")
    private ControlledObject controlledObject;

    @JsonProperty("controlled-object")
    public ControlledObject getControlledObject() {
        return controlledObject;
    }

    @JsonProperty("controlled-object")
    public void setControlledObject(ControlledObject controlledObject) {
        this.controlledObject = controlledObject;
    }

}
