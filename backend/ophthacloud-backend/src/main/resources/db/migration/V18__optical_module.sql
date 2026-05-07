DROP TABLE IF EXISTS invoice_lines CASCADE;
DROP TABLE IF EXISTS invoices CASCADE;
DROP TABLE IF EXISTS optical_order_items CASCADE;
DROP TABLE IF EXISTS optical_orders CASCADE;
DROP TABLE IF EXISTS stock_items CASCADE;
DROP TABLE IF EXISTS service_catalog CASCADE;

CREATE TABLE service_catalog (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    name            VARCHAR(256) NOT NULL,
    category        VARCHAR(64) NOT NULL,
    sku             VARCHAR(64),
    brand           VARCHAR(128),
    unit_price      NUMERIC(12,4) NOT NULL DEFAULT 0,
    vat_rate        NUMERIC(5,2) NOT NULL DEFAULT 19.00,
    currency        VARCHAR(3) NOT NULL DEFAULT 'RON',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    notes           TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_items (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    service_item_id UUID        REFERENCES service_catalog(id),
    name            VARCHAR(256) NOT NULL,
    category        VARCHAR(64) NOT NULL,
    sku             VARCHAR(64),
    barcode         VARCHAR(128),
    brand           VARCHAR(128),
    current_stock   INT         NOT NULL DEFAULT 0,
    minimum_stock   INT         NOT NULL DEFAULT 5,
    unit_cost       NUMERIC(12,4),
    unit_price      NUMERIC(12,4),
    currency        VARCHAR(3)     NOT NULL DEFAULT 'RON',
    location        VARCHAR(128),
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_restocked_at TIMESTAMPTZ,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_stock_non_negative CHECK (current_stock >= 0)
);

CREATE TABLE optical_orders (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL REFERENCES tenants(id),
    order_number        VARCHAR(32)     NOT NULL,
    patient_id          UUID            NOT NULL REFERENCES patients(id),
    prescription_id     UUID            REFERENCES prescriptions(id),
    consultation_id     UUID            REFERENCES consultations(id),
    order_type          VARCHAR(32)     NOT NULL DEFAULT 'GLASSES',
    stage               VARCHAR(32)     NOT NULL DEFAULT 'RECEIVED',
    assigned_to_id      UUID,
    assigned_to_name    VARCHAR(256),
    lab_name            VARCHAR(256),
    lab_reference       VARCHAR(128),
    sent_to_lab_at      TIMESTAMPTZ,
    qc_passed_at        TIMESTAMPTZ,
    ready_at            TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason TEXT,
    total_amount        NUMERIC(12,4)   NOT NULL DEFAULT 0,
    deposit_paid        NUMERIC(12,4)   NOT NULL DEFAULT 0,
    currency            VARCHAR(3)         NOT NULL DEFAULT 'RON',
    notes               TEXT,
    internal_notes      TEXT,
    qc_result           JSONB,
    created_by_id       UUID,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_optical_orders_number UNIQUE (tenant_id, order_number)
);

CREATE TABLE optical_order_items (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    order_id            UUID        NOT NULL REFERENCES optical_orders(id) ON DELETE CASCADE,
    service_item_id     UUID        REFERENCES service_catalog(id),
    stock_item_id       UUID        REFERENCES stock_items(id),
    description         VARCHAR(512) NOT NULL,
    quantity            INT         NOT NULL DEFAULT 1,
    unit_price          NUMERIC(12,4) NOT NULL,
    discount_percent    NUMERIC(5,2) DEFAULT 0,
    line_total          NUMERIC(12,4) NOT NULL,
    eye                 VARCHAR(2),
    lens_specifications JSONB,
    notes               TEXT,
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE invoices (
    id                  UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID                NOT NULL REFERENCES tenants(id),
    invoice_number      VARCHAR(32)         NOT NULL,
    patient_id          UUID                NOT NULL REFERENCES patients(id),
    optical_order_id    UUID                REFERENCES optical_orders(id),
    consultation_id     UUID                REFERENCES consultations(id),
    status              VARCHAR(32)         NOT NULL DEFAULT 'DRAFT',
    issued_at           TIMESTAMPTZ,
    due_at              TIMESTAMPTZ,
    paid_at             TIMESTAMPTZ,
    subtotal            NUMERIC(12,4)       NOT NULL DEFAULT 0,
    vat_total           NUMERIC(12,4)       NOT NULL DEFAULT 0,
    discount_total      NUMERIC(12,4)       NOT NULL DEFAULT 0,
    total               NUMERIC(12,4)       NOT NULL DEFAULT 0,
    amount_paid         NUMERIC(12,4)       NOT NULL DEFAULT 0,
    currency            VARCHAR(3)             NOT NULL DEFAULT 'RON',
    payment_method      VARCHAR(32),
    payment_reference   VARCHAR(256),
    notes               TEXT,
    pdf_path            VARCHAR(1024),
    created_by_id       UUID,
    version             BIGINT              NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_invoices_number UNIQUE (tenant_id, invoice_number)
);

CREATE TABLE invoice_lines (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    invoice_id          UUID        NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description         VARCHAR(512) NOT NULL,
    quantity            INT         NOT NULL DEFAULT 1,
    unit_price          NUMERIC(12,4) NOT NULL,
    vat_rate            NUMERIC(5,2) NOT NULL DEFAULT 19.00,
    discount_percent    NUMERIC(5,2) DEFAULT 0,
    line_total          NUMERIC(12,4) NOT NULL,
    service_item_id     UUID        REFERENCES service_catalog(id),
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
