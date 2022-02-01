/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.iotgateway;

import name.buurmeijermile.smilesware.services.opcua.iotgateway.remote.informationmodel.RemoteControllerTwin;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public interface DeviceDiscoveryListener {
    public void receiveDeviceDiscoveryEvent( RemoteControllerTwin aTwin);
}
