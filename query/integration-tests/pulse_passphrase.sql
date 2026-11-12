CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
CREATE SCHEMA IF NOT EXISTS pulse;
CREATE TABLE IF NOT EXISTS pulse.passphrase (
    owned_by_hashed VARCHAR(255) PRIMARY KEY,
    passphrase bytea
);

--------------------------------------------------------------------------
-- forbid_owned_by_hashed_update
--------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION pulse.forbid_owned_by_hashed_update()
RETURNS TRIGGER AS
$_$
BEGIN
    IF NEW.owned_by_hashed IS DISTINCT FROM OLD.owned_by_hashed THEN
        RAISE EXCEPTION
            'Modification of owned_by_hashed is forbidden.';
    END IF;
    RETURN NEW;
END;
$_$ LANGUAGE plpgsql;

CREATE TRIGGER passphrase_forbid_owned_by_hashed_update
BEFORE UPDATE ON pulse.passphrase
FOR EACH ROW
EXECUTE FUNCTION pulse.forbid_owned_by_hashed_update();

--------------------------------------------------------------------------
-- forbid_passphrase_update
--------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION pulse.forbid_passphrase_update()
RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.passphrase IS DISTINCT FROM OLD.passphrase
       AND NEW.passphrase IS NOT NULL THEN
        RAISE EXCEPTION
            'Modification of passphrase is forbidden.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER passphrase_forbid_passphrase_update
BEFORE UPDATE ON pulse.passphrase
FOR EACH ROW
EXECUTE FUNCTION pulse.forbid_passphrase_update();

--------------------------------------------------------------------------
-- forbid_passphrase_delete
--------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION pulse.forbid_passphrase_delete()
RETURNS TRIGGER AS
$_$
BEGIN
    RAISE EXCEPTION 'Deletion of passphrase entries is forbidden.';
END;
$_$ LANGUAGE plpgsql;

CREATE TRIGGER passphrase_forbid_delete
BEFORE DELETE ON pulse.passphrase
FOR EACH ROW
EXECUTE FUNCTION pulse.forbid_passphrase_delete();

--------------------------------------------------------------------------
-- forbid_passphrase_truncate
--------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION pulse.forbid_passphrase_truncate()
RETURNS TRIGGER AS
$_$
BEGIN
    RAISE EXCEPTION 'Truncating the passphrase table is forbidden.';
END;
$_$ LANGUAGE plpgsql;

CREATE TRIGGER passphrase_forbid_truncate
BEFORE TRUNCATE ON pulse.passphrase
FOR EACH STATEMENT
EXECUTE FUNCTION pulse.forbid_passphrase_truncate();
