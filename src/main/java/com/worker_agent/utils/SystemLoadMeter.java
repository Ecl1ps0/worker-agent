package com.worker_agent.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.management.OperatingSystemMXBean;

public interface SystemLoadMeter {
    BigDecimal wGpu = BigDecimal.valueOf(0.5);
    BigDecimal wCpu = BigDecimal.valueOf(0.3);
    BigDecimal wRam = BigDecimal.valueOf(0.2);

    private static BigDecimal safeLoad(double load) {
        if (Double.isNaN(load) || load < 0) return BigDecimal.ZERO;
        if (load > 1.0) return BigDecimal.ONE;
        return BigDecimal.valueOf(load);
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || v < 0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static String osName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    private static boolean isWindows() {
        return osName().contains("win");
    }

    private static boolean isLinux() {
        return osName().contains("linux");
    }

    private static boolean isMac() {
        return osName().contains("mac");
    }

    private static String runCommand(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append("\n");
            }
        }

        int code = p.waitFor();
        if (code != 0) {
            throw new RuntimeException("Command failed (" + code + "): " + String.join(" ", cmd));
        }
        return out.toString();
    }

    private static double getCPULoad() {
        var osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return osBean.getProcessCpuLoad();
    }

    private static double getRAMLoad() {
        var osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long total = osBean.getTotalMemorySize();
        long free = osBean.getFreeMemorySize();
        return 1.0 - ((double) free / total);
    }

    private static double parseFirstPercentNumber(String text) {
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(text);
        while (m.find()) {
            double value = Double.parseDouble(m.group(1));
            if (value >= 0 && value <= 100) {
                return value / 100.0;
            }
        }
        return 0.0;
    }

    private static double getGpuLoadWindows() throws Exception {
        String out = runCommand(List.of(
                "cmd.exe", "/c",
                "typeperf \"\\GPU Engine(*)\\Utilization Percentage\" -sc 2"
        ));

        String[] lines = out.split("\\R");
        String lastData = null;

        for (int i = lines.length - 1; i >= 0; i--) {
            String s = lines[i].trim();
            if (s.startsWith("\"") && s.contains(",") && !s.toLowerCase().contains("exiting")) {
                lastData = s;
                break;
            }
        }

        if (lastData == null) return 0.0;

        Pattern num = Pattern.compile("\"(-?\\d+(?:\\.\\d+)?)\"");
        Matcher m = num.matcher(lastData);

        List<Double> values = new ArrayList<>();
        while (m.find()) {
            values.add(Double.parseDouble(m.group(1)));
        }

        if (values.isEmpty()) return 0.0;

        double maxPct = values.stream().mapToDouble(v -> v).max().orElse(0.0);
        return clamp01(maxPct / 100.0);
    }

    private static double getGpuLoadLinux() throws Exception {
        String[] commands = {
                "nvidia-smi --query-gpu=utilization.gpu --format=csv,noheader,nounits",
                "rocm-smi --showuse",
                "amd-smi --query=gpu_usage --format=csv,noheader,nounits"
        };

        for (String cmd : commands) {
            try {
                String out = runCommand(List.of("bash", "-c", cmd));
                double load = parseFirstPercentNumber(out);
                if (load > 0.0) return clamp01(load);
            } catch (Exception ignored) {
            }
        }

        return 0.0;
    }

    private static double getGpuLoadMac() throws Exception {
        String out = runCommand(List.of(
                "bash", "-c",
                "powermetrics --samplers gpu_power -n1"
        ));
        return clamp01(parseFirstPercentNumber(out));
    }

    private static double getGpuLoad() {
        try {
            if (isWindows()) {
                return getGpuLoadWindows();
            } else if (isLinux()) {
                return getGpuLoadLinux();
            } else if (isMac()) {
                return getGpuLoadMac();
            }
        } catch (Exception e) {
            System.err.println("GPU load unavailable: " + e.getMessage());
        }
        return 0.0;
    }

    static BigDecimal getCpuPercent() {
        return safeLoad(getCPULoad())
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal getRamPercent() {
        return safeLoad(getRAMLoad())
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal getGpuPercent() {
        return safeLoad(getGpuLoad())
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal getAvgLoad() {
        BigDecimal gpuLoad = getGpuPercent();
        BigDecimal cpuLoad = getCpuPercent();
        BigDecimal ramLoad = getRamPercent();

        return gpuLoad.multiply(wGpu)
                .add(cpuLoad.multiply(wCpu))
                .add(ramLoad.multiply(wRam))
                .setScale(2, RoundingMode.HALF_UP);
    }
}