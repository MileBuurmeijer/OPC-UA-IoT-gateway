package name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "gettopic",
    "settopic"
})
@Generated("jsonschema2pojo")
public class Parameter {

    @JsonProperty("name")
    private String name;
    @JsonProperty("gettopic")
    private String gettopic;
    @JsonProperty("settopic")
    private String settopic;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<String, Object>();
    @JsonIgnore
    private UaVariableNode propertyNode;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("gettopic")
    public String getGetTopic() {
        return gettopic;
    }

    @JsonProperty("gettopic")
    public void setGetTopic(String gettopic) {
        this.gettopic = gettopic;
    }

    @JsonProperty("settopic")
    public String getSetTopic() {
        return settopic;
    }

    @JsonProperty("settopic")
    public void setSetTopic(String settopic) {
        this.settopic = settopic;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @JsonIgnore
    public void setUaVariableNode(UaVariableNode aPropertyNode) {
        propertyNode = aPropertyNode;
    }

    @JsonIgnore
    public UaVariableNode getUaVariableNode() {
        return this.propertyNode;
    }
}
