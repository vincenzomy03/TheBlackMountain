/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.type;

/**
 * Enum che definisce i tipi di arma nel gioco
 *
 * @author vince
 */
public enum WeaponType {

    /**
     * Spada da combattimento
     */
    SWORD("Spada", "Arma da mischia classica e versatile"),
    /**
     * Arco a distanza
     */
    BOW("Arco", "Arma a distanza precisa e letale"),
    /**
     * Arma magica
     */
    MAGIC("Magica", "Arma potenziata da energia arcana"),
    /**
     * Bastone
     */
    STAFF("Bastone", ""),
    /**
     * Libro del fuoco
     */
    BOOK("Libro del fuoco", ""),
    /**
     * Veleno
     */
    POISON("Ampolla di veleno", ""),
    /**
     * Nessuna arma (combattimento a mani nude)
     */
    UNARMED("Disarmato", "Combattimento a mani nude");

    private final String displayName;
    private final String description;

    /**
     * Costruttore per WeaponType
     *
     * @param displayName nome da visualizzare
     * @param description descrizione del tipo di arma
     */
    WeaponType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Restituisce il nome da visualizzare
     *
     * @return nome visualizzabile
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Restituisce la descrizione del tipo di arma
     *
     * @return descrizione
     */
    public String getDescription() {
        return description;
    }

    /**
     * Calcola il modificatore di danno basato sul tipo di arma AGGIORNATO per
     * includere i nuovi tipi
     *
     * @return modificatore percentuale (1.0 = normale, 1.2 = +20%, etc.)
     */
    public double getDamageModifier() {
        switch (this) {
            case BOOK:
                return 1.5; // +50% danno per libri magici
            case MAGIC:
                return 1.3; // +30% danno per armi magiche
            case BOW:
                return 1.2; // +20% danno per archi
            case SWORD:
                return 1.15; // +15% danno per spade
            case STAFF:
                return 1.1; // +10% danno per bastoni
            case POISON:
                return 1.0; // Il veleno ha effetto speciale, non bonus diretto
            case UNARMED:
                return 0.7; // -30% danno a mani nude
            default:
                return 1.0;
        }
    }

    /**
     * Restituisce la precisione base del tipo di arma AGGIORNATO per includere
     * i nuovi tipi
     *
     * @return valore di precisione (0-100)
     */
    public int getBaseAccuracy() {
        switch (this) {
            case BOOK:
            case MAGIC:
                return 90; // Altissima precisione per magia
            case BOW:
                return 85; // Alta precisione
            case SWORD:
                return 80; // Buona precisione
            case STAFF:
                return 75; // Discreta precisione
            case POISON:
                return 95; // Il veleno colpisce sempre
            case UNARMED:
                return 70; // Precisione discreta
            default:
                return 70;
        }
    }
}
