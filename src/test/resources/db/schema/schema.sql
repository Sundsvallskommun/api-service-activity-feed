
    create table activity (
        customer_action_required bit not null,
        errand_completed bit not null,
        municipality_id varchar(4) not null,
        created datetime(6) not null,
        modified datetime(6),
        occurred_at datetime(6) not null,
        language varchar(10) not null,
        version varchar(10) not null,
        customer_identifier varchar(12),
        customer_suffix varchar(20),
        customer_type varchar(20),
        errand_type varchar(30),
        activity_id varchar(100) not null,
        errand_identifier varchar(100),
        activity_type varchar(200) not null,
        producer varchar(200) not null,
        id varchar(255) not null,
        title varchar(255) not null,
        description longtext not null,
        extended_information longtext,
        reference_data longtext,
        primary key (id)
    ) engine=InnoDB;

    create table activity_tag (
        tag varchar(100) not null,
        activity_ref varchar(255) not null,
        primary key (tag, activity_ref)
    ) engine=InnoDB;

    create index ix_activity_customer 
       on activity (municipality_id, customer_identifier, customer_type);

    create index ix_activity_errand 
       on activity (municipality_id, errand_identifier, errand_type);

    create index ix_activity_occurred_at 
       on activity (occurred_at);

    create index ix_activity_type 
       on activity (activity_type);

    alter table if exists activity 
       add constraint uq_activity_municipality_id_activity_id unique (municipality_id, activity_id);

    create index ix_activity_tag 
       on activity_tag (tag);

    alter table if exists activity_tag 
       add constraint fk_activity_tag_activity 
       foreign key (activity_ref) 
       references activity (id);
