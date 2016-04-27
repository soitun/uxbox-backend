CREATE TABLE IF NOT EXISTS image_collections (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),

  version bigint NOT NULL DEFAULT 0,

  "user" uuid NOT NULL REFERENCES users(id),
  name text NOT NULL
) WITH (OIDS=FALSE);

CREATE TRIGGER image_collections_occ_tgr BEFORE UPDATE ON image_collections
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER image_collections_modified_at_tgr BEFORE UPDATE ON image_collections
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TABLE IF NOT EXISTS images (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),

  "user" uuid NOT NULL REFERENCES users(id),
  collection uuid REFERENCES image_collections(id)
                  ON DELETE SET NULL
                  DEFAULT NULL,

  name text NOT NULL,
  size bigint NOT NULL,
  path text NOT NULL
) WITH (OIDS=FALSE);