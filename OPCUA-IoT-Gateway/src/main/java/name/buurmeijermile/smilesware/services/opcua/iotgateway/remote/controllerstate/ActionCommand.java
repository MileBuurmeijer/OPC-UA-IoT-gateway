package name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "get",
    "set",
    "pause-duration",
    "controller-state"
})
//@Generated("jsonschema2pojo")
public class ActionCommand {

    @JsonProperty("get")
    private String get;
    @JsonProperty("set")
    private String set;
    @JsonProperty("pause-duration")
    private int pauseDuration = 0; // in milliseconds, for now as int, due to JSON not having duration as data type
    @JsonProperty("controller-state")
    private ControllerState controllerState;

    @JsonProperty("get")
    public String getGet() {
        return get;
    }

    @JsonProperty("get")
    public void setGet(String get) {
        this.get = get;
    }

    @JsonProperty("set")
    public String getSet() {
        return set;
    }

    @JsonProperty("set")
    public void setSet(String set) {
        this.set = set;
    }

    @JsonProperty("pause-duration")
    public int getDuration() {
        return pauseDuration;
    }

    @JsonProperty("pause-duration")
    public void setDuration(int aDuration) {
        this.pauseDuration = aDuration;
    }

    @JsonProperty("controller-state")
    public ControllerState getControllerState() {
        return controllerState;
    }

    @JsonProperty("controller-state")
    public void setControllerState(ControllerState controllerState) {
        this.controllerState = controllerState;
    }

    @JsonIgnore()
    public boolean isReceived() {
        return this.get != null;
    }

    @JsonIgnore()
    public boolean isSend() {
        return this.set != null;
    }
}
