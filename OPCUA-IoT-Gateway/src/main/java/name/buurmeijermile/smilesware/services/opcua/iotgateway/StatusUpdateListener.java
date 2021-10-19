/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.iotgateway;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public interface StatusUpdateListener {
    public void receiveUpdate( String topic, String value);
}
