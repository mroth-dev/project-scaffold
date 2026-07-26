-- Create audit_events table
CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    event_type VARCHAR(100) NOT NULL,
    user_id BIGINT,
    details TEXT,
    timestamp TIMESTAMP NOT NULL,
    ip_address INET,
    user_agent TEXT,
    
    -- Add foreign key constraint to users table
    CONSTRAINT fk_audit_events_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for better query performance
CREATE INDEX idx_audit_events_entity_type ON audit_events(entity_type);
CREATE INDEX idx_audit_events_entity_id ON audit_events(entity_id);
CREATE INDEX idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_events_user_id ON audit_events(user_id);
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp DESC);
CREATE INDEX idx_audit_events_entity_type_id ON audit_events(entity_type, entity_id);

-- Create composite index for common filter combinations
CREATE INDEX idx_audit_events_filters ON audit_events(entity_type, event_type, user_id, timestamp DESC);