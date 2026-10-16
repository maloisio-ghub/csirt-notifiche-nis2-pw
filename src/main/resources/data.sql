INSERT INTO servizio_critico (codice, nome, descrizione, base_utenti) VALUES
  ('S1', 'Managed Network', 'Gestione di reti, firewall e SD-WAN dei clienti', 12000),
  ('S2', 'SOC/MDR', 'Monitoraggio della sicurezza e risposta gestita agli incidenti', 8000),
  ('S3', 'Backup-as-a-Service', 'Backup e ripristino dei dati dei clienti', 5000);

INSERT INTO parametri_significativita
  (versione, valido_dal, fatturato_annuo, perdita_massima, quota_fatturato,
   minuti_indisponibilita_totale, minuti_disponibilita_limitata, quota_utenti, utenti_massimi, nota)
VALUES
  (1, DATE '2026-01-01', 24000000, 500000, 0.05, 30, 60, 0.05, 1000000,
   'Reg. (UE) 2024/2690 artt. 3 e 10; fatturato dell''ultimo esercizio chiuso');
   