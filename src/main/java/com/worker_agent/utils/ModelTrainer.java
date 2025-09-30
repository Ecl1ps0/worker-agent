package com.worker_agent.utils;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class ModelTrainer {
    private volatile Process trainProcess;
    private HttpClient client;

    public ModelTrainer(HttpClient client) {
        this.client = client;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public File startTraining(String url) throws IOException, InterruptedException {
        String filePath = this.getFilePath(url);
        if (filePath.isEmpty()) {
            throw new IOException("The File path is empty!");
        }

        ProcessBuilder pb = new ProcessBuilder(filePath);
        pb.redirectErrorStream(true);
        this.trainProcess = pb.start();

        Thread outputThread = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(this.trainProcess.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[TRAINER] " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        outputThread.start();

        int exitCode = this.trainProcess.waitFor();
        outputThread.join();
        if (exitCode != 0) {
            throw new IOException("Training failed with exit code: " + exitCode);
        }

        File trainedModel = new File("./trained_model.pkl");
        if (!new File(filePath).delete()) {
            throw new IOException("Fail to delete exe file!");
        }
        if (!trainedModel.exists()) {
            throw new FileNotFoundException("Training succeeded but trained_model.pkl not found!");
        }

        return trainedModel;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public File getLatestCheckpoint() {
        Path checkpointsPath = Paths.get("../checkpoints/");
        try (Stream<Path> pathes = Files.list(checkpointsPath)) {
            Optional<Path> lastModifiedFilePath = pathes.filter(d -> !Files.isDirectory(d))
                    .max(Comparator.comparingLong(f -> f.toFile().lastModified()));

            return lastModifiedFilePath.map(Path::toFile).orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private String getFilePath(String urlFile) {
        try {
            String fileExtension = urlFile.substring(urlFile.lastIndexOf('.'));
            Path tempFile = Files.createTempFile("downloaded_", fileExtension);
            tempFile.toFile().setExecutable(true);

            HttpRequest req = HttpRequest.newBuilder(new URI(urlFile))
                .GET()
                .build();

            this.client.send(req, HttpResponse.BodyHandlers.ofFile(tempFile));

            return tempFile.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}
