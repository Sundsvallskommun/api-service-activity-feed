INSERT INTO activity (id, municipality_id, activity_id, producer, customer_identifier, customer_type, customer_suffix, errand_identifier, errand_type, title, description, language, occurred_at, activity_type, customer_action_required, errand_completed, version, extended_information, reference_data, created, modified)
VALUES
    ('00000000-0000-4000-8000-000000000001', '2281', 'TESTKOP-BYGG-1', 'Testköpings kommun', '199009090000', 'Personnummer', NULL, 'BYGG-1', 'Diarienummer', 'Ansökan mottagen', 'Beskrivning 1', 'sv', '2026-08-20 07:12:00.000', 'TESTKOP.BYGGLOV.ANSOKAN_MOTTAGEN', 0, 0, '6.1', NULL, '{"diarienummer":"BYGG-1"}', '2026-08-20 07:12:00.000', NULL),
    ('00000000-0000-4000-8000-000000000002', '2281', 'TESTKOP-BYGG-2', 'Testköpings kommun', '199009090000', 'Personnummer', NULL, 'BYGG-1', 'Diarienummer', 'beslut fattat', 'Beskrivning 2', 'sv', '2026-09-02 12:30:00.000', 'TESTKOP.BYGGLOV.BESLUT', 1, 1, '6.1', '{"senastDatum":"2026-09-30"}', NULL, '2026-09-02 12:30:00.000', NULL),
    ('00000000-0000-4000-8000-000000000003', '2281', 'TESTKOP-FSK-1', 'Testköpings kommun', '199009090000', 'Personnummer', NULL, NULL, NULL, 'Plats erbjuden', 'Beskrivning 3', 'sv', '2026-09-10 05:30:00.000', 'TESTKOP.FORSKOLA.PLATS_ERBJUDEN', 1, 0, '6.1', NULL, NULL, '2026-09-10 05:30:00.000', NULL),
    ('00000000-0000-4000-8000-000000000004', '2281', 'TESTKOP-SERV-1', 'Testköpings kommun', '165560001234', 'Organisationsnummer', NULL, 'SERV-1', 'Diarienummer', 'Serveringstillstånd', 'Beskrivning 4', 'sv', '2026-09-05 09:00:00.000', 'TESTKOP.SERVERINGSTILLSTAND.ANSOKAN_MOTTAGEN', 0, 0, '6.1', NULL, NULL, '2026-09-05 09:00:00.000', NULL),
    ('00000000-0000-4000-8000-000000000005', '2262', 'TIMRA-BYGG-1', 'Timrå kommun', '199009090000', 'Personnummer', NULL, 'BYGG-1', 'Diarienummer', 'Ansökan mottagen i Timrå', 'Beskrivning 5', 'sv', '2026-09-01 08:00:00.000', 'TIMRA.BYGGLOV.ANSOKAN_MOTTAGEN', 0, 0, '6.1', NULL, NULL, '2026-09-01 08:00:00.000', NULL);

INSERT INTO activity_tag (activity_ref, tag)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'bygglov'),
    ('00000000-0000-4000-8000-000000000002', 'bygglov'),
    ('00000000-0000-4000-8000-000000000002', 'kundatgard'),
    ('00000000-0000-4000-8000-000000000003', 'forskola'),
    ('00000000-0000-4000-8000-000000000003', 'kundatgard'),
    ('00000000-0000-4000-8000-000000000005', 'bygglov');
