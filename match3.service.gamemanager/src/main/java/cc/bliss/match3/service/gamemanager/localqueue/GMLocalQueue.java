/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.localqueue;

import bliss.lib.framework.queue.QueueCommand;
import bliss.lib.framework.queue.QueueManager;

/**
 * @author Phong
 */
public class GMLocalQueue {

    private static final QueueManager qm = QueueManager.getInstance("gm.local.queue");

    static {
        qm.init(4, 100000);
        qm.process();
    }

    public static void addQueue(QueueCommand cmd) {
        qm.put(cmd);
    }

}
