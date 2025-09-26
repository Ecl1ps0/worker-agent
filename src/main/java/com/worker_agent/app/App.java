package com.worker_agent.app;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

@SuppressWarnings("CallToPrintStackTrace")
public class App {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();

        // String mainHost = args.length > 0 ? args[0] : "localhost";
        String mainHost = System.getenv("MAIN_HOST");
        profile.setParameter(Profile.MAIN_HOST, mainHost);
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.GUI, "false");

        String localHost;
        try {
            localHost = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            localHost = "127.0.0.1";
        }
        profile.setParameter(Profile.LOCAL_HOST, localHost);
        profile.setParameter(Profile.LOCAL_PORT, "1099");

        ContainerController container = rt.createAgentContainer(profile);

        try {
            AgentController worker = container.createNewAgent(
                    "worker",
                    "com.worker_agent.agents.Worker",
                    null
            );
            worker.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
