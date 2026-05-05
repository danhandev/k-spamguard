ALTER TABLE moderation_queue_items
    ADD COLUMN reviewed_at  DATETIME(6)  NULL AFTER created_at,
    ADD COLUMN reviewer_note VARCHAR(500) NULL AFTER reviewed_at;
