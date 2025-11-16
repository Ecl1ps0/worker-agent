package com.worker_agent.agents;

import com.worker_agent.utils.ModelTrainer;
import com.worker_agent.utils.SystemLoadMeter;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.concurrent.Executors;

public class Worker extends Agent {
    final private HttpClient client = HttpClient.newBuilder()
                                    .executor(Executors.newFixedThreadPool(4))
                                    .build();
    final private ModelTrainer trainer = new ModelTrainer(client);
    final private String uploadURL = String.format("http://%s:8080/upload/", System.getProperty("MAIN_HOST"));

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    protected void setup() {
        // Register to Yellow Pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName("worker-" + System.currentTimeMillis());
        sd.setType("model-trainer");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    return;
                }

                switch (msg.getPerformative()) {
                    case ACLMessage.QUERY_IF:
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        
                        reply.setContent(String.valueOf(SystemLoadMeter.getAvgLoad()));
                        
                        send(reply);

                        break;

                    case ACLMessage.REQUEST:
                        new Thread(() -> {
                            try {
                                long start = System.currentTimeMillis();

                                File trainedModel = trainer.startTraining(msg.getContent());
                                System.out.println("Start training model: " + trainedModel.getName());

                                long duration = System.currentTimeMillis() - start;
                                double durationSeconds = duration / 1000.0;

                                // Upload model
                                HttpRequest req = HttpRequest.newBuilder(new URI(uploadURL + trainedModel.getName()))
                                        .header("Content-Type", "application/octet-stream")
                                        .POST(BodyPublishers.ofFile(trainedModel.toPath()))
                                        .build();

                                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                                System.out.println("The trained model sent with status code: " + res.statusCode());

                                // Send back CONFIRM + metadata
                                ACLMessage confirm = msg.createReply();
                                confirm.setPerformative(ACLMessage.CONFIRM);
                                confirm.setContent(msg.getContent() + "|" + durationSeconds);
                                send(confirm);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                        break;
                
                    default:
                        System.out.println("Unknown message");
                        break;
                }
            }
        });
    }
}