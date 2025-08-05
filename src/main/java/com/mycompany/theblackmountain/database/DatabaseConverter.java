package com.mycompany.theblackmountain.database;

import com.mycompany.theblackmountain.database.entities.*;
import com.mycompany.theblackmountain.type.*;
import com.mycompany.theblackmountain.factory.WeaponFactory;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Classe per convertire entità del database in oggetti del gioco e viceversa
 * @author vince
 */
public class DatabaseConverter {
    
    /**
     * Converte un RoomEntity in Room
     */
    public static Room toRoom(RoomEntity entity) {
        if (entity == null) return null;
        
        Room room = new Room(entity.getId(), entity.getName(), entity.getDescription());
        room.setLook(entity.getLookDescription());
        room.setVisible(entity.isVisible());
        
        // Le connessioni alle altre stanze verranno impostate separatamente
        // dopo aver caricato tutte le stanze
        
        return room;
    }
    
    /**
     * Converte una Room in RoomEntity
     */
    public static RoomEntity toRoomEntity(Room room) {
        if (room == null) return null;
        
        RoomEntity entity = new RoomEntity();
        entity.setId(room.getId());
        entity.setName(room.getName());
        entity.setDescription(room.getDescription());
        entity.setLookDescription(room.getLook());
        entity.setVisible(room.isVisible());
        
        // Imposta gli ID delle stanze connesse
        entity.setNorthRoomId(room.getNorth() != null ? room.getNorth().getId() : null);
        entity.setSouthRoomId(room.getSouth() != null ? room.getSouth().getId() : null);
        entity.setEastRoomId(room.getEast() != null ? room.getEast().getId() : null);
        entity.setWestRoomId(room.getWest() != null ? room.getWest().getId() : null);
        
        return entity;
    }
    
    /**
     * Converte un ObjectEntity in Objects
     */
    public static Objects toObject(ObjectEntity entity) {
        if (entity == null) return null;
        
        Objects obj;
        
        // Crea il tipo appropriato di oggetto
        if ("CONTAINER".equals(entity.getObjectType())) {
            obj = new ContainerObj(entity.getId(), entity.getName(), entity.getDescription());
        } else {
            obj = new Objects(entity.getId(), entity.getName(), entity.getDescription());
        }
        
        // Imposta le proprietà
        obj.setOpenable(entity.isOpenable());
        obj.setPickupable(entity.isPickupable());
        obj.setPushable(entity.isPushable());
        obj.setOpen(entity.isOpen());
        obj.setPush(entity.isPushed());
        
        // Converte gli alias da stringa separata da virgole
        if (entity.getAliases() != null && !entity.getAliases().trim().isEmpty()) {
            String[] aliasArray = entity.getAliases().split(",");
            for (int i = 0; i < aliasArray.length; i++) {
                aliasArray[i] = aliasArray[i].trim();
            }
            obj.setAlias(new HashSet<>(Arrays.asList(aliasArray)));
        }
        
        return obj;
    }
    
    /**
     * Converte Objects in ObjectEntity
     */
    public static ObjectEntity toObjectEntity(Objects obj) {
        if (obj == null) return null;
        
        ObjectEntity entity = new ObjectEntity();
        entity.setId(obj.getId());
        entity.setName(obj.getName());
        entity.setDescription(obj.getDescription());
        entity.setOpenable(obj.isOpenable());
        entity.setPickupable(obj.isPickupable());
        entity.setPushable(obj.isPushable());
        entity.setOpen(obj.isOpen());
        entity.setPushed(obj.isPush());
        
        // Determina il tipo di oggetto
        if (obj instanceof ContainerObj) {
            entity.setObjectType("CONTAINER");
        } else if (obj instanceof Weapon) {
            entity.setObjectType("WEAPON");
        } else {
            entity.setObjectType("NORMAL");
        }
        
        // Converte gli alias in stringa separata da virgole
        if (obj.getAlias() != null && !obj.getAlias().isEmpty()) {
            entity.setAliases(String.join(",", obj.getAlias()));
        }
        
        return entity;
    }
    
    /**
     * Converte un CharacterEntity in Character
     */
    public static Character toCharacter(CharacterEntity entity) {
        if (entity == null) return null;
        
        CharacterType type = CharacterType.valueOf(entity.getCharacterType());
        
        Character character = new Character(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getMaxHp(),
            entity.getAttack(),
            entity.getDefense(),
            type
        );
        
        character.setCurrentHp(entity.getCurrentHp());
        if (!entity.isAlive()) {
            // Se il personaggio è morto, imposta HP a 0 che triggera isAlive = false
            character.setCurrentHp(0);
        }
        
        return character;
    }
    
    /**
     * Converte Character in CharacterEntity
     */
    public static CharacterEntity toCharacterEntity(Character character) {
        if (character == null) return null;
        
        CharacterEntity entity = new CharacterEntity();
        entity.setId(character.getId());
        entity.setName(character.getName());
        entity.setDescription(character.getDescription());
        entity.setCharacterType(character.getType().name());
        entity.setMaxHp(character.getMaxHp());
        entity.setCurrentHp(character.getCurrentHp());
        entity.setAttack(character.getAttack());
        entity.setDefense(character.getDefense());
        entity.setAlive(character.isAlive());
        
        return entity;
    }
    
    /**
     * Converte un WeaponEntity in Weapon
     */
    public static Weapon toWeapon(WeaponEntity weaponEntity, ObjectEntity objectEntity) {
        if (weaponEntity == null || objectEntity == null) return null;
        
        WeaponType type = WeaponType.valueOf(weaponEntity.getWeaponType());
        
        Weapon weapon = new Weapon(
            objectEntity.getId(),
            objectEntity.getName(),
            objectEntity.getDescription(),
            weaponEntity.getAttackBonus(),
            type,
            weaponEntity.getCriticalChance(),
            weaponEntity.getCriticalMultiplier()
        );
        
        // Imposta proprietà dell'oggetto base
        weapon.setOpenable(objectEntity.isOpenable());
        weapon.setPickupable(objectEntity.isPickupable());
        weapon.setPushable(objectEntity.isPushable());
        weapon.setOpen(objectEntity.isOpen());
        weapon.setPush(objectEntity.isPushed());
        
        // Converte gli alias
        if (objectEntity.getAliases() != null && !objectEntity.getAliases().trim().isEmpty()) {
            String[] aliasArray = objectEntity.getAliases().split(",");
            for (int i = 0; i < aliasArray.length; i++) {
                aliasArray[i] = aliasArray[i].trim();
            }
            weapon.setAlias(new HashSet<>(Arrays.asList(aliasArray)));
        }
        
        // Applica veleno se presente
        if (weaponEntity.isPoisoned()) {
            weapon.applyPoison(weaponEntity.getPoisonDamage());
            if (weaponEntity.getSpecialEffect() != null) {
                weapon.setSpecialEffect(weaponEntity.getSpecialEffect());
            }
        }
        
        return weapon;
    }
    
    /**
     * Converte Weapon in WeaponEntity
     */
    public static WeaponEntity toWeaponEntity(Weapon weapon) {
        if (weapon == null) return null;
        
        WeaponEntity entity = new WeaponEntity();
        entity.setId(weapon.getId());
        entity.setObjectId(weapon.getId()); // Weapon estende Objects, quindi stesso ID
        entity.setWeaponType(weapon.getWeaponType().name());
        entity.setAttackBonus(weapon.getAttackBonus());
        entity.setCriticalChance(weapon.getCriticalChance());
        entity.setCriticalMultiplier(weapon.getCriticalMultiplier());
        entity.setPoisoned(weapon.isPoisoned());
        entity.setPoisonDamage(weapon.getPoisonDamage());
        entity.setSpecialEffect(weapon.getSpecialEffect());
        
        return entity;
    }
    
    /**
     * Crea un'arma dal database usando WeaponFactory come fallback
     */
    public static Weapon createWeaponFromDatabase(WeaponEntity weaponEntity, ObjectEntity objectEntity) {
        if (weaponEntity == null || objectEntity == null) return null;
        
        // Prima prova a convertire direttamente
        Weapon weapon = toWeapon(weaponEntity, objectEntity);
        
        // Se la conversione fallisce, usa WeaponFactory come fallback
        if (weapon == null) {
            weapon = WeaponFactory.createWeaponById(objectEntity.getId());
            if (weapon != null) {
                // Aggiorna con i dati dal database
                weapon.setAttackBonus(weaponEntity.getAttackBonus());
                weapon.setCriticalChance(weaponEntity.getCriticalChance());
                weapon.setCriticalMultiplier(weaponEntity.getCriticalMultiplier());
                
                if (weaponEntity.isPoisoned()) {
                    weapon.applyPoison(weaponEntity.getPoisonDamage());
                }
            }
        }
        
        return weapon;
    }
    
    /**
     * Converte una lista di RoomEntity in Room con connessioni
     */
    public static java.util.List<Room> toRoomsWithConnections(java.util.List<RoomEntity> entities) {
        if (entities == null) return new java.util.ArrayList<>();
        
        // Prima passa: crea tutte le stanze
        java.util.Map<Integer, Room> roomMap = new java.util.HashMap<>();
        for (RoomEntity entity : entities) {
            Room room = toRoom(entity);
            roomMap.put(room.getId(), room);
        }
        
        // Seconda passa: imposta le connessioni
        for (RoomEntity entity : entities) {
            Room room = roomMap.get(entity.getId());
            
            if (entity.getNorthRoomId() != null) {
                room.setNorth(roomMap.get(entity.getNorthRoomId()));
            }
            if (entity.getSouthRoomId() != null) {
                room.setSouth(roomMap.get(entity.getSouthRoomId()));
            }
            if (entity.getEastRoomId() != null) {
                room.setEast(roomMap.get(entity.getEastRoomId()));
            }
            if (entity.getWestRoomId() != null) {
                room.setWest(roomMap.get(entity.getWestRoomId()));
            }
        }
        
        return new java.util.ArrayList<>(roomMap.values());
    }
    
    /**
     * Utility per validare la consistenza dei dati
     */
    public static boolean validateRoomConnections(java.util.List<RoomEntity> rooms) {
        java.util.Set<Integer> roomIds = new java.util.HashSet<>();
        for (RoomEntity room : rooms) {
            roomIds.add(room.getId());
        }
        
        for (RoomEntity room : rooms) {
            if (room.getNorthRoomId() != null && !roomIds.contains(room.getNorthRoomId())) {
                System.err.println("Stanza " + room.getId() + " ha connessione nord invalida: " + room.getNorthRoomId());
                return false;
            }
            if (room.getSouthRoomId() != null && !roomIds.contains(room.getSouthRoomId())) {
                System.err.println("Stanza " + room.getId() + " ha connessione sud invalida: " + room.getSouthRoomId());
                return false;
            }
            if (room.getEastRoomId() != null && !roomIds.contains(room.getEastRoomId())) {
                System.err.println("Stanza " + room.getId() + " ha connessione est invalida: " + room.getEastRoomId());
                return false;
            }
            if (room.getWestRoomId() != null && !roomIds.contains(room.getWestRoomId())) {
                System.err.println("Stanza " + room.getId() + " ha connessione ovest invalida: " + room.getWestRoomId());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Utility per debug - stampa informazioni su un'entità
     */
    public static void printEntityInfo(Object entity) {
        if (entity instanceof RoomEntity) {
            RoomEntity room = (RoomEntity) entity;
            System.out.println("Room: " + room.getId() + " - " + room.getName());
        } else if (entity instanceof ObjectEntity) {
            ObjectEntity obj = (ObjectEntity) entity;
            System.out.println("Object: " + obj.getId() + " - " + obj.getName() + " (" + obj.getObjectType() + ")");
        } else if (entity instanceof CharacterEntity) {
            CharacterEntity chr = (CharacterEntity) entity;
            System.out.println("Character: " + chr.getId() + " - " + chr.getName() + " (" + chr.getCharacterType() + ")");
        } else if (entity instanceof WeaponEntity) {
            WeaponEntity wpn = (WeaponEntity) entity;
            System.out.println("Weapon: " + wpn.getId() + " - " + wpn.getWeaponType() + " (ATT+" + wpn.getAttackBonus() + ")");
        }
    }
}