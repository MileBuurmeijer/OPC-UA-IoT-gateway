/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.controllerstate.ControllerCapabilities;

/**
 * Task store handles the storage and retrieval of tasks
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public class CapabilitiesStore {
    
    private static final Logger LOGGER = Logger.getLogger(CapabilitiesStore.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ControllerCapabilities readCapabilities( File inputFile) {
        if (inputFile != null && inputFile.exists() && inputFile.canRead()) {
            try {
                ControllerCapabilities controllerCapabilities = this.objectMapper.readValue( inputFile, ControllerCapabilities.class);
                return controllerCapabilities;
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } else {
            LOGGER.log( Level.SEVERE, "Input file does not exist or is not readable");
        }
        return null;
    }
    
    public void writeCapabilities( ControllerCapabilities controllerCapabilities, File outputFile) {
        if (outputFile != null && outputFile.canWrite()) {
            try {
                this.objectMapper.writeValue(outputFile, controllerCapabilities);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "IO error mapping JSON and writing to file", ex);
            }
        } else {
            LOGGER.log( Level.SEVERE, "Output file not writable");
        }
    }
}
