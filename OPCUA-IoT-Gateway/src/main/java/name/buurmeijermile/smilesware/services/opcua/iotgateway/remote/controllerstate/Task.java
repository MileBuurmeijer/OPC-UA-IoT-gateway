/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate;

import java.util.List;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "id",
    "action-commands"
})
@Generated("jsonschema2pojo")
public class Task {

    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("action-commands")
    private List<ActionCommand> actionCommands = null;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("action-commands")
    public List<ActionCommand> getActionCommands() {
        return actionCommands;
    }

    @JsonProperty("action-commands")
    public void setActionCommands(List<ActionCommand> actionCommands) {
        this.actionCommands = actionCommands;
    }
    @Override
    public boolean equals( Object object) {
        boolean result = false;
        if ( object instanceof Task) {
            Task anotherTask = (Task) object;
            result = anotherTask.getName().contentEquals( this.name);
            result = result && anotherTask.getId()==this.id;
        }
        return result;
    }
//    
//    @Override
//    public boolean equals(Object other) {
//        if (other == this) {
//            return true;
//        }
//        if ((other instanceof Task) == false) {
//            return false;
//        }
//        Task rhs = ((Task) other);
//        return ((((this.name == rhs.name) || ((this.name != null) && this.name.equals(rhs.name))) && ((this.id == rhs.id) || ((this.id != null) && this.id.equals(rhs.id)))) && ((this.actionCommands == rhs.actionCommands) || ((this.actionCommands != null) && this.actionCommands.equals(rhs.actionCommands))));
//    }
}
