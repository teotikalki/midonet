/*
 * @(#)RestResource.java        1.6 11/09/05
 *
 * Copyright 2011 Midokura KK
 */
package com.midokura.midolman.mgmt.rest_api.v1.resources;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

/**
 * Base abstract class for all the resources.
 * 
 * @version 1.6 05 Sept 2011
 * @author Ryu Ishimoto
 */
public abstract class RestResource {
    /*
     * Provide resources that can be shared for all the subclassed resources.
     */

    /** Constants **/
    private final static int DEFAULT_ZK_TIMEOUT = 3000;

    /** Zookeeper connection string **/
    protected String zookeeperConn = null;
    protected int zookeeperTimeout = DEFAULT_ZK_TIMEOUT;
    protected String zookeeperRoot = "/midolman";
    protected String zookeeperMgmtRoot = "/midolman-mgmt";

    /**
     * Set zookeeper connection from config at the application initialization.
     * 
     * @param context
     *            ServletContext object to which it gets data from.
     */
    @Context
    public void setZookeeperConn(ServletContext context) {
        zookeeperConn = context.getInitParameter("zookeeper-connection");
        String zkTo = context.getInitParameter("zookeeper-timeout");
        if (zkTo != null) {
            zookeeperTimeout = Integer.parseInt(zkTo);
        }
        String rootPath = context.getInitParameter("zookeeper-root");
        if (rootPath != null) {
            zookeeperRoot = rootPath;
        }
        rootPath = context.getInitParameter("zookeeper-mgmt-root");
        if (rootPath != null) {
            zookeeperMgmtRoot = rootPath;
        }
    }
}
