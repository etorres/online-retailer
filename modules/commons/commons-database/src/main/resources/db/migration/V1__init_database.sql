CREATE TYPE sales_tax AS ENUM (
    'luxury goods',
    'standard rate',
    'reduced rate',
    'essential goods'
);

CREATE TABLE taxes (
    id SMALLINT NOT NULL PRIMARY KEY,
    tax sales_tax NOT NULL,
    rate NUMERIC(3,2) NOT NULL DEFAULT 0
        CHECK (rate >= 0.00 AND rate <= 1.00),
    UNIQUE (tax)
);

CREATE TABLE garments (
    id BIGINT NOT NULL PRIMARY KEY,
    category TEXT NOT NULL,
    model TEXT NOT NULL,
    size TEXT NOT NULL,
    color TEXT NOT NULL,
    price_in_eur NUMERIC(9,5) NOT NULL
        CHECK (price_in_eur > 0),
    tax sales_tax NOT NULL,
    description TEXT NOT NULL,
    launch_date DATE NOT NULL,
    images TEXT ARRAY NOT NULL
);

CREATE TABLE electronics (
    id BIGINT NOT NULL PRIMARY KEY,
    category TEXT NOT NULL,
    model TEXT NOT NULL,
    power_consumption_in_watts NUMERIC(6,2) NOT NULL
        CHECK (power_consumption_in_watts >= 0),
    price_in_eur NUMERIC(9,5) NOT NULL
        CHECK (price_in_eur > 0),
    tax sales_tax NOT NULL,
    description TEXT NOT NULL,
    launch_date DATE NOT NULL,
    images TEXT ARRAY NOT NULL
);

CREATE TABLE stock_availability (
    sku BIGINT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    quantity INT NOT NULL
        CHECK (quantity >= 0),
    unit_price_in_eur NUMERIC(9,5) NOT NULL
        CHECK (unit_price_in_eur > 0),
    reorder_level INT NOT NULL
        CHECK (reorder_level > 0)
);