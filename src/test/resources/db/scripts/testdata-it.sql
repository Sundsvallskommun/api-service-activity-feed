-- Samma exempelhändelser som exempel-kundhandelser.json (med prefixet TESTKOP) för kommun 2281, samt en händelse för
-- kommun 2262 som aldrig ska synas i frågor mot 2281. Tidpunkter i UTC.
INSERT INTO kundhandelse (id, municipality_id, kundhandelse_id, producent, kund_identifierare, kund_typ, kund_tillagg, arende_identifierare, arende_typ, rubrik, beskrivning, sprak, tidpunkt, kundhandelse_typ, producentarendet_kraver_kundatgard, producentarendet_klart, version, utokad_information, referenser, skapad, andrad)
VALUES
    ('00000000-0000-4000-8000-000000000001', '2281', 'TESTKOP-BYGG-2026-00123-1', 'Testköpings kommun', '199009090000', 'Personnummer', NULL, 'BYGG-2026-00123', 'Diarienummer', 'Ansökan om bygglov mottagen', 'Vi har tagit emot din ansökan om bygglov.', 'sv', '2026-08-20 07:12:00.000', 'TESTKOP.BYGGLOV.ANSOKAN_MOTTAGEN', 0, 0, '6.1', NULL, '{"diarienummer":"BYGG-2026-00123"}', '2026-08-20 07:12:00.000', NULL),
    ('00000000-0000-4000-8000-000000000002', '2281', 'TESTKOP-BYGG-2026-00123-2', 'Testköpings kommun', '199009090000', 'Personnummer', NULL, 'BYGG-2026-00123', 'Diarienummer', 'Ansökan behöver kompletteras', 'Situationsplan saknas.', 'sv', '2026-09-02 12:30:00.000', 'TESTKOP.BYGGLOV.KOMPLETTERING_BEGARD', 1, 0, '6.1', '{"senastDatum":"2026-09-30","saknadeHandlingar":["Situationsplan"]}', NULL, '2026-09-02 12:30:00.000', NULL),
    ('00000000-0000-4000-8000-000000000003', '2281', 'TESTKOP-SERV-2026-00042-1', 'Testköpings kommun', '165560001234', 'Organisationsnummer', NULL, 'SERV-2026-00042', 'Diarienummer', 'Ansökan om serveringstillstånd mottagen', 'Vi har tagit emot ansökan om serveringstillstånd.', 'sv', '2026-09-05 09:00:00.000', 'TESTKOP.SERVERINGSTILLSTAND.ANSOKAN_MOTTAGEN', 0, 0, '6.1', NULL, NULL, '2026-09-05 09:00:00.000', NULL),
    ('00000000-0000-4000-8000-000000000004', '2281', 'TESTKOP-SERV-2026-00042-2', 'Testköpings kommun', '165560001234', 'Organisationsnummer', NULL, 'SERV-2026-00042', 'Diarienummer', 'Beslut om serveringstillstånd', 'Kommunen har beviljat serveringstillstånd.', 'sv', '2026-09-14 06:45:00.000', 'TESTKOP.SERVERINGSTILLSTAND.BESLUT', 0, 1, '6.1', NULL, NULL, '2026-09-14 06:45:00.000', NULL),
    ('00000000-0000-4000-8000-000000000005', '2281', 'TESTKOP-FSK-2026-00777-1', 'Testköpings kommun', '199009090000', 'Personnummer', NULL, NULL, NULL, 'Plats i förskola erbjuden', 'Ditt barn har erbjudits plats i förskola.', 'sv', '2026-09-10 05:30:00.000', 'TESTKOP.FORSKOLA.PLATS_ERBJUDEN', 1, 0, '6.1', NULL, NULL, '2026-09-10 05:30:00.000', NULL),
    ('00000000-0000-4000-8000-000000000006', '2262', 'TIMRA-BYGG-2026-00001-1', 'Timrå kommun', '199009090000', 'Personnummer', NULL, 'BYGG-2026-00001', 'Diarienummer', 'Ansökan om bygglov mottagen', 'Timrå kommun har tagit emot din ansökan.', 'sv', '2026-09-01 08:00:00.000', 'TIMRA.BYGGLOV.ANSOKAN_MOTTAGEN', 0, 0, '6.1', NULL, NULL, '2026-09-01 08:00:00.000', NULL);

INSERT INTO kundhandelse_tagg (kundhandelse_ref, tagg)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'bygglov'),
    ('00000000-0000-4000-8000-000000000002', 'bygglov'),
    ('00000000-0000-4000-8000-000000000002', 'kundatgard'),
    ('00000000-0000-4000-8000-000000000003', 'foretag'),
    ('00000000-0000-4000-8000-000000000003', 'serveringstillstand'),
    ('00000000-0000-4000-8000-000000000004', 'foretag'),
    ('00000000-0000-4000-8000-000000000004', 'serveringstillstand'),
    ('00000000-0000-4000-8000-000000000005', 'forskola'),
    ('00000000-0000-4000-8000-000000000005', 'kundatgard'),
    ('00000000-0000-4000-8000-000000000006', 'bygglov');
