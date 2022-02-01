
package old;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "type",
    "speed",
    "degrees"
})
@Generated("jsonschema2pojo")
public class ControlledObject {

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("speed")
    private Integer speed;
    @JsonProperty("degrees")
    private Double degrees;

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("speed")
    public Integer getSpeed() {
        return speed;
    }

    @JsonProperty("speed")
    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    @JsonProperty("degrees")
    public Double getDegrees() {
        return degrees;
    }

    @JsonProperty("degrees")
    public void setDegrees(Double degrees) {
        this.degrees = degrees;
    }

}
