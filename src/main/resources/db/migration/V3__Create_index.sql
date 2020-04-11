update NEWS set created_at = date(created_at), modified_at = date(modified_at);

CREATE INDEX create_modify_at_index ON NEWS(created_at, modified_at);
