/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Settings;

/**
 * Psync message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class PsyncRouter extends ActiveRouter {

    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public PsyncRouter(Settings s) {
        super(s);
        //TODO: read&use Psync router specific settings (if any)
    }

    /**
     * Copy constructor.
     * @param r The router prototype where setting values are copied from
     */
    protected PsyncRouter(PsyncRouter r) {
        super(r);
        //TODO: copy Psync settings here (if any)
    }

    @Override
    public void update() {
        super.update();
        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }
//
//        // Try first the messages that can be delivered to final recipient
//        if (exchangeDeliverableMessages() != null) {
//            return; // started a transfer, don't try others (yet)
//        }

        // then try any/all message to any/all connection
        this.tryAllMessagesToAllConnections();
    }


    @Override
    public PsyncRouter replicate() {
        return new PsyncRouter(this);
    }

}
