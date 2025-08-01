/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.theblackmountain.factory;

import com.mycompany.theblackmountain.type.Weapon;
import com.mycompany.theblackmountain.type.WeaponType;

/**
 * Factory per creare armi del gioco
 * @author vince
 */
public class WeaponFactory {
    
    /**
     * Crea una spada base
     * @return Weapon spada
     */
    public static Weapon createSword() {
        return new Weapon(12, "spada", 
            "Una spada d'acciaio ben bilanciata. Arma affidabile per il combattimento.", 
            8, WeaponType.SWORD, 10, 2);
    }
    
    /**
     * Crea un arco magico
     * @return Weapon arco magico
     */
    public static Weapon createMagicBow() {
        return new Weapon(7, "arco magico", 
            "Un arco leggero ma potente, creato con energia arcana e materiali raccolti nella fortezza.", 
            12, WeaponType.MAGIC, 15, 2);
    }
    
    /**
     * Crea un bastone
     * @return Weapon bastone
     */
    public static Weapon createStaff() {
        return new Weapon(6, "bastone", 
            "Un robusto bastone di legno. Può essere usato come arma o combinato con altri oggetti.", 
            5, WeaponType.STAFF, 5, 2);
    }
    
    /**
     * Crea frecce
     * @return Weapon frecce
     */
    public static Weapon createArrows() {
        return new Weapon(3, "frecce", 
            "Un piccolo fascio di frecce con punte d'acciaio. Sembrano leggere ma letali.", 
            3, WeaponType.ARROWS, 5, 2);
    }
    
    /**
     * Crea un'arma specifica basata sull'ID
     * @param weaponId ID dell'arma da creare
     * @return Weapon corrispondente o null se non trovata
     */
    public static Weapon createWeaponById(int weaponId) {
        switch (weaponId) {
            case 3:
                return createArrows();
            case 6:
                return createStaff();
            case 7:
                return createMagicBow();
            case 12:
                return createSword();
            default:
                return null;
        }
    }
    
    /**
     * Crea un'arma potenziata con veleno
     * @param baseWeapon arma base
     * @param poisonDamage danno del veleno
     * @return arma avvelenata
     */
    public static Weapon createPoisonedWeapon(Weapon baseWeapon, int poisonDamage) {
        Weapon poisonedWeapon = createWeaponById(baseWeapon.getId());
        if (poisonedWeapon != null) {
            poisonedWeapon.applyPoison(poisonDamage);
        }
        return poisonedWeapon;
    }
    
    /**
     * Crea un'arma improvvisata da un oggetto normale
     * @param objectId ID dell'oggetto
     * @param objectName nome dell'oggetto
     * @return arma improvvisata
     */
    public static Weapon createImprovisedWeapon(int objectId, String objectName) {
        return new Weapon(objectId, objectName + " (improvvisata)", 
            "Un " + objectName + " usato come arma improvvisata.", 
            2, WeaponType.IMPROVISED, 2, 2);
    }
}