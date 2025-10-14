DO $$
BEGIN

  CREATE TABLE IF NOT EXISTS T_EVENT (
    aggregaterootid character varying(255) not null,
    aggregateroottype character varying(255) not null,
    version bigint not null,
    creationdate timestamp without time zone not null,
    eventtype character varying(255) not null,
    eventpayload jsonb not null,
    CONSTRAINT event_pkey PRIMARY KEY (aggregaterootid, aggregateroottype, version),
    CONSTRAINT event_unique UNIQUE (aggregaterootid, aggregateroottype, version)
  );
  CREATE INDEX IF NOT EXISTS idx_t_event_aggregateRootIdentifier ON T_EVENT USING BTREE (aggregaterootid, aggregateroottype);
  CREATE INDEX IF NOT EXISTS idx_t_event_payload_gin ON T_EVENT USING gin (eventpayload);
  IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'event_check_version_on_create') THEN
    RAISE NOTICE 'Routine event_check_version_on_create EXISTS';
  ELSE
    CREATE FUNCTION event_check_version_on_create() RETURNS trigger LANGUAGE PLPGSQL AS $event_check_version_on_create$
    DECLARE
      event_count INTEGER;
      last_version INTEGER;
      expected_version INTEGER;
    BEGIN
      SELECT COUNT(*) INTO event_count FROM T_EVENT WHERE aggregaterootid = NEW.aggregaterootid AND aggregateroottype = NEW.aggregateroottype;
      IF NEW.version = 0 AND event_count > 0 THEN
        RAISE EXCEPTION 'Event already present while should not be ! aggregaterootid % aggregateroottype %', NEW.aggregaterootid, NEW.aggregateroottype;
      END IF;
      SELECT version INTO last_version FROM T_EVENT WHERE aggregaterootid = NEW.aggregaterootid AND aggregateroottype = NEW.aggregateroottype ORDER BY version DESC LIMIT 1;
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
    BEFORE INSERT ON T_EVENT FOR EACH ROW
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
    BEFORE UPDATE ON T_EVENT FOR EACH ROW
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
    BEFORE DELETE ON T_EVENT FOR EACH ROW
    EXECUTE FUNCTION event_not_deletable();
    EXCEPTION
      WHEN duplicate_object THEN
        RAISE NOTICE 'Trigger event_not_deletable_trigger EXISTS';
  END;

  BEGIN
    CREATE TRIGGER event_table_not_truncable_trigger
    BEFORE TRUNCATE ON T_EVENT
    EXECUTE FUNCTION event_not_deletable();
    EXCEPTION
      WHEN duplicate_object THEN
        RAISE NOTICE 'Trigger event_table_not_truncable_trigger EXISTS';
  END;
END;
$$;
