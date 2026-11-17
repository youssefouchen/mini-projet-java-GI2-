package com.tawkit.model;

import java.time.LocalTime;

/**
 * [MEMBRE 1] Objet contenant les 6 heures de prière (LocalTime) d'une journée.
 */
public class DailyPrayerTimes {
    private final LocalTime fajr;
    private final LocalTime chourouq;
    private final LocalTime dohr;
    private final LocalTime asr;
    private final LocalTime maghrib;
    private final LocalTime icha;

    public DailyPrayerTimes(LocalTime fajr, LocalTime chourouq, LocalTime dohr,
                            LocalTime asr, LocalTime maghrib, LocalTime icha) {
        this.fajr = fajr;
        this.chourouq = chourouq;
        this.dohr = dohr;
        this.asr = asr;
        this.maghrib = maghrib;
        this.icha = icha;
    }

    public LocalTime getFajr() { return fajr; }
    public LocalTime getChourouq() { return chourouq; }
    public LocalTime getDohr() { return dohr; }
    public LocalTime getAsr() { return asr; }
    public LocalTime getMaghrib() { return maghrib; }
    public LocalTime getIcha() { return icha; }
}
