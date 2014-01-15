/*
 * Copyright (c) 2013 Midokura Europe SARL, All Rights Reserved.
 */
package org.midonet.midolman.state.zkManagers;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooDefs.Ids;
import org.midonet.midolman.serialization.SerializationException;
import org.midonet.midolman.serialization.Serializer;
import org.midonet.midolman.state.*;
import org.midonet.packets.IPAddr;
import org.midonet.packets.IPAddr$;
import org.midonet.util.functors.Functors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Class to manage the router ZooKeeper data.
 */
public class IpAddrGroupZkManager extends AbstractZkManager {

    private final static Logger log = LoggerFactory
            .getLogger(IpAddrGroupZkManager.class);

    public static class IpAddrGroupConfig {

        public IpAddrGroupConfig() {
        }

        public IpAddrGroupConfig(String name) {
            this.name = name;
        }

        public UUID id;
        public String name;
        public Map<String, String> properties = new HashMap<String, String>();
    }

    private final RuleZkManager ruleDao;

    /**
     * Initializes a IpAddrGroupZkManager object with a ZooKeeper client and the
     * root path of the ZooKeeper directory.
     *
     * @param zk
     *         Zk data access class
     * @param paths
     *         PathBuilder class to construct ZK paths
     * @param serializer
     *         ZK data serialization class
     * @versionProvider
     *         Provides versioning information
     */
    public IpAddrGroupZkManager(ZkManager zk, PathBuilder paths,
                            Serializer serializer) {
        super(zk, paths, serializer);
        ruleDao = new RuleZkManager(zk, paths, serializer);
    }

    /**
     * Constructs a list of ZooKeeper update operations to perform when adding a
     * new IP addr group.
     *
     * @param id
     *            IP addr group ID
     * @param config
     *            IpAddrGroupConfig object.
     * @return A list of Op objects to represent the operations to perform.
     */
    public List<Op> prepareCreate(UUID id, IpAddrGroupConfig config)
            throws StateAccessException, SerializationException {
        log.debug("IpddrGroupZkManager.prepareCreate: entered");

        List<Op> ops = new ArrayList<Op>();
        ops.add(Op.create(paths.getIpAddrGroupPath(id),
                serializer.serialize(config),
                Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT));

        // Directory for member addresses.
        ops.add(Op.create(paths.getIpAddrGroupAddrsPath(id), null,
                Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));

        // Keep track of rules that reference this group.
        ops.add(Op.create(paths.getIpAddrGroupRulesPath(id), null,
                Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));

        log.debug("IpAddrGroupZkManager.prepareCreate: exiting");
        return ops;
    }

    /**
     * Constructs a list of operations to perform in a IP addr group deletion.
     *
     * @param id
     *            The ID of a IP addr group to delete.
     * @return A list of Op objects representing the operations to perform.
     * @throws org.midonet.midolman.state.StateAccessException
     */
    public List<Op> prepareDelete(UUID id) throws StateAccessException,
            SerializationException {

        List<Op> ops = new ArrayList<Op>();

        // Delete all the rules that reference this IP addr group
        String rulesPath = paths.getIpAddrGroupRulesPath(id);
        Set<String> ruleIds = zk.getChildren(rulesPath);
        for (String ruleId : ruleIds) {
            ops.addAll(ruleDao.prepareRuleDelete(UUID.fromString(ruleId)));
        }

        // Delete addresses.
        Set<String> addrs = zk.getChildren(paths.getIpAddrGroupAddrsPath(id));
        for (String addr : addrs) {
            ops.add(Op.delete(paths.getIpAddrGroupAddrPath(id, addr), -1));
        }

        ops.add(Op.delete(rulesPath, -1));
        ops.add(Op.delete(paths.getIpAddrGroupAddrsPath(id), -1));
        ops.add(Op.delete(paths.getIpAddrGroupPath(id), -1));

        return ops;
    }

    /**
     * Performs an atomic update on the ZooKeeper to add a new IP addr group.
     *
     * @return The UUID of the newly created object.
     * @throws org.midonet.midolman.state.StateAccessException
     */
    public UUID create(IpAddrGroupConfig config) throws StateAccessException,
            SerializationException {
        UUID id = config.id != null ? config.id : UUID.randomUUID();
        zk.multi(prepareCreate(id, config));
        return id;
    }

    /***
     * Deletes a IP addr group and its related data from the directories
     * atomically.
     *
     * @param id
     *            ID of the IP addr group to delete.
     * @throws org.midonet.midolman.state.StateAccessException
     */
    public void delete(UUID id) throws StateAccessException,
            SerializationException {
        zk.multi(prepareDelete(id));
    }

    /**
     * Checks whether a IP addr group with the given ID exists.
     *
     * @param id
     *            IP addr group ID to check
     * @return True if exists
     * @throws org.midonet.midolman.state.StateAccessException
     */
    public boolean exists(UUID id) throws StateAccessException {
        return zk.exists(paths.getIpAddrGroupPath(id));
    }

    /**
     * Gets a IpAddrGroupConfig object with the given ID.
     *
     * @param id
     *            The ID of the ip addr group.
     * @return IpAddrGroupConfig object
     * @throws org.midonet.midolman.state.StateAccessException
     */
    public IpAddrGroupConfig get(UUID id) throws StateAccessException,
            SerializationException {
        byte[] data = zk.get(paths.getIpAddrGroupPath(id));
        return serializer.deserialize(data, IpAddrGroupConfig.class);
    }

    public Set<UUID> getAllIds() throws StateAccessException {
        String path = paths.getIpAddrGroupsPath();
        Set<String> groups = zk.getChildren(path);
        Set<UUID> ids = new HashSet<UUID>();
        for (String group : groups) {
            ids.add(UUID.fromString(group));
        }
        return ids;
    }

    public List<IpAddrGroupConfig> list()
            throws StateAccessException, SerializationException {
        String path = paths.getIpAddrGroupsPath();
        Set<String> groups = zk.getChildren(path);
        List<IpAddrGroupConfig> configs = new ArrayList<IpAddrGroupConfig>();
        for (String group : groups) {
            configs.add(get(UUID.fromString(group)));
        }
        return configs;
    }

    public boolean isMember(UUID groupId, String addr)
            throws StateAccessException {
        addr = IPAddr$.MODULE$.canonicalize(addr);
        return zk.exists(paths.getIpAddrGroupAddrPath(groupId, addr));
    }

    public void addAddr(UUID groupId, String addr)
            throws StateAccessException, SerializationException {
        addr = IPAddr$.MODULE$.canonicalize(addr);
        try {
            zk.addPersistent(paths.getIpAddrGroupAddrPath(groupId, addr), null);
        } catch (StatePathExistsException ex) {
            // This group already has this address. Do nothing.
        }

    }

    public Set<String> getAddrs(UUID id) throws StateAccessException {
        return zk.getChildren(paths.getIpAddrGroupAddrsPath(id));
    }

    public void getAddrsAsync(UUID ipAddrGroupId,
                              DirectoryCallback<Set<String>> addrsCallback,
                              Directory.TypedWatcher watcher) {
        zk.asyncGetChildren(
                paths.getIpAddrGroupAddrsPath(ipAddrGroupId),
                DirectoryCallbackFactory.transform(
                        addrsCallback, Functors.<Set<String>>identity()),
                watcher);
    }

    public void removeAddr(UUID groupId, String addr)
            throws StateAccessException, SerializationException {
        addr = IPAddr$.MODULE$.canonicalize(addr);
        try {
            zk.delete(paths.getIpAddrGroupAddrPath(groupId, addr));
        } catch (NoStatePathException ex) {
            // Either the group doesn't exist, or the address doesn't.
            // Either way, the desired postcondition is met.
        }
    }
}
