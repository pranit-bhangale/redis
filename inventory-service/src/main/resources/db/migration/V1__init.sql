CREATE TABLE IF NOT EXISTS products
(
    id               UUID          NOT NULL PRIMARY KEY,
    product_name     VARCHAR(150)  NOT NULL,
    category         VARCHAR(100)  NOT NULL,
    price_per_piece  NUMERIC(12,2) NOT NULL,
    total_quantity   INTEGER       NOT NULL,
    remaining        INTEGER       NOT NULL,
    total_price      NUMERIC(14,2) NOT NULL,
    version          BIGINT,

    CONSTRAINT uk_products_product_name_category
        UNIQUE (product_name, category),

    CONSTRAINT chk_products_price
        CHECK (price_per_piece >= 0),

    CONSTRAINT chk_products_total_price
        CHECK (total_price >= 0),

    CONSTRAINT chk_products_quantity
        CHECK (
                  total_quantity >= 0
                  AND remaining >= 0
                AND remaining <= total_quantity
            ),

    CONSTRAINT chk_products_total_value
        CHECK (
                  total_price = price_per_piece * total_quantity
            )
);

CREATE INDEX IF NOT EXISTS idx_products_product_name
    ON products (product_name);

CREATE INDEX IF NOT EXISTS idx_products_category
    ON products (category);