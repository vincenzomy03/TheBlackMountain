-- Dati iniziali per The Black Mountain
SET AUTOCOMMIT FALSE;

-- Inserimento delle stanze
INSERT INTO rooms (id, name, description, look_description, east_room_id) VALUES 
    (0, 'Ingresso della Fortezza', 
     'Ti trovi all''ingresso della fortezza maledetta.\nL''aria è densa di umidità e il pavimento è cosparso di muschio.\nUn enorme goblin dalla pelle verde scuro ti osserva con occhi colmi d''odio. Blocca il passaggio, pronto a colpirti.',
     'Scorgi una vecchia cassa, probabilmente abbandonata dai precedenti avventurieri.\nDentro potrebbe esserci qualcosa di utile per iniziare il tuo cammino.',
     1);

INSERT INTO rooms (id, name, description, look_description, west_room_id, east_room_id, south_room_id) VALUES
    (1, 'Stanza del Topo',
     'Le pareti sono coperte di ragnatele e muffa.\nUn enorme topo, con denti giallastri e occhi rossi, grugnisce in un angolo buio.',
     'Tra le ragnatele, potresti trovare qualcosa di utile o… disgustoso.',
     0, 2, 6);

INSERT INTO rooms (id, name, description, look_description, west_room_id, east_room_id) VALUES
    (2, 'Mensa Abbandonata',
     'Le tavole di legno sono rovesciate, piatti infranti ovunque.\nDue goblin chiassosi stanno litigando per un osso ancora sanguinante. Si accorgono della tua presenza.',
     'Non sembra esserci nulla di valore qui, a parte i goblin pronti a combattere.',
     1, 3);

INSERT INTO rooms (id, name, description, look_description, west_room_id, south_room_id) VALUES
    (3, 'Dormitorio delle Guardie',
     'Letti rotti e coperte lacerate giacciono sparsi ovunque.\nIl silenzio regna sovrano: nessun nemico in vista, solo l''eco dei tuoi passi.',
     'Una cassa giace accanto a un letto distrutto. Dentro, potresti trovare qualcosa di prezioso.',
     2, 4);

INSERT INTO rooms (id, name, description, look_description, north_room_id, west_room_id) VALUES
    (4, 'Sala delle Guardie',
     'I resti di un banchetto interrotto sono sparsi ovunque.\nUn goblin gigante impugna una clava insanguinata, affiancato da un goblin più minuto ma altrettanto minaccioso.',
     'Tra i resti di una barricata, spunta una cassa semiaperta.',
     3, 5);

INSERT INTO rooms (id, name, description, look_description, east_room_id) VALUES
    (5, 'Sala degli Incantesimi',
     'Le pareti sono incise con simboli magici che pulsano di luce blu.\nNel centro, un altare emette un suono basso e costante. Qui, puoi sacrificare parte della tua vita per ottenere un potente arco.',
     'Un''iscrizione recita: ''Offri parte della tua essenza, e il tuo braccio sarà guidato da un arco etereo.''',
     4);

INSERT INTO rooms (id, name, description, look_description, north_room_id, south_room_id) VALUES
    (6, 'Stanza delle Torture',
     'Catene arrugginite pendono dal soffitto. Il pavimento è macchiato di vecchio sangue secco.\nNon ci sono nemici… ma il luogo mette i brividi.',
     'Una cassa chiusa giace in un angolo, apparentemente dimenticata.',
     1, 7);

INSERT INTO rooms (id, name, description, look_description, north_room_id, west_room_id) VALUES
    (7, 'Sala del Signore dei Goblin',
     'L''aria è irrespirabile. Un fumo denso copre il volto del cane demone, una creatura infernale con zanne fumanti e occhi di fuoco.\nProtegge la cella della principessa con feroce determinazione.',
     'Sul piedistallo alle spalle del demone c''è una chiave che potrebbe aprire la cella.',
     6, 8);

INSERT INTO rooms (id, name, description, east_room_id) VALUES
    (8, 'Uscita', 'L''uscita dalla Montagna Nera. La libertà ti aspetta.', 7);

-- Aggiorna le connessioni inverse
UPDATE rooms SET west_room_id = 1 WHERE id = 2;
UPDATE rooms SET west_room_id = 2 WHERE id = 3;
UPDATE rooms SET north_room_id = 3 WHERE id = 4;
UPDATE rooms SET east_room_id = 4 WHERE id = 5;
UPDATE rooms SET north_room_id = 1 WHERE id = 6;
UPDATE rooms SET north_room_id = 6 WHERE id = 7;
UPDATE rooms SET east_room_id = 7 WHERE id = 8;

-- Inserimento personaggi
INSERT INTO characters (id, name, description, character_type, max_hp, current_hp, attack, defense, room_id) VALUES
    (0, 'Giocatore', 'Il coraggioso avventuriero che si addentra nella Montagna Nera per salvare la principessa.', 'PLAYER', 100, 100, 15, 5, 0),
    (1, 'Goblin', 'Una creatura malvagia dalla pelle verde scuro, con occhi pieni d''odio e artigli affilati.', 'GOBLIN', 40, 40, 12, 3, 0),
    (2, 'Topo Gigante', 'Un enorme roditore con denti giallastri e occhi rossi.', 'GIANT_RAT', 25, 25, 8, 2, 1),
    (3, 'Goblin Chiassoso', 'Un goblin aggressivo che litiga per un osso.', 'GOBLIN', 35, 35, 10, 3, 2),
    (4, 'Goblin Rissoso', 'Un altro goblin altrettanto aggressivo.', 'GOBLIN', 30, 30, 9, 2, 2),
    (5, 'Goblin Gigante', 'Un goblin enorme che impugna una clava insanguinata.', 'GOBLIN', 60, 60, 16, 5, 4),
    (6, 'Goblin Minuto', 'Un goblin più piccolo ma altrettanto minaccioso.', 'GOBLIN', 25, 25, 8, 2, 4),
    (7, 'Cane Demone', 'Una creatura infernale con zanne fumanti e occhi di fuoco.', 'DEMON_DOG', 120, 120, 25, 8, 7);

-- Inserimento oggetti base
INSERT INTO objects (id, name, description, aliases, is_pickupable, object_type) VALUES
    (1, 'chiave ingresso', 'Una chiave d''ottone annerita dal tempo. Potrebbe aprire la porta principale della fortezza.', 'chiave,key', true, 'NORMAL'),
    (2, 'pozione di cura', 'Una fiala dal liquido rosso, emana un lieve calore.', 'pozione,cura,healing', true, 'NORMAL'),
    (3, 'frecce', 'Un piccolo fascio di frecce con punte d''acciaio. Sembrano leggere ma letali.', 'arrow,arrows', true, 'WEAPON'),
    (4, 'stringhe di ragnatela', 'Filamenti spessi e resistenti. Potrebbero servire per creare qualcosa di utile.', 'ragnatela,filo', true, 'NORMAL'),
    (5, 'pozione cura totale', 'Una pozione brillante di colore dorato. Ti riempie di energia solo a guardarla.', 'pozione totale,cura totale', true, 'NORMAL'),
    (6, 'bastone', 'Un robusto bastone di legno. Può essere usato come arma o combinato con altri oggetti.', 'staff,stick', true, 'WEAPON'),
    (7, 'arco magico', 'Un arco leggero ma potente, creato con energia arcana e materiali raccolti nella fortezza.', 'arco,bow', true, 'WEAPON'),
    (8, 'libro incantesimo del fuoco', 'Un grimorio antico, le sue pagine brillano di energia arcana. Contiene l''Incantesimo del Fuoco.', 'libro,grimorio,fuoco', true, 'NORMAL'),
    (9, 'veleno', 'Una boccetta scura. Può essere applicata su armi per aumentare il danno.', 'poison,boccetta', true, 'NORMAL'),
    (10, 'chiave cella principessa', 'Una chiave dorata e decorata, diversa da tutte le altre. Probabilmente apre la cella della principessa.', 'chiave principessa,chiave dorata', true, 'NORMAL'),
    (11, 'chiave del collo del boss', 'Una chiave pesante, con pendaglio di ferro annerito. Cade dal collo del demone canino quando lo sconfiggi: apre l''uscita dalla Montagna dei Goblin.', 'chiave boss,chiave finale', true, 'NORMAL'),
    (12, 'spada', 'Una spada d''acciaio ben bilanciata. Arma affidabile per il combattimento.', 'sword,lama', true, 'WEAPON');

-- Inserimento contenitori (casse)
INSERT INTO objects (id, name, description, aliases, is_openable, is_pickupable, object_type) VALUES
    (100, 'cassa', 'Una vecchia cassa di legno, chiusa ma non bloccata.', 'baule,contenitore,scrigno', true, false, 'CONTAINER'),
    (101, 'cassa', 'Una cassa giace accanto a un letto distrutto.', 'baule,contenitore,scrigno', true, false, 'CONTAINER'),
    (102, 'cassa', 'Una cassa semiaperta tra i resti di una barricata.', 'baule,contenitore,scrigno', true, false, 'CONTAINER'),
    (103, 'cassa', 'Una cassa chiusa giace in un angolo, apparentemente dimenticata.', 'baule,contenitore,scrigno', true, false, 'CONTAINER');

-- Inserimento armi
INSERT INTO weapons (id, object_id, weapon_type, attack_bonus, critical_chance, critical_multiplier) VALUES
    (3, 3, 'ARROWS', 3, 5, 2),
    (6, 6, 'STAFF', 5, 5, 2),
    (7, 7, 'MAGIC', 12, 15, 2),
    (12, 12, 'SWORD', 8, 10, 2);

-- Posizionamento oggetti nelle stanze
-- Ingresso: cassa con chiave e pozione
INSERT INTO room_objects (room_id, object_id) VALUES (0, 100);
INSERT INTO room_objects (room_id, object_id, is_container_content, container_id) VALUES 
    (0, 1, true, 100),
    (0, 2, true, 100);

-- Stanza del Topo: stringhe di ragnatela
INSERT INTO room_objects (room_id, object_id) VALUES (1, 4);

-- Dormitorio: cassa con pozione totale e bastone
INSERT INTO room_objects (room_id, object_id) VALUES (3, 101);
INSERT INTO room_objects (room_id, object_id, is_container_content, container_id) VALUES 
    (3, 5, true, 101),
    (3, 6, true, 101);

-- Sala delle Guardie: cassa con libro e veleno
INSERT INTO room_objects (room_id, object_id) VALUES (4, 102);
INSERT INTO room_objects (room_id, object_id, is_container_content, container_id) VALUES 
    (4, 8, true, 102),
    (4, 9, true, 102);

-- Stanza delle Torture: cassa con chiave principessa
INSERT INTO room_objects (room_id, object_id) VALUES (6, 103);
INSERT INTO room_objects (room_id, object_id, is_container_content, container_id) VALUES 
    (6, 10, true, 103);

COMMIT;