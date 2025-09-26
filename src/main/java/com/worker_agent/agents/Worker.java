package com.worker_agent.agents;

import com.worker_agent.utils.ModelTrainer;
import com.worker_agent.utils.SystemLoadMeter;
import com.worker_agent.utils.UserActivityDetector;

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

public class Worker extends Agent {
    final private HttpClient client = HttpClient.newHttpClient();
    final private ModelTrainer trainer = new ModelTrainer(client);
    final private String uploadURL = String.format("http://%s:8080/upload/", System.getenv("MAIN_HOST"));

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

                var reply = msg.createReply();
                switch (msg.getPerformative()) {
                    case ACLMessage.QUERY_IF:
                        reply.setPerformative(ACLMessage.INFORM);

                        if (UserActivityDetector.isUserActiveRecently(60000)) {
                            reply.setContent(String.valueOf(SystemLoadMeter.getAvgLoad()));
                        } else reply.setContent("100.00");

                        send(reply);

                        break;

                    case ACLMessage.REQUEST:
                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setContent("Training started on" + dfd.getName());
                        send(reply);

                        new Thread(() -> {
                            try {
                                File trainedModel = trainer.startTraining(msg.getContent());
                                System.out.println("Start training model: " + trainedModel.getName());
                                
                                HttpRequest req = HttpRequest.newBuilder(new URI(uploadURL + trainedModel.getName()))
                                    .header("Content-Type", "application/octet-stream")
                                    .POST(BodyPublishers.ofFile(trainedModel.toPath()))
                                    .build();

                                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                                System.out.println("The trained model sent with status code: " + res.statusCode());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                        break;
                
                    default:
                        System.out.println("Unknown message");

                        break;
                }

                block();
            }
        });
    }
}