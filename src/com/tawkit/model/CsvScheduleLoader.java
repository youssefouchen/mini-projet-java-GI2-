package com.tawkit.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * [MEMBRE 1] Chargeur du fichier CSV contenant les 366 jours de l'année.
 * Format attendu : Mois;Jour;Fajr;Chourouq;Dohr;Asr;Maghrib;Icha
 */
public class CsvScheduleLoader {

    public DailyPrayerTimes loadForDate(String csvFilePath, LocalDate date) {
        // TODO [MEMBRE 1] : Lire le fichier CSV en UTF-8 avec BufferedReader
        return new DailyPrayerTimes(
            LocalTime.of(5, 45),
            LocalTime.of(7, 10),
            LocalTime.of(12, 35),
            LocalTime.of(15, 30),
            LocalTime.of(17, 55),
            LocalTime.of(19, 15)
        );
    }
}
