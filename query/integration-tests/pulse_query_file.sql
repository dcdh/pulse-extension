CREATE TABLE IF NOT EXISTS pulse.file (
    file_identifier VARCHAR(255) PRIMARY KEY,
    filename        VARCHAR(1024) NOT NULL,
    content_type    VARCHAR(255) NOT NULL,
    content_length  BIGINT NOT NULL,
    uploaded_at     TIMESTAMPTZ NOT NULL,
    uploaded_by     VARCHAR(1024) NOT NULL,
    owned_by        VARCHAR(255) NOT NULL,
    metadata        BYTEA NOT NULL,
    content         BYTEA NOT NULL,
    custom_metadata BYTEA NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_file_owned_by ON pulse.file (owned_by);

CREATE OR REPLACE FUNCTION pulse.prevent_file_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $_$
BEGIN
    RAISE EXCEPTION 'Deletion of token_download is forbidden';
END;
$_$;

CREATE TRIGGER trg_prevent_file_delete
BEFORE DELETE ON pulse.file
FOR EACH ROW
EXECUTE FUNCTION pulse.prevent_file_delete();

CREATE TABLE IF NOT EXISTS pulse.token_download (
    token UUID PRIMARY KEY,
    file_identifier VARCHAR(255) NOT NULL,
    downloaded_by VARCHAR(1024) NOT NULL,
    downloaded_at TIMESTAMPTZ NOT NULL,
    owned_by VARCHAR(255) NOT NULL
);

CREATE OR REPLACE FUNCTION pulse.prevent_token_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $_$
BEGIN
    RAISE EXCEPTION 'Deletion of token_download is forbidden';
END;
$_$;

CREATE TRIGGER token_download_no_delete
BEFORE DELETE ON pulse.token_download
FOR EACH ROW
EXECUTE FUNCTION pulse.prevent_token_delete();

CREATE OR REPLACE FUNCTION pulse.prevent_token_update()
RETURNS trigger
LANGUAGE plpgsql
AS $_$
BEGIN
    RAISE EXCEPTION 'Modification of token_download is forbidden';
END;
$_$;

CREATE TRIGGER token_download_no_update
BEFORE UPDATE ON pulse.token_download
FOR EACH ROW
EXECUTE FUNCTION pulse.prevent_token_update();
