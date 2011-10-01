package com.midokura.midolman.mgmt.tools;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;

import com.midokura.midolman.mgmt.data.dto.AdRoute;
import com.midokura.midolman.mgmt.data.dto.Bgp;
import com.midokura.midolman.mgmt.data.dto.Chain;
import com.midokura.midolman.mgmt.data.dto.MaterializedRouterPort;
import com.midokura.midolman.mgmt.data.dto.Route;
import com.midokura.midolman.mgmt.data.dto.Router;
import com.midokura.midolman.mgmt.data.dto.Rule;
import com.midokura.midolman.mgmt.data.dto.Tenant;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class CreateZkTestConfig {

    static String basePath = "http://mido-iida-2.midokura.jp:8081/midolmanj-mgmt/v1";

    public static void main(String[] args) {
        WebResource resource;
        String url;
        ClientResponse response;

        ClientConfig cc = new DefaultClientConfig();
        cc.getSingletons().add(new JacksonJaxbJsonProvider());
        Client client = Client.create(cc);

        /*
         * url = new StringBuilder(basePath).append("/admin/init").toString();
         * resource = client.resource(url); response =
         * resource.type(MediaType.APPLICATION_JSON)
         * .header("HTTP_X_AUTH_TOKEN", "999888777666")
         * .post(ClientResponse.class);
         */

        // Add the tenant
        url = new StringBuilder(basePath).append("/tenants").toString();
        resource = client.resource(url);
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, new Tenant());
        System.out.println(response.getLocation());

        // Add the router
        url = new StringBuilder(response.getLocation().toString()).append(
                "/routers").toString();
        resource = client.resource(url);
        Router rtr = new Router();
        rtr.setName("PinoDanTest");
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, rtr);
        System.out.println(response.getLocation());
        String router = response.getLocation().toString();

        // Add the BGP port.
        url = new StringBuilder(router).append("/ports").toString();
        resource = client.resource(url);
        MaterializedRouterPort port = new MaterializedRouterPort();
        port.setNetworkAddress("180.214.47.64");
        port.setNetworkLength(30);
        port.setPortAddress("180.214.47.66");
        port.setLocalNetworkAddress("180.214.47.64");
        port.setLocalNetworkLength(30);
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, port);
        String portPath = response.getLocation().toString();
        System.out.println(portPath);
        String[] parts = portPath.split("/");
        String bgpPortId = parts[parts.length - 1];
        System.out.println("Got port id: " + bgpPortId);

        url = new StringBuilder(response.getLocation().toString()).append(
                "/bgps").toString();
        resource = client.resource(url);
        Bgp bgp = new Bgp();
        bgp.setLocalAS(65104);
        bgp.setPeerAS(23637);
        bgp.setPeerAddr("180.214.47.65");
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, bgp);
        System.out.println(response.getLocation());

        url = new StringBuilder(response.getLocation().toString()).append(
                "/ad_routes").toString();
        resource = client.resource(url);
        AdRoute adRt = new AdRoute();
        adRt.setNwPrefix("14.128.23.0");
        adRt.setPrefixLength((byte)27);
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, adRt);
        System.out.println(response.getLocation());

        // Now add a port for 10.0.4.0/24
        url = new StringBuilder(router).append("/ports").toString();
        resource = client.resource(url);
        port.setNetworkAddress("10.0.4.0");
        port.setNetworkLength(24);
        port.setPortAddress("10.0.4.1");
        port.setLocalNetworkAddress("10.0.4.0");
        port.setLocalNetworkLength(24);
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, port);
        portPath = response.getLocation().toString();
        System.out.println(portPath);
        parts = portPath.split("/");
        String portId = parts[parts.length - 1];
        System.out.println("Got port id: " + portId);

        // Add a route to 10.0.4.0/24
        url = new StringBuilder(router).append("/routes").toString();
        resource = client.resource(url);
        Route rt = new Route();
        rt.setSrcNetworkAddr("0.0.0.0");
        rt.setSrcNetworkLength(0);
        rt.setDstNetworkAddr("10.0.4.0");
        rt.setDstNetworkLength(0);
        rt.setType("Normal");
        rt.setNextHopPort(UUID.fromString(portId));
        rt.setWeight(10);
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, rt);
        System.out.println(response.getLocation());

        // Now add a port for 10.0.5.0/24
        url = new StringBuilder(router).append("/ports").toString();
        resource = client.resource(url);
        port.setNetworkAddress("10.0.5.0");
        port.setNetworkLength(24);
        port.setPortAddress("10.0.5.1");
        port.setLocalNetworkAddress("10.0.5.0");
        port.setLocalNetworkLength(24);
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, port);
        portPath = response.getLocation().toString();
        System.out.println(portPath);
        parts = portPath.split("/");
        portId = parts[parts.length - 1];
        System.out.println("Got port id: " + portId);

        // Add a route to 10.0.5.0/24
        url = new StringBuilder(router).append("/routes").toString();
        resource = client.resource(url);
        rt.setSrcNetworkAddr("0.0.0.0");
        rt.setSrcNetworkLength(0);
        rt.setDstNetworkAddr("10.0.5.0");
        rt.setDstNetworkLength(0);
        rt.setType("Normal");
        rt.setNextHopPort(UUID.fromString(portId));
        rt.setWeight(10);
        response = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .post(ClientResponse.class, rt);
        System.out.println(response.getLocation());

        url = new StringBuilder(router).append("/chains").toString();
        resource = client.resource(url);
        Chain[] chains = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .get(Chain[].class);
        System.out.println("Listing chains in router:");
        for (int i = 0; i < chains.length; i++)
            System.out.println("Chain named " + chains[i].getName() +
                    " has id " + chains[i].getId().toString());

        url = new StringBuilder(router).append("/tables/nat/chains/pre_routing")
                .toString();
        resource = client.resource(url);
        Chain chain = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .get(Chain.class);
        String pre_chain = chain.getId().toString();
        System.out.println("Pre-routing chain id: " + pre_chain);

        url = new StringBuilder(router).append("/tables/nat/chains/post_routing")
                .toString();
        resource = client.resource(url);
        chain = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .get(Chain.class);
        String post_chain = chain.getId().toString();
        System.out.println("Post-routing chain id: " + pre_chain);

        /*
        url = new StringBuilder(basePath).append("/chains/")
                .append(pre_chain)
                .append("/rules").toString();
        resource = client.resource(url);
        Rule rule = new Rule();
        rule.setInPorts(new UUID [] {UUID.fromString(bgpPortId)});
        rule.setNwProto(17);
        rule.setNwDstAddress("14.128.23.17");
        rule.setNwDstLength(32);
        rule.setType("dnat");
        rule.setFlowAction("accept");
        String[][][] targets = new String [2][2][1];
        targets[0] = new String[2][2];
        
        
        rule.setNatTargets(new String[] {new String[] {new String [] {
                "10.0.4.135", "10.0.4.135"},
                new String [] {"0", "0"}}});

        String answer = resource.type(MediaType.APPLICATION_JSON)
                .header("HTTP_X_AUTH_TOKEN", "999888777666")
                .get(String.class);
        System.out.println(answer);

       "flowAction": "accept", "natTargets": [[["14.128.23.2", "14.128.23.2"], [80, 80]]], "position": 1}

        //*/
    }

}
