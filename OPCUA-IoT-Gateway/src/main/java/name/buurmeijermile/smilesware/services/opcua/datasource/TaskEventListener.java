/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package name.buurmeijermile.smilesware.services.opcua.datasource;

/**
 *
 * @author Milé Buurmeijer <mbuurmei at netscape.net>
 */
public interface TaskEventListener {
    public void receiveTaskEvent( TaskEvent aTaskEvent);
}
