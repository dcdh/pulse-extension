DO $$
BEGIN

  CREATE TABLE IF NOT EXISTS t_aggregate_root (
    aggregate_root_type character varying(255) not null,
    aggregate_root_id character varying(255) not null,
    last_version bigint not null,
    aggregate_root_payload bytea NOT NULL CHECK (octet_length(aggregate_root_payload) <= 1000 * 1024),
    owned_by character varying(255) not null,
    in_relation_with character varying(255) not null,
    CONSTRAINT t_aggregate_root_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type)
  );

  CREATE TABLE IF NOT EXISTS t_event (
    aggregate_root_type character varying(255) not null,
    aggregate_root_id character varying(255) not null,
    version bigint not null,
    creation_date timestamp(6) without time zone not null,
    event_type character varying(255) not null,
    event_payload bytea not null CHECK (octet_length(event_payload) <= 1000 * 1024),
    owned_by character varying(255) not null,
    CONSTRAINT event_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type, version),
    CONSTRAINT event_unique UNIQUE (aggregate_root_id, aggregate_root_type, version)
  );
  CREATE INDEX IF NOT EXISTS idx_t_event_aggregate_root_identifier ON t_event USING BTREE (aggregate_root_id, aggregate_root_type);
  IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'event_check_version_on_create') THEN
    RAISE NOTICE 'Routine event_check_version_on_create EXISTS';
  ELSE
    CREATE FUNCTION event_check_version_on_create() RETURNS trigger LANGUAGE PLPGSQL AS $event_check_version_on_create$
    DECLARE
      event_count INTEGER;
      last_version INTEGER;
      expected_version INTEGER;
    BEGIN
      SELECT COUNT(*) INTO event_count FROM t_event WHERE aggregate_root_id = NEW.aggregate_root_id AND aggregate_root_type = NEW.aggregate_root_type;
      IF NEW.version = 0 AND event_count > 0 THEN
        RAISE EXCEPTION 'Event already present while should not be ! aggregate_root_id % aggregate_root_type %', NEW.aggregate_root_id, NEW.aggregate_root_type;
      END IF;
      SELECT version INTO last_version FROM t_event WHERE aggregate_root_id = NEW.aggregate_root_id AND aggregate_root_type = NEW.aggregate_root_type ORDER BY version DESC LIMIT 1;
      expected_version = last_version + 1;
      IF NEW.version != expected_version THEN
        RAISE EXCEPTION 'current version unexpected % - expected version %', NEW.version, expected_version;
      END IF;
      RETURN NEW;
    END;
    $event_check_version_on_create$;
  END IF;

  BEGIN
    CREATE TRIGGER event_check_version_on_create_trigger
    BEFORE INSERT ON t_event FOR EACH ROW
    EXECUTE FUNCTION event_check_version_on_create();
    EXCEPTION
      WHEN duplicate_object THEN
        RAISE NOTICE 'Trigger event_check_version_on_create_trigger EXISTS';
  END;

  IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'event_immutable') THEN
    RAISE NOTICE 'Routine event_immutable EXISTS';
  ELSE
    CREATE FUNCTION event_immutable() RETURNS trigger LANGUAGE PLPGSQL AS $event_immutable$
    BEGIN
      RAISE EXCEPTION 'not allowed';
    END;
    $event_immutable$;
  END IF;

  BEGIN
    CREATE TRIGGER event_immutable_trigger
    BEFORE UPDATE ON t_event FOR EACH ROW
    EXECUTE FUNCTION event_immutable();
    EXCEPTION
      WHEN duplicate_object THEN
        RAISE NOTICE 'Trigger event_check_version_on_create_trigger EXISTS';
  END;

  IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'event_not_deletable') THEN
    RAISE NOTICE 'Routine event_not_deletable EXISTS';
  ELSE
    CREATE FUNCTION event_not_deletable() RETURNS trigger LANGUAGE PLPGSQL AS $event_not_deletable$
    BEGIN
      RAISE EXCEPTION 'not allowed';
    END;
    $event_not_deletable$;
  END IF;

  BEGIN
    CREATE TRIGGER event_not_deletable_trigger
    BEFORE DELETE ON t_event FOR EACH ROW
    EXECUTE FUNCTION event_not_deletable();
    EXCEPTION
      WHEN duplicate_object THEN
        RAISE NOTICE 'Trigger event_not_deletable_trigger EXISTS';
  END;

  BEGIN
    CREATE TRIGGER event_table_not_truncable_trigger
    BEFORE TRUNCATE ON t_event
    EXECUTE FUNCTION event_not_deletable();
    EXCEPTION
      WHEN duplicate_object THEN
        RAISE NOTICE 'Trigger event_table_not_truncable_trigger EXISTS';
  END;
END;
$$;
