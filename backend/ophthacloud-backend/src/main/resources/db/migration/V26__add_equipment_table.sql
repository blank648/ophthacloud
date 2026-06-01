-- V26__add_equipment_table.sql
CREATE TABLE equipment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    brand VARCHAR(64),
    type VARCHAR(64) NOT NULL,
    location VARCHAR(128),
    dicom_enabled BOOLEAN NOT NULL DEFAULT false,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_equipment_tenant_id ON equipment(tenant_id);
