
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
    "mqttConfig",
    "opcuaConfig"
})
@Generated("jsonschema2pojo")
public class ServerConfiguration {

    @JsonProperty("mqttConfig")
    private MqttConfig mqttConfig;
    @JsonProperty("opcuaConfig")
    private OpcuaConfig opcuaConfig;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("mqttConfig")
    public MqttConfig getMqttConfig() {
        return mqttConfig;
    }

    @JsonProperty("mqttConfig")
    public void setMqttConfig(MqttConfig mqttConfig) {
        this.mqttConfig = mqttConfig;
    }

    @JsonProperty("opcuaConfig")
    public OpcuaConfig getOpcuaConfig() {
        return opcuaConfig;
    }

    @JsonProperty("opcuaConfig")
    public void setOpcuaConfig(OpcuaConfig opcuaConfig) {
        this.opcuaConfig = opcuaConfig;
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
