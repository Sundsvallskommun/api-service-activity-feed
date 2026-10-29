-- Ärendecache: kundhändelser enligt Mina ärendens standard, per kommun och med den part
-- (kund och/eller ärende) som händelsen gäller. Kolumnnamnen följer API-specifikationen.
--
-- Teckenuppsättning och sortering anges uttryckligen, så att de inte beror på serverns standard (som skiljer mellan
-- MariaDB-versioner). Text sorteras enligt svenska regler. Identifierare jämförs exakt (utf8mb4_bin), så att t.ex.
-- REFKOM-x och refkom-X är olika kundhändelser och taggarna bygglov och Bygglov kan finnas på samma händelse.

CREATE TABLE kundhandelse (
    id                                  VARCHAR(255)  COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    municipality_id                     VARCHAR(4)    NOT NULL,
    kundhandelse_id                     VARCHAR(100)  COLLATE utf8mb4_bin NOT NULL,
    producent                           VARCHAR(200)  NOT NULL,
    kund_identifierare                  VARCHAR(12),
    kund_typ                            VARCHAR(20),
    kund_tillagg                        VARCHAR(20),
    arende_identifierare                VARCHAR(100)  COLLATE utf8mb4_bin,
    arende_typ                          VARCHAR(30),
    rubrik                              VARCHAR(255)  NOT NULL,
    -- LONGTEXT eftersom API:t begränsar i tecken, inte byte (å, ä och ö tar två byte, emoji fyra).
    beskrivning                         LONGTEXT      NOT NULL,
    sprak                               VARCHAR(10)   NOT NULL,
    tidpunkt                            DATETIME(3)   NOT NULL,
    kundhandelse_typ                    VARCHAR(200)  COLLATE utf8mb4_bin NOT NULL,
    producentarendet_kraver_kundatgard  BOOLEAN       NOT NULL,
    producentarendet_klart              BOOLEAN       NOT NULL,
    version                             VARCHAR(10)   NOT NULL,
    utokad_information                  LONGTEXT,
    referenser                          LONGTEXT,
    skapad                              DATETIME(3)   NOT NULL,
    -- Tidpunkt för senaste ersättning via POST /{municipalityId}/kundhandelser.
    andrad                              DATETIME(3),
    -- kundhandelseId är unikt per kommun.
    CONSTRAINT uq_kundhandelse_municipality_id_kundhandelse_id UNIQUE (municipality_id, kundhandelse_id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_swedish_ci;

-- Frågor filtrerar alltid på kommun först.
CREATE INDEX ix_kundhandelse_kund     ON kundhandelse (municipality_id, kund_identifierare, kund_typ);
CREATE INDEX ix_kundhandelse_arende   ON kundhandelse (municipality_id, arende_identifierare, arende_typ);
CREATE INDEX ix_kundhandelse_tidpunkt ON kundhandelse (tidpunkt);
CREATE INDEX ix_kundhandelse_typ      ON kundhandelse (kundhandelse_typ);

-- Taggar som pekar ut producentärendetyper eller kundhändelsetyper (fraga.taggar).
CREATE TABLE kundhandelse_tagg (
    kundhandelse_ref  VARCHAR(255) COLLATE utf8mb4_bin NOT NULL,
    tagg              VARCHAR(100) COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (kundhandelse_ref, tagg),
    CONSTRAINT fk_tagg_kundhandelse FOREIGN KEY (kundhandelse_ref) REFERENCES kundhandelse (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_swedish_ci;

CREATE INDEX ix_kundhandelse_tagg ON kundhandelse_tagg (tagg);
