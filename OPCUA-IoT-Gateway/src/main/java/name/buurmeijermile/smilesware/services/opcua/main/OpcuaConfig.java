
package name.buurmeijermile.smilesware.services.opcua.main;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "startNode",
    "serverPort"
})
@Generated("jsonschema2pojo")
public class OpcuaConfig {

    @JsonProperty("startNode")
    private String startNode;
    @JsonProperty("serverPort")
    private Integer serverPort;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("startNode")
    public String getStartNode() {
        return startNode;
    }

    @JsonProperty("startNode")
    public void setStartNode(String startNode) {
        this.startNode = startNode;
    }

    @JsonProperty("serverPort")
    public Integer getServerPort() {
        return serverPort;
    }

    @JsonProperty("serverPort")
    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
