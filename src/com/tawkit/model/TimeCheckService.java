package com.tawkit.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoUnit;
import com.tawkit.controller.PrayerStateController;

/**
 * [MEMBRE 1] Détecteur d'heure de prière et calcul de la date hégirienne.
 */
public class TimeCheckService {
    private final ConfigManager configManager;
    private final CsvScheduleLoader csvLoader;
    private PrayerStateController stateController;

    public TimeCheckService(ConfigManager configManager) {
        this.configManager = configManager;
        this.csvLoader = new CsvScheduleLoader();
    }

    public void setStateController(PrayerStateController stateController) {
        this.stateController = stateController;
    }

    /**
     * Contrat d'intégration : Appelé par Membre 2 (MainFrame) au démarrage et à minuit.
     */
    public DailyPrayerTimes getTodayTimes() {
        String csvFile = "resources/horaires/" + configManager.getProperty("city.csv", "horaires_ville.csv");
        return csvLoader.loadForDate(csvFile, LocalDate.now());
    }

    /**
     * Compare l'heure actuelle aux 5 prières obligatoires (hors Chourouq).
     */
    public void checkCurrentTime(LocalTime now) {
        // TODO [MEMBRE 1] : Identifier la prochaine prière et déclencher startAdhanSequence
        // si now correspond à une heure de prière à la seconde 00.
    }

    public HijrahDate getAdjustedHijriDate() {
        int adjustment = Integer.parseInt(configManager.getProperty("hijri.adjustment", "0"));
        return HijrahDate.now().plus(adjustment, ChronoUnit.DAYS);
    }
}
