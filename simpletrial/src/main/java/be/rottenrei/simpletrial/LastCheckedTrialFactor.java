package be.rottenrei.simpletrial;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;


/**
 * Stores the last trial check timestamp from a shared preferences file and optionally triggers a
 * backup via the {@link BackupManager} after updating the timestamp. If the last check timestamp is
 * in the future, it is assumed that the device date was set back to extend the trial, and
 * {@link #readTimestamp} returns {@link #TRIAL_INVALID_TIMESTAMP}. This factor can trigger a
 * licence invalidation if the device date is set back for other reasons, or by accident.
 * Shared preference files are lost when the user clears the app's cache or reinstall. They are, however, restored
 * automatically upon a reinstallation if they were previously backed up with the
 * {@link BackupManager}. Reinstalling and then clearing the cache is then the only way to
 * permanently remove the value stored in the shared preference file.
 * <p>
 * Please note that in order for the backup to work, you will either have to enable Android
 * AutoBackup or create a {@link android.app.backup.BackupAgentHelper}. C.f. <a
 * href="https://developer.android.com/guide/topics/data/backup.html">the Android
 * developer documentation</a>
 */
public class LastCheckedTrialFactor extends TrialFactor {

    /**
     * The default shared preference file to cache the trial start timestamp in.
     */
    public static final String DEFAULT_SHARED_PREFERENCES_FILE = "simple_trial";

    /**
     * The default name of the shared preference under which the trial start timestamp will be
     * cached.
     */
    public static final String DEFAULT_PREFERENCE_NAME = "last_check";

    /**
     * The configuration for this factor.
     */
    private final Config config;

    /**
     * @see LastCheckedTrialFactor
     */
    public LastCheckedTrialFactor(Config config) {
        this.config = config;
    }

    /**
     * Calls {@link #readTimestamp(Context)}. If it returns {@link #NOT_AVAILABLE_TIMESTAMP}, then
     * persists the current time to this factor's backing storage method, otherwise persists {@link #TRIAL_INVALID_TIMESTAMP}.
     */
    @Override
    public void persistTimestamp(long timestamp, Context context) {
        SharedPreferences preferences = context
                .getSharedPreferences(config.preferenceFile, Context.MODE_PRIVATE);
        // No need to use the provided timestamp
        boolean isTrialValid = readTimestamp(context) == NOT_AVAILABLE_TIMESTAMP;
        // Persist the timestamp of the current check, millisecond accuracy not required
        long time;
        if (isTrialValid)
            time = System.currentTimeMillis();
        else
            time = TRIAL_INVALID_TIMESTAMP;
        String timeStr = Long.toString(time);
        String timeEncoded = Base64.encodeToString(timeStr.getBytes(), Base64.DEFAULT);
        preferences.edit().putString(config.preferenceName, timeEncoded).apply();

        if (config.shouldTriggerBackup) {
            new BackupManager(context).dataChanged();
        }
    }

    /**
     * Reads the persisted timestamp from this factor's backing storage method. If no timestamp
     * has been stored yet, or it has been cleared/reset, or the timestamp is in the future,
     * but not {@link #NOT_AVAILABLE_TIMESTAMP}, returns {@link #TRIAL_INVALID_TIMESTAMP}.
     * Returns {@link #NOT_AVAILABLE_TIMESTAMP} otherwise. Does NOT return the currently persisted timestamp.
     */
    @Override
    public long readTimestamp(Context context) {
        SharedPreferences preferences = context
                .getSharedPreferences(config.preferenceFile, Context.MODE_PRIVATE);
        String notAvailable = Long.toString(NOT_AVAILABLE_TIMESTAMP);
        String timeEncoded = preferences.getString(config.preferenceName, notAvailable);
        long timestamp;
        if (notAvailable.equals(timeEncoded))
            return NOT_AVAILABLE_TIMESTAMP;
        else {
            timestamp = Long.parseLong(new String(Base64.decode(timeEncoded, Base64.DEFAULT)));
            long currentTime = System.currentTimeMillis();
            // Assume intentional device time tampering and invalidate the trial
            if (currentTime < timestamp)
                return TRIAL_INVALID_TIMESTAMP;
            else
                return NOT_AVAILABLE_TIMESTAMP;
        }
    }

    /**
     * Configuration for the {@link LastCheckedTrialFactor}. If you don't change any of the
     * defaults, the timestamp will be stored in a shared preference file named
     * {@value LastCheckedTrialFactor#DEFAULT_SHARED_PREFERENCES_FILE} in a preference named
     * {@value LastCheckedTrialFactor#DEFAULT_PREFERENCE_NAME}
     * and a backup will be triggered when persisting the timestamp.
     */
    public static class Config {

        /**
         * The shared preferences file to cache the start timestamp in.
         */
        private String preferenceFile = DEFAULT_SHARED_PREFERENCES_FILE;

        /**
         * The name of the preference under which to cache the start timestamp.
         */
        private String preferenceName = DEFAULT_PREFERENCE_NAME;

        /**
         * Whether the {@link BackupManager} should be triggered after updating the trial start
         * timestamp. This helps in persisting the timestamp cached in the shared preference
         * across reinstallations of the app.
         */
        private boolean shouldTriggerBackup = true;

        /**
         * Changes the name of the shared preference file in which the timestamp will be persisted.
         */
        public Config preferenceFile(String preferenceFile) {
            this.preferenceFile = preferenceFile;
            return this;
        }

        /**
         * Changes the name of the preference under which the timestamp will be persisted.
         */
        public Config preferenceName(String preferenceName) {
            this.preferenceName = preferenceName;
            return this;
        }

        /**
         * Changes whether the {@link BackupManager} should be invoked after persisting the
         * timestamp.
         */
        public Config shouldTriggerBackup(boolean shouldTriggerBackup) {
            this.shouldTriggerBackup = shouldTriggerBackup;
            return this;
        }
    }
}
