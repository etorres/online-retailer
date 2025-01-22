CREATE TABLE garments (
    id BIGINT NOT NULL PRIMARY KEY,
    category TEXT NOT NULL,
    model TEXT NOT NULL,
    size TEXT NOT NULL,
    color TEXT NOT NULL,
    price_in_eur NUMERIC(9,5) NOT NULL,
    description TEXT NOT NULL,
    images TEXT ARRAY NOT NULL
);