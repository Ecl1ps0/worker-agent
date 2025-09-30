package com.worker_agent.app;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import com.worker_agent.utils.IPReciever;

@SuppressWarnings("CallToPrintStackTrace")
public class App {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();

        String mainHost = System.getProperty("MAIN_HOST");
        profile.setParameter(Profile.MAIN_HOST, mainHost);
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.GUI, "false");

        try {
            profile.setParameter(Profile.LOCAL_HOST, IPReciever.getLocalIp());
            profile.setParameter(Profile.LOCAL_PORT, "1099");
        } catch (SocketException e) {
            e.printStackTrace();
        }

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
