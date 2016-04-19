CREATE TABLE IF NOT EXISTS persistence_metadata (
  persistence_key BIGINT NOT NULL AUTO_INCREMENT,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_nr BIGINT NOT NULL,
  PRIMARY KEY (persistence_key),
  UNIQUE (persistence_id)
);

CREATE TABLE IF NOT EXISTS persistence_journal (
  persistence_key BIGINT NOT NULL,
  sequence_nr BIGINT NOT NULL,
  message BLOB NOT NULL,
  PRIMARY KEY (persistence_key, sequence_nr),
  FOREIGN KEY (persistence_key) REFERENCES persistence_metadata (persistence_key)
);

CREATE TABLE IF NOT EXISTS persistence_snapshot (
  persistence_key BIGINT NOT NULL,
  sequence_nr BIGINT NOT NULL,
  created_at BIGINT NOT NULL,
  snapshot BLOB NOT NULL,
  PRIMARY KEY (persistence_key, sequence_nr),
  FOREIGN KEY (persistence_key) REFERENCES persistence_metadata (persistence_key)
);

CREATE TABLE IF NOT EXISTS DBSchema (
  id BIGINT NOT NULL,
  revision BIGINT NOT NULL,
  status varchar(50) NOT NULL,
  erstelldat DATETIME NOT NULL,
  ersteller BIGINT NOT NULL, 
  modifidat DATETIME NOT NULL, 
  modifikator BIGINT NOT NULL,
  PRIMARY KEY (id)
);
