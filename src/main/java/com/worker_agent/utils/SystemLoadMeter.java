package com.worker_agent.utils;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

import com.sun.management.OperatingSystemMXBean;

public interface SystemLoadMeter {
    BigDecimal wGpu = BigDecimal.valueOf(0.5);
    BigDecimal wCpu = BigDecimal.valueOf(0.3);
    BigDecimal wRam = BigDecimal.valueOf(0.2);

    private static BigDecimal safeLoad(double load) {

        if (Double.isNaN(load) || load < 0) {
            return BigDecimal.ZERO;
        }

        if (load > 1.0) {
            return BigDecimal.ONE;
        }

        return BigDecimal.valueOf(load);
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

    private static double getVRAMLoad() {
        try {
            var process = new ProcessBuilder("nvidia-smi",
                                            "--query-gpu=utilization.gpu",
                                            "--format=csv,noheader,nounits")
                                            .start();

            try (var sc = new Scanner(process.getInputStream())) {
                if (sc.hasNextInt()) {
                    int usage = sc.nextInt();
                    return usage / 100.0;
                }
            }
        } catch (Exception e) {
            System.err.println("GPU not available (nvidia-smi not found). Skipping VRAM load.");
        }
        return 0.0; 
    }

    public static BigDecimal getAvgLoad() {
        BigDecimal gpuLoad = safeLoad(getVRAMLoad()).multiply(BigDecimal.valueOf(100));
        BigDecimal cpuLoad = safeLoad(getCPULoad()).multiply(BigDecimal.valueOf(100));
        BigDecimal ramLoad = safeLoad(getRAMLoad()).multiply(BigDecimal.valueOf(100));
        
        var load = gpuLoad.multiply(wGpu)
            .add(cpuLoad.multiply(wCpu))
            .add(ramLoad.multiply(wRam));
            
        return load.setScale(2, RoundingMode.HALF_UP);
    }
}
