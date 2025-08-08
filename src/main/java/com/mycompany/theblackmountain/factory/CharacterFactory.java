/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.factory;

import com.mycompany.theblackmountain.type.GameCharacter;
import com.mycompany.theblackmountain.type.CharacterType;

/**
 * Factory class per creare personaggi con statistiche predefinite
 *
 * @author vince
 */
public class CharacterFactory {

    /**
     * Crea il protagonista del gioco
     *
     * @return GameCharacter protagonista
     */
    public static GameCharacter createPlayer() {
        return new GameCharacter(
                0,
                "Avventuriero",
                "Un coraggioso avventuriero che si addentra nella Montagna Nera per salvare la principessa.",
                100, // HP
                15, // Attacco
                5, // Difesa
                CharacterType.PLAYER
        );
    }

    /**
     * Crea un Goblin normale
     *
     * @param id ID univoco del goblin
     * @return GameCharacter goblin
     */
    public static GameCharacter createGoblin(int id) {
        return new GameCharacter(
                1,
                "Goblin",
                "Una creatura malvagia dalla pelle verde scuro, con occhi pieni d'odio e artigli affilati.",
                40, // HP
                12, // Attacco
                3, // Difesa
                CharacterType.GOBLIN
        );
    }

    /**
     * Crea un Goblin Gigante (più forte)
     *
     * @param id ID univoco del goblin gigante
     * @return GameCharacter goblin gigante
     */
    public static GameCharacter createGiantGoblin(int id) {
        return new GameCharacter(
                2,
                "Goblin Gigante",
                "Un goblin di dimensioni enormi che impugna una clava insanguinata. La sua forza è leggendaria.",
                70, // HP
                18, // Attacco
                6, // Difesa
                CharacterType.GOBLIN
        );
    }

    /**
     * Crea un Topo Gigante
     *
     * @param id ID univoco del topo
     * @return GameCharacter topo gigante
     */
    public static GameCharacter createGiantRat(int id) {
        return new GameCharacter(
                3,
                "Topo Gigante",
                "Un enorme topo con denti giallastri e occhi rossi che brillano nel buio. Veloce e aggressivo.",
                35, // HP
                10, // Attacco
                2, // Difesa
                CharacterType.GIANT_RAT
        );
    }

    /**
     * Crea il Boss finale - Cane Demone
     *
     * @return GameCharacter boss
     */
    public static GameCharacter createDemonDog() {
        return new GameCharacter(
                4,
                "Cane Demone",
                "Una creatura infernale con zanne fumanti e occhi di fuoco. Il suo pelo nero come la notte emana un calore terrificante.",
                150, // HP
                25, // Attacco
                10, // Difesa
                CharacterType.DEMON_DOG
        );
    }

    /**
     * Crea un personaggio personalizzato
     *
     * @param id ID univoco
     * @param name nome del personaggio
     * @param description descrizione
     * @param hp punti vita
     * @param attack attacco
     * @param defense difesa
     * @param type tipo di personaggio
     * @return GameCharacter personalizzato
     */
    public static GameCharacter createCustomCharacter(int id, String name, String description,
            int hp, int attack, int defense, CharacterType type) {
        return new GameCharacter(id, name, description, hp, attack, defense, type);
    }

    /**
     * Restituisce le statistiche base per un tipo di personaggio
     *
     * @param type tipo di personaggio
     * @return array con [hp, attack, defense]
     */
    public static int[] getBaseStatsForType(CharacterType type) {
        return switch (type) {
            case PLAYER ->
                new int[]{100, 15, 5};
            case GOBLIN ->
                new int[]{40, 12, 3};
            case GIANT_RAT ->
                new int[]{35, 10, 2};
            case DEMON_DOG ->
                new int[]{150, 25, 10};
            default ->
                new int[]{30, 8, 2};
        };
    }
}
