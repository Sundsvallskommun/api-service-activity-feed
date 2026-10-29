
    create table kundhandelse (
        municipality_id varchar(4) not null,
        producentarendet_klart bit not null,
        producentarendet_kraver_kundatgard bit not null,
        andrad datetime(6),
        skapad datetime(6) not null,
        tidpunkt datetime(6) not null,
        sprak varchar(10) not null,
        version varchar(10) not null,
        kund_identifierare varchar(12),
        kund_tillagg varchar(20),
        kund_typ varchar(20),
        arende_typ varchar(30),
        arende_identifierare varchar(100),
        kundhandelse_id varchar(100) not null,
        kundhandelse_typ varchar(200) not null,
        producent varchar(200) not null,
        id varchar(255) not null,
        rubrik varchar(255) not null,
        beskrivning longtext not null,
        referenser longtext,
        utokad_information longtext,
        primary key (id)
    ) engine=InnoDB;

    create table kundhandelse_tagg (
        tagg varchar(100) not null,
        kundhandelse_ref varchar(255) not null,
        primary key (tagg, kundhandelse_ref)
    ) engine=InnoDB;

    create index ix_kundhandelse_kund 
       on kundhandelse (municipality_id, kund_identifierare, kund_typ);

    create index ix_kundhandelse_arende 
       on kundhandelse (municipality_id, arende_identifierare, arende_typ);

    create index ix_kundhandelse_tidpunkt 
       on kundhandelse (tidpunkt);

    create index ix_kundhandelse_typ 
       on kundhandelse (kundhandelse_typ);

    alter table if exists kundhandelse 
       add constraint uq_kundhandelse_municipality_id_kundhandelse_id unique (municipality_id, kundhandelse_id);

    create index ix_kundhandelse_tagg 
       on kundhandelse_tagg (tagg);

    alter table if exists kundhandelse_tagg 
       add constraint fk_tagg_kundhandelse 
       foreign key (kundhandelse_ref) 
       references kundhandelse (id);
