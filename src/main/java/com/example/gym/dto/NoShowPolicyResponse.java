package com.example.gym.dto;

public class NoShowPolicyResponse {
    private int threshold;
    private int windowDays;
    private int defaultThreshold;
    private int defaultWindowDays;

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public int getDefaultThreshold() {
        return defaultThreshold;
    }

    public void setDefaultThreshold(int defaultThreshold) {
        this.defaultThreshold = defaultThreshold;
    }

    public int getDefaultWindowDays() {
        return defaultWindowDays;
    }

    public void setDefaultWindowDays(int defaultWindowDays) {
        this.defaultWindowDays = defaultWindowDays;
    }
}
