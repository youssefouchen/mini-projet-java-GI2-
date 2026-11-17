package com.tawkit.controller;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.tawkit.model.ConfigManager;
import com.tawkit.view.MainFrame;

/**
 * [MEMBRE 3] Interface d'administration en 3 onglets :
 * 1. Mosquée & Ville   : nom, fichier CSV d'horaires, ajustement hégirien, annonce.
 * 2. Minuteurs d'Iqama : 5 JSpinner (1 à 60 minutes) pour l'écran noir de chaque prière.
 * 3. Microphone & Calibrage Iqama : activation vocale, seuil (JSlider) et test du
 *    micro en direct (JProgressBar) pour calibrer dans la mosquée.
 *
 * Au clic sur "Enregistrer" : sauvegarde via le ConfigManager (Membre 1) puis
 * rafraîchissement instantané via reloadUI() de la MainFrame (Membre 2).
 */
public class AdminDialog extends JDialog {

    /** Dossier contenant les fichiers CSV d'horaires annuels. */
    private static final String HORAIRES_DIR = "resources/horaires";

    /** En-tête attendu dans tout fichier CSV d'horaires importé. */
    static final String CSV_HEADER = "Mois;Jour;Fajr;Chourouq;Dohr;Asr;Maghrib;Icha";

    /** Clés de configuration des 5 prières obligatoires (même nom que ConfigManager). */
    private static final String[] PRAYER_KEYS = {"fajr", "dohr", "asr", "maghrib", "icha"};
    private static final String[] PRAYER_LABELS = {"Fajr", "Dohr", "Asr", "Maghrib", "Icha"};

    private final ConfigManager configManager;
    private final MainFrame mainFrame;

    // Onglet 1 — Mosquée & Ville
    private JTextField mosqueNameField;
    private JComboBox<String> csvFileCombo;
    private JSpinner hijriAdjustmentSpinner;
    private JTextArea announcementArea;

    // Onglet 2 — Minuteurs d'Iqama (un spinner par prière)
    private final JSpinner[] iqamaSpinners = new JSpinner[PRAYER_KEYS.length];

    // Onglet 3 — Microphone & Calibrage Iqama
    private JCheckBox voiceEnabledCheckbox;
    private JSlider thresholdSlider;
    private JLabel thresholdValueLabel;
    private JProgressBar micTestBar;
    private JButton micTestButton;
    private volatile boolean micTestRunning = false;
    private Thread micTestThread;

    public AdminDialog(MainFrame mainFrame, ConfigManager configManager) {
        super(mainFrame, "Administration - Tawkit Desktop", true);
        this.mainFrame = mainFrame;
        this.configManager = configManager;

        setLayout(new BorderLayout(0, 8));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Mosquée & Ville", buildMosqueTab());
        tabbedPane.addTab("Minuteurs d'Iqama", buildIqamaTab());
        tabbedPane.addTab("Microphone & Calibrage", buildMicroTab());
        add(tabbedPane, BorderLayout.CENTER);
        add(buildButtonsPanel(), BorderLayout.SOUTH);

        setSize(620, 500);
        setLocationRelativeTo(mainFrame);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        // Si l'utilisateur ferme par la croix, on libère le micro du test en cours.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopMicTest();
            }
        });
    }

    // ------------------------------------------------------------------
    // Onglet 1 : Mosquée & Ville
    // ------------------------------------------------------------------

    private JPanel buildMosqueTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Nom de la mosquée
        mosqueNameField = new JTextField(
                configManager.getProperty("mosque.name", "Grande Mosquée"), 24);
        addFormRow(panel, 0, "Nom de la mosquée :", mosqueNameField);

        // Choix du fichier CSV dans une liste déroulante
        csvFileCombo = new JComboBox<>();
        loadCsvFiles();
        addFormRow(panel, 1, "Fichier d'horaires (CSV) :", csvFileCombo);

        // Import du fichier d'horaires de la mosquée : validation puis copie dans resources/horaires
        JButton importCsvButton = new JButton("Importer un fichier CSV…");
        importCsvButton.addActionListener(e -> importCsvFile());
        JLabel importHint = new JLabel("(validation + copie dans " + HORAIRES_DIR + ")");
        importHint.setFont(importHint.getFont().deriveFont(Font.PLAIN, 11f));
        JPanel importRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        importRow.add(importCsvButton);
        importRow.add(importHint);
        addFormRow(panel, 2, "Importer :", importRow);

        // Ajustement de la date hégirienne : -2 à +2 jours
        int hijriAdjustment = parseIntSafe(configManager.getProperty("hijri.adjustment", "0"), 0);
        hijriAdjustmentSpinner = new JSpinner(
                new SpinnerNumberModel(Math.max(-2, Math.min(2, hijriAdjustment)), -2, 2, 1));
        addFormRow(panel, 3, "Ajustement hégirien (jours) :", hijriAdjustmentSpinner);

        // Texte d'annonce
        announcementArea = new JTextArea(
                configManager.getProperty("announcement.text", ""), 4, 24);
        announcementArea.setLineWrap(true);
        announcementArea.setWrapStyleWord(true);
        JScrollPane announcementScroll = new JScrollPane(announcementArea);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.FIRST_LINE_END;
        gbc.insets = new Insets(8, 12, 8, 8);
        panel.add(new JLabel("Texte d'annonce :"), gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(8, 0, 8, 12);
        panel.add(announcementScroll, gbc);

        return panel;
    }

    /**
     * Remplit la JComboBox avec les fichiers CSV du dossier resources/horaires.
     * La valeur actuellement configurée est toujours proposée, même si absente du disque.
     */
    private void loadCsvFiles() {
        loadCsvFiles(configManager.getProperty("city.csv", "horaires_ville.csv"));
    }

    private void loadCsvFiles(String preferred) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        String current = preferred;

        File dir = new File(HORAIRES_DIR);
        File[] files = dir.listFiles((parent, name) -> name.toLowerCase().endsWith(".csv"));
        boolean currentFound = false;
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                model.addElement(file.getName());
                if (file.getName().equals(current)) {
                    currentFound = true;
                }
            }
        }
        if (!currentFound) {
            model.addElement(current);
        }
        csvFileCombo.setModel(model);
        csvFileCombo.setSelectedItem(current);
    }

    // ------------------------------------------------------------------
    // Import du fichier d'horaires de la mosquée (Onglet 1)
    // ------------------------------------------------------------------

    /**
     * Importe le fichier CSV choisi par l'administrateur :
     * 1. validation du format (en-tête + dates + heures HH:mm) ;
     * 2. copie dans resources/horaires ;
     * 3. sélection du nouveau fichier dans la JComboBox.
     * L'import ne devient effectif qu'au clic sur "Enregistrer"
     * (mise à jour de city.csv + reloadUI).
     */
    private void importCsvFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Importer le fichier d'horaires de la mosquée");
        chooser.setFileFilter(new FileNameExtensionFilter("Fichiers CSV (*.csv)", "csv"));
        chooser.setCurrentDirectory(new File(HORAIRES_DIR));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path source = chooser.getSelectedFile().toPath();
        CsvValidation validation = validateCsvFile(source);
        if (validation.error != null) {
            JOptionPane.showMessageDialog(this, validation.error, "Import impossible",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String fileName = source.getFileName().toString();
        Path destination = new File(HORAIRES_DIR, fileName).toPath();
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Copie impossible : " + e.getMessage(),
                    "Import impossible", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Le nouveau fichier apparaît dans la liste et devient sélectionné.
        loadCsvFiles(fileName);

        String message = "Fichier importé et validé : " + fileName + "\n"
                + validation.dayCount + " jours d'horaires détectés.\n\n"
                + "Cliquez sur \"Enregistrer\" pour l'appliquer à l'application.";
        if (validation.dayCount < 360) {
            message += "\n\nATTENTION : " + validation.dayCount
                    + " jours seulement (366 attendus). Les jours absents utiliseront les horaires par défaut.";
        }
        JOptionPane.showMessageDialog(this, message, "Import réussi",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** Résultat de validation d'un CSV d'horaires : erreur (ou null) + nombre de jours. */
    static CsvValidation validateCsvFile(Path csvPath) {
        int validDays = 0;
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (lineNumber == 1) {
                    if (!trimmed.replace(" ", "").equalsIgnoreCase(CSV_HEADER)) {
                        return new CsvValidation(
                                "En-tête attendu en première ligne :\n" + CSV_HEADER
                                        + "\n(reçu : " + trimmed + ")", 0);
                    }
                    continue;
                }
                String rowError = validateDataRow(trimmed, lineNumber);
                if (rowError != null) {
                    return new CsvValidation(rowError, validDays);
                }
                validDays++;
            }
        } catch (Exception e) {
            return new CsvValidation("Lecture impossible du fichier : " + e.getMessage(), 0);
        }
        if (validDays == 0) {
            return new CsvValidation("Le fichier ne contient aucune ligne de données.", 0);
        }
        return new CsvValidation(null, validDays);
    }

    /**
     * Valide une ligne de données : 8 colonnes, mois/jour cohérents, heures HH:mm.
     * @return null si la ligne est valide, sinon le message d'erreur
     */
    static String validateDataRow(String line, int lineNumber) {
        String[] parts = line.split(";", -1);
        if (parts.length < 8) {
            return "Ligne " + lineNumber + " : 8 colonnes attendues "
                    + "(Mois;Jour;Fajr;Chourouq;Dohr;Asr;Maghrib;Icha)";
        }
        try {
            int month = Integer.parseInt(parts[0].trim());
            int day = Integer.parseInt(parts[1].trim());
            if (month < 1 || month > 12) {
                return "Ligne " + lineNumber + " : mois invalide (" + parts[0].trim() + ")";
            }
            if (day < 1 || day > 31) {
                return "Ligne " + lineNumber + " : jour invalide (" + parts[1].trim() + ")";
            }
            LocalDate.of(2024, month, day); // 2024 = bissextile, valide aussi le 29 février
            for (int i = 2; i < 8; i++) {
                LocalTime.parse(parts[i].trim()); // "HH:mm" sinon DateTimeParseException
            }
        } catch (NumberFormatException | DateTimeException e) {
            return "Ligne " + lineNumber + " : données invalides (" + e.getMessage() + ")";
        }
        return null;
    }

    /** Résultat de validation d'un CSV d'horaires. */
    static class CsvValidation {
        /** null si le fichier est valide, sinon le message d'erreur complet. */
        final String error;
        /** Nombre de lignes de données validées (jours d'horaires). */
        final int dayCount;

        CsvValidation(String error, int dayCount) {
            this.error = error;
            this.dayCount = dayCount;
        }
    }

    // ------------------------------------------------------------------
    // Onglet 2 : Minuteurs d'Iqama
    // ------------------------------------------------------------------

    private JPanel buildIqamaTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel hint = new JLabel("Durée de l'écran noir (attente Iqama) après l'Adhan, pour chaque prière :");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(10, 12, 14, 12);
        panel.add(hint, gbc);

        for (int i = 0; i < PRAYER_KEYS.length; i++) {
            int minutes = parseIntSafe(configManager.getProperty("iqama." + PRAYER_KEYS[i], "15"), 15);
            iqamaSpinners[i] = new JSpinner(
                    new SpinnerNumberModel(Math.max(1, Math.min(60, minutes)), 1, 60, 1));

            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            gbc.anchor = GridBagConstraints.LINE_END;
            gbc.insets = new Insets(6, 12, 6, 10);
            panel.add(new JLabel(PRAYER_LABELS[i] + " :"), gbc);

            gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = i + 1;
            gbc.anchor = GridBagConstraints.LINE_START;
            gbc.insets = new Insets(6, 0, 6, 12);
            panel.add(iqamaSpinners[i], gbc);
        }

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = PRAYER_KEYS.length + 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(10, 12, 8, 12);
        panel.add(new JLabel("(de 1 à 60 minutes par prière)"), gbc);

        return panel;
    }

    // ------------------------------------------------------------------
    // Onglet 3 : Microphone & Calibrage Iqama
    // ------------------------------------------------------------------

    private JPanel buildMicroTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Case à cocher d'activation de la détection vocale
        voiceEnabledCheckbox = new JCheckBox("Activer la détection vocale de l'Iqama");
        voiceEnabledCheckbox.setSelected(
                Boolean.parseBoolean(configManager.getProperty("voice.enabled", "true")));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(10, 12, 12, 12);
        panel.add(voiceEnabledCheckbox, gbc);

        // Curseur de seuil de déclenchement
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(4, 12, 4, 8);
        panel.add(new JLabel("Seuil de déclenchement :"), gbc);

        int threshold = parseIntSafe(configManager.getProperty("voice.threshold", "50"), 50);
        thresholdSlider = new JSlider(0, 100, Math.max(0, Math.min(100, threshold)));
        thresholdSlider.setMajorTickSpacing(25);
        thresholdSlider.setMinorTickSpacing(5);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 0, 4, 4);
        panel.add(thresholdSlider, gbc);

        thresholdValueLabel = new JLabel(String.valueOf(thresholdSlider.getValue()));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(4, 4, 4, 12);
        panel.add(thresholdValueLabel, gbc);
        thresholdSlider.addChangeListener(
                e -> thresholdValueLabel.setText(String.valueOf(thresholdSlider.getValue())));

        // Test du micro en direct (calibrage dans la mosquée)
        micTestButton = new JButton("Tester le micro en direct");
        micTestButton.addActionListener(e -> toggleMicTest());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(16, 12, 4, 8);
        panel.add(micTestButton, gbc);

        micTestBar = new JProgressBar(0, 100);
        micTestBar.setStringPainted(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(16, 0, 4, 12);
        panel.add(micTestBar, gbc);

        JLabel calibHint = new JLabel(
                "<html>Calibrage : lancez le test et demandez à quelqu'un de prononcer l'Iqama "
                        + "depuis le micro de la mosquée. Le niveau doit rester au-dessus du seuil "
                        + "pendant environ 2,5 s pour déclencher la détection.</html>");
        calibHint.setVerticalAlignment(JLabel.TOP);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 12, 8, 12);
        panel.add(calibHint, gbc);

        return panel;
    }

    // ------------------------------------------------------------------
    // Test du micro en direct (même format et même échelle que IqamaVoiceDetector)
    // ------------------------------------------------------------------

    private void toggleMicTest() {
        if (micTestRunning) {
            stopMicTest();
        } else {
            startMicTest();
        }
    }

    private void startMicTest() {
        micTestRunning = true;
        micTestButton.setText("Arrêter le test");
        micTestThread = new Thread(this::micTestLoop, "Admin-MicTest");
        micTestThread.setDaemon(true);
        micTestThread.start();
    }

    private void stopMicTest() {
        micTestRunning = false;
        micTestButton.setText("Tester le micro en direct");
        Thread thread = micTestThread;
        if (thread != null) {
            try {
                thread.join(500); // La boucle sort en moins de 128 ms (taille du tampon)
            } catch (InterruptedException ignored) {
            }
        }
        micTestBar.setValue(0);
    }

    /**
     * Boucle de test : ouvre le micro, mesure le niveau RMS et met à jour la
     * JProgressBar en direct. Utilise le même format (16 kHz / 16 bits / Mono)
     * et le même calcul RMS que le IqamaVoiceDetector pour un calibrage fidèle.
     */
    private void micTestLoop() {
        try {
            DataLine.Info info =
                    new DataLine.Info(TargetDataLine.class, IqamaVoiceDetector.getMicAudioFormat());
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[AdminDialog] Aucune ligne microphone supportée sur ce poste.");
                micTestRunning = false;
                SwingUtilities.invokeLater(() -> micTestButton.setText("Micro indisponible"));
                return;
            }
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(IqamaVoiceDetector.getMicAudioFormat());
            line.start();
            byte[] buffer = new byte[IqamaVoiceDetector.READ_CHUNK_BYTES];
            while (micTestRunning) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) {
                    continue;
                }
                final int level = (int) IqamaVoiceDetector.computeRmsLevel(buffer, bytesRead);
                SwingUtilities.invokeLater(() -> micTestBar.setValue(level));
            }
            line.stop();
            line.close();
        } catch (Exception e) {
            System.err.println("[AdminDialog] Test micro impossible : " + e.getMessage());
            micTestRunning = false;
            SwingUtilities.invokeLater(() -> micTestButton.setText("Tester le micro en direct"));
        } finally {
            SwingUtilities.invokeLater(() -> micTestBar.setValue(0));
        }
    }

    // ------------------------------------------------------------------
    // Enregistrement
    // ------------------------------------------------------------------

    private JPanel buildButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton saveButton = new JButton("Enregistrer");
        saveButton.addActionListener(e -> onSaveClicked());
        JButton cancelButton = new JButton("Annuler");
        cancelButton.addActionListener(e -> {
            stopMicTest();
            dispose();
        });
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(saveButton);
        getRootPane().setDefaultButton(saveButton);
        return buttonsPanel;
    }

    /**
     * Contrat d'intégration : au clic sur "Enregistrer", écrire toutes les valeurs
     * dans le ConfigManager (Membre 1), sauvegarder le fichier .properties,
     * puis rafraîchir l'interface via reloadUI() de la MainFrame (Membre 2).
     */
    private void onSaveClicked() {
        stopMicTest();

        // Onglet 1 — Mosquée & Ville
        configManager.setProperty("mosque.name", mosqueNameField.getText().trim());
        Object selectedCsv = csvFileCombo.getSelectedItem();
        if (selectedCsv != null) {
            configManager.setProperty("city.csv", selectedCsv.toString());
        }
        configManager.setProperty("hijri.adjustment", String.valueOf(hijriAdjustmentSpinner.getValue()));
        configManager.setProperty("announcement.text", announcementArea.getText().trim());

        // Onglet 2 — Minuteurs d'Iqama
        for (int i = 0; i < PRAYER_KEYS.length; i++) {
            configManager.setProperty("iqama." + PRAYER_KEYS[i],
                    String.valueOf(iqamaSpinners[i].getValue()));
        }

        // Onglet 3 — Microphone & Calibrage
        configManager.setProperty("voice.enabled", String.valueOf(voiceEnabledCheckbox.isSelected()));
        configManager.setProperty("voice.threshold", String.valueOf(thresholdSlider.getValue()));

        // Sauvegarde (Membre 1) puis rafraîchissement à chaud (Membre 2)
        configManager.save();
        mainFrame.reloadUI();
        dispose();
    }

    // ------------------------------------------------------------------
    // Utilitaires de construction
    // ------------------------------------------------------------------

    /**
     * Ajoute une ligne "label : champ" dans un GridBagLayout.
     */
    private static void addFormRow(JPanel panel, int row, String labelText, Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(8, 12, 8, 8);
        panel.add(new JLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.anchor = GridBagConstraints.LINE_START;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.insets = new Insets(8, 0, 8, 12);
        panel.add(field, fieldConstraints);
    }

    /**
     * Analyse un entier de configuration avec valeur de secours si invalide.
     */
    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
