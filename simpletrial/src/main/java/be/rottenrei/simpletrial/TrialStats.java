package be.rottenrei.simpletrial;

public class TrialStats {
    private boolean isTrialOver;
    private long trialTimeRemaining;

    TrialStats(boolean isTrialOver, long trialTimeRemaining) {
        this.isTrialOver = isTrialOver;
        this.trialTimeRemaining = trialTimeRemaining;
    }

    public boolean getTrialPeriodFinished() {
        return isTrialOver;
    }

    public long getTrialTimeRemaining() {
        return trialTimeRemaining;
    }
}
