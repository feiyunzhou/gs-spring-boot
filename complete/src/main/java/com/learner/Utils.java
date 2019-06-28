package com.learner;

import java.util.Date;
import java.util.UUID;

public class Utils {
    static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;
    public static long getTimeFromUUID(UUID uuid) {
        return (uuid.timestamp() - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH) / 10000;
    }

    public static Date timeUUID2Date(UUID uuid) {
        long time = getTimeFromUUID(uuid);
        return new Date(time);
    }

}
