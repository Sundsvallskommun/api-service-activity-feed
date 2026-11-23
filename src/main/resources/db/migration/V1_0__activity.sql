-- Activity cache: activities (kundhändelser) per the Mina ärenden standard, per municipality and with the party
-- (customer and/or errand) the activity concerns.
--
-- Charset and collation are set explicitly, so they do not depend on the server default (which differs between
-- MariaDB versions). Text sorts by Swedish rules. Identifiers compare exactly (utf8mb4_bin), so that e.g.
-- REFKOM-x and refkom-X are different activities and the tags bygglov and Bygglov can both be on the same activity.

CREATE TABLE activity (
    id                        VARCHAR(36)  COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    municipality_id           VARCHAR(4)    NOT NULL,
    activity_id               VARCHAR(100)  COLLATE utf8mb4_bin NOT NULL,
    producer                  VARCHAR(200)  NOT NULL,
    customer_identifier       VARCHAR(12),
    customer_type             VARCHAR(20),
    customer_suffix           VARCHAR(20),
    errand_identifier         VARCHAR(100)  COLLATE utf8mb4_bin,
    errand_type               VARCHAR(30),
    title                     VARCHAR(255)  NOT NULL,
    -- LONGTEXT because the API limits characters, not bytes (å, ä and ö take two bytes, emoji four).
    description               LONGTEXT      NOT NULL,
    language                  VARCHAR(10)   NOT NULL,
    occurred_at               DATETIME(3)   NOT NULL,
    activity_type             VARCHAR(200)  COLLATE utf8mb4_bin NOT NULL,
    customer_action_required  BOOLEAN       NOT NULL,
    errand_completed          BOOLEAN       NOT NULL,
    version                   VARCHAR(10)   NOT NULL,
    extended_information      LONGTEXT,
    reference_data            LONGTEXT,
    created                   DATETIME(3)   NOT NULL,
    -- Time of the latest replacement through POST /{municipalityId}/kundhandelser.
    modified                  DATETIME(3),
    -- activity_id (kundhandelseId in the API) is unique per municipality.
    CONSTRAINT uq_activity_municipality_id_activity_id UNIQUE (municipality_id, activity_id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_swedish_ci;

-- Queries always filter on municipality first.
CREATE INDEX ix_activity_customer    ON activity (municipality_id, customer_identifier, customer_type);
CREATE INDEX ix_activity_errand      ON activity (municipality_id, errand_identifier, errand_type);
CREATE INDEX ix_activity_occurred_at ON activity (occurred_at);
CREATE INDEX ix_activity_type        ON activity (activity_type);

-- Tags that point out producer errand types or activity types (fraga.taggar).
CREATE TABLE activity_tag (
    activity_ref  VARCHAR(36) COLLATE utf8mb4_bin NOT NULL,
    tag           VARCHAR(100) COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (activity_ref, tag),
    CONSTRAINT fk_activity_tag_activity FOREIGN KEY (activity_ref) REFERENCES activity (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_swedish_ci;

CREATE INDEX ix_activity_tag ON activity_tag (tag);
