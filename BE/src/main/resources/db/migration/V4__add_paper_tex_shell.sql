ALTER TABLE documents
    ADD COLUMN preamble_tex LONGTEXT NULL,
    ADD COLUMN front_matter_tex LONGTEXT NULL;
