CREATE TABLE IF NOT EXISTS todo_serialization.event_counter (
    counter_type character varying(50) NOT NULL,
    counter_id character varying(255) NOT NULL,
    event_count bigint NOT NULL DEFAULT 0,
    CONSTRAINT event_counter_pkey PRIMARY KEY (counter_type, counter_id)
);

CREATE OR REPLACE FUNCTION todo_serialization.update_event_counter()
RETURNS trigger
LANGUAGE plpgsql
AS $_$
BEGIN

    -- AggregateId
    INSERT INTO todo_serialization.event_counter(counter_type, counter_id, event_count)
    VALUES ('AGGREGATE_ID', NEW.aggregate_root_id, 1)
    ON CONFLICT (counter_type, counter_id)
    DO UPDATE
       SET event_count = event_counter.event_count + 1;

    -- OwnedBy
    INSERT INTO todo_serialization.event_counter(counter_type, counter_id, event_count)
    VALUES ('OWNED_BY', NEW.owned_by, 1)
    ON CONFLICT (counter_type, counter_id)
    DO UPDATE
       SET event_count = event_counter.event_count + 1;

    RETURN NEW;
END;
$_$;

DROP TRIGGER IF EXISTS trg_update_event_counter ON todo_serialization.event;

CREATE TRIGGER trg_update_event_counter
AFTER INSERT ON todo_serialization.event
FOR EACH ROW
EXECUTE FUNCTION todo_serialization.update_event_counter();
