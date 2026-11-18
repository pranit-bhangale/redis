INSERT INTO products (id, product_name, category, price_per_piece, total_quantity, remaining, total_price, version)
WITH
    params AS (
        SELECT 1.00::numeric AS fx
    ),
    cat(category, brands, items, prices, variants, vmults) AS (
        VALUES
            ('Smartphones',
             ARRAY['Samsung','Google','OnePlus','Motorola','Xiaomi','Vivo'],
             ARRAY['5G Smartphone 128GB','5G Smartphone 256GB','5G Smartphone 512GB',
             'Flagship Smartphone 256GB','Flagship Smartphone 512GB','Flagship Smartphone 1TB',
             'Mid-Range Smartphone 128GB','Mid-Range Smartphone 256GB',
             'Budget Smartphone 64GB','Budget Smartphone 128GB'],
             ARRAY[399,479,599,899,1099,1299,279,329,129,159]::numeric[],
             ARRAY['Black','White','Blue','Green','Silver','Graphite','Lite','Mini','Plus','Pro','Max','Ultra','Dual SIM','Special Edition'],
             ARRAY[1,1,1,1,1,1,0.85,0.80,1.12,1.25,1.35,1.45,1.00,1.15]::numeric[]),

            ('Laptops',
             ARRAY['Dell','Lenovo','HP','Asus','Acer','MSI'],
             ARRAY['Ultrabook 13-inch 8GB/256GB','Laptop 14-inch 16GB/512GB','Laptop 15.6-inch 8GB/512GB',
             'Gaming Laptop RTX 4060','Gaming Laptop RTX 4070','Business Laptop 14-inch i7',
             '2-in-1 Convertible Laptop','Workstation Laptop 16-inch','Student Laptop 14-inch 8GB/256GB',
             'Creator Laptop 16-inch OLED'],
             ARRAY[749,949,649,1199,1599,1099,899,1999,479,1399]::numeric[],
             ARRAY['Silver','Black','Graphite','Blue','Eco','Lite','Plus','Pro','Max','Premium','Elite','Touchscreen','Special Edition','Business Edition'],
             ARRAY[1,1,1,1,0.90,0.85,1.10,1.25,1.40,1.50,1.60,1.15,1.20,1.30]::numeric[]),

            ('Audio',
             ARRAY['Sony','Bose','Sennheiser','JBL','Anker Soundcore','boAt'],
             ARRAY['Wireless Over-Ear Headphones','Noise Cancelling Headphones','True Wireless Earbuds','Sports Earbuds',
             'Portable Bluetooth Speaker','Party Speaker','Soundbar 2.1','Wired Headphones',
             'Neckband Earphones','Smart Speaker'],
             ARRAY[249,349,129,79,99,299,249,69,39,89]::numeric[],
             ARRAY['Black','White','Blue','Red','Grey','Green','Lite','Mini','Plus','Pro','Max','Elite','Limited Edition','Gen 2'],
             ARRAY[1,1,1,1,1,1,0.85,0.80,1.12,1.30,1.45,1.50,1.20,1.10]::numeric[]),

            ('Home Appliances',
             ARRAY['LG','Samsung','Bosch','Whirlpool','Panasonic','Haier'],
             ARRAY['Front Load Washing Machine 7kg','Top Load Washing Machine 8kg','Double Door Refrigerator 260L',
             'Single Door Refrigerator 190L','Split Air Conditioner 1.5 Ton','Microwave Oven 28L',
             'Dishwasher 12 Place','Bagless Vacuum Cleaner','HEPA Air Purifier','Water Heater 15L'],
             ARRAY[499,399,649,329,549,169,699,149,199,119]::numeric[],
             ARRAY['White','Silver','Black','Graphite','Steel Grey','Beige','Eco','Smart','Compact','Plus','Premium','Deluxe','Inverter','Energy Saver'],
             ARRAY[1,1,1,1,1,1,0.90,1.15,0.85,1.12,1.40,1.30,1.20,1.10]::numeric[]),

            ('Kitchen',
             ARRAY['Philips','Tefal','Morphy Richards','Prestige','Bajaj','Havells'],
             ARRAY['Air Fryer 4.2L','Mixer Grinder 750W','Electric Kettle 1.8L','Induction Cooktop 2000W',
             'Non-Stick Cookware Set 5pc','Pressure Cooker 5L','Rice Cooker 1.8L','2-Slice Toaster',
             'Drip Coffee Maker 12-Cup','Hand Blender 600W'],
             ARRAY[89,69,29,59,79,45,55,35,79,39]::numeric[],
             ARRAY['Black','White','Silver','Red','Steel','Copper','Compact','Eco','Plus','Pro','Premium','Deluxe','Family Size','Digital'],
             ARRAY[1,1,1,1,1,1,0.85,0.90,1.12,1.30,1.40,1.25,1.20,1.15]::numeric[]),

            ('Footwear',
             ARRAY['Nike','Adidas','New Balance','Puma','Reebok','Skechers'],
             ARRAY['Running Shoes Men','Running Shoes Women','Casual Sneakers','Training Shoes','Basketball Shoes',
             'Walking Shoes','Slip-On Shoes','Sports Sandals','Flip Flops','Trail Running Shoes'],
             ARRAY[89,89,69,79,109,65,55,39,25,119]::numeric[],
             ARRAY['Black','White','Grey','Navy','Red','Olive','Blue','Lite','Classic','Eco','Max','Pro','Elite','Limited Edition'],
             ARRAY[1,1,1,1,1,1,1,0.90,1.00,0.95,1.30,1.25,1.50,1.40]::numeric[]),

            ('Men''s Clothing',
             ARRAY['Tommy Hilfiger','Levi''s','Zara','Uniqlo','Allen Solly','H&M'],
             ARRAY['Slim Fit Jeans','Cotton Crew Neck T-Shirt','Polo Shirt','Oxford Button-Down Shirt','Chino Trousers',
             'Hooded Sweatshirt','Puffer Jacket','Formal Blazer','Cargo Shorts','Track Pants'],
             ARRAY[59,19,35,49,55,59,129,149,39,35]::numeric[],
             ARRAY['Black','White','Navy','Grey','Olive','Maroon','Beige','S','M','L','XL','XXL','Premium Cotton','Organic Cotton'],
             ARRAY[1,1,1,1,1,1,1,1,1,1,1,1.05,1.25,1.15]::numeric[]),

            ('Women''s Clothing',
             ARRAY['Marks & Spencer','Mango','Zara','Uniqlo','Vero Moda','H&M'],
             ARRAY['Floral Midi Dress','High-Rise Skinny Jeans','Cotton Tunic Top','Casual Blouse','Knit Cardigan',
             'Yoga Leggings','Denim Jacket','Pleated Skirt','Wool Coat','Maxi Dress'],
             ARRAY[59,55,29,35,49,29,79,39,169,65]::numeric[],
             ARRAY['Black','White','Navy','Red','Olive','Beige','Pink','XS','S','M','L','XL','Premium Cotton','Organic Cotton'],
             ARRAY[1,1,1,1,1,1,1,1,1,1,1,1.05,1.25,1.15]::numeric[]),

            ('Fitness & Sports',
             ARRAY['Nike','Adidas','Puma','Reebok','Decathlon','Cosco'],
             ARRAY['Yoga Mat 6mm','Adjustable Dumbbell Set 20kg','Resistance Bands Set','Foam Roller 45cm','Kettlebell 12kg',
             'Gym Gloves','Ab Roller Wheel','Speed Jump Rope','Gym Duffel Bag','Sports Water Bottle 1L'],
             ARRAY[25,89,19,29,35,19,25,12,39,15]::numeric[],
             ARRAY['Black','Blue','Grey','Red','Green','Purple','Pink','Lite','Starter','Eco','Pro','Premium','Heavy Duty','Compact'],
             ARRAY[1,1,1,1,1,1,1,0.85,0.80,0.90,1.30,1.40,1.30,0.85]::numeric[]),

            ('Beauty & Personal Care',
             ARRAY['L''Oreal','Neutrogena','Nivea','Olay','Dove','Garnier'],
             ARRAY['Face Wash 150ml','Shampoo 650ml','SPF50 Sunscreen 100ml','Conditioner 650ml','Body Lotion 400ml',
             'Deodorant Spray 150ml','Face Moisturizer 50ml','Hand Cream 75ml','Lip Balm 15g','Body Wash 500ml'],
             ARRAY[7,9,14,9,8,6,15,5,4,8]::numeric[],
             ARRAY['Original','Sensitive','Aloe Vera','Fresh','Nourishing','Fragrance Free','Natural','Intense',
             'Travel Size','Pack of 2','Pack of 3','Value Pack','Family Pack','Gift Set'],
             ARRAY[1,1.05,1.05,1,1.10,1.05,1.05,1.15,0.45,1.90,2.70,1.80,2.20,2.50]::numeric[]),

            ('Furniture & Decor',
             ARRAY['IKEA','Urban Ladder','Godrej','Wakefit','Durian','Nilkamal'],
             ARRAY['Ergonomic Office Chair','Study Desk 120cm','5-Tier Bookshelf','Queen Bed Frame','3-Seater Sofa',
             '4-Seater Dining Table','2-Door Wardrobe','TV Unit 150cm','Coffee Table','4-Tier Shoe Rack'],
             ARRAY[189,129,99,399,599,299,279,149,89,35]::numeric[],
             ARRAY['Walnut','Oak','Teak','White','Black','Grey','Compact','Classic','Plus','Premium','Deluxe','Solid Wood','Metal Frame','Modular'],
             ARRAY[1,1,1,1,1,1,0.85,1.00,1.12,1.30,1.25,1.60,1.10,1.20]::numeric[]),

            ('Computer Accessories',
             ARRAY['Logitech','Razer','HP','Anker','Belkin','Targus'],
             ARRAY['Wireless Mouse','Mechanical Keyboard','1080p Webcam','USB-C Hub 7-in-1','Gaming Mouse',
             'Laptop Backpack 15.6-inch','Aluminum Laptop Stand','Keyboard Mouse Combo','Power Bank 20000mAh','Gaming Headset'],
             ARRAY[25,79,59,39,59,45,35,45,39,69]::numeric[],
             ARRAY['Black','White','Grey','Blue','Pink','Graphite','Lite','Compact','Plus','Pro','Max','Elite','Special Edition','RGB'],
             ARRAY[1,1,1,1,1,1,0.85,0.85,1.12,1.30,1.40,1.50,1.20,1.15]::numeric[])
    ),
    brand AS (
        SELECT c.category, b.brand, b.bi
        FROM cat c, unnest(c.brands) WITH ORDINALITY AS b(brand, bi)
    ),
    item AS (
        SELECT c.category, i.item, i.price
        FROM cat c, unnest(c.items, c.prices) AS i(item, price)
    ),
    variant AS (
        SELECT c.category, v.variant, v.vmult
        FROM cat c, unnest(c.variants, c.vmults) AS v(variant, vmult)
    ),
    combos AS (
        SELECT b.category,
               b.brand || ' ' || i.item || ' ' || v.variant AS product_name,
               i.price * (ARRAY[1.25, 1.15, 1.00, 0.95, 0.85, 0.75])[b.bi::int] * v.vmult AS raw_price
FROM brand b
    JOIN item i    ON i.category = b.category
    JOIN variant v ON v.category = b.category
    ),
    picked AS (
SELECT * FROM combos
ORDER BY md5(category || '|' || product_name)
    LIMIT 10000
    ),
    priced AS (
SELECT p.category,
    p.product_name,
    floor(
    p.raw_price * pa.fx
    * (0.92 + ((('x' || substr(md5(p.product_name || 'p'), 1, 8))::bit(32)::bigint) % 17) / 100.0)
    ) + 0.99 AS price,
    (25 + ((('x' || substr(md5(p.product_name || 'q'), 1, 8))::bit(32)::bigint) % 976))::int AS total_qty,
    ((('x' || substr(md5(p.product_name || 'r'), 1, 8))::bit(32)::bigint) % 85)::int AS rem_pct
FROM picked p
    CROSS JOIN params pa
    )
SELECT md5(category || '|' || product_name)::uuid,
    product_name,
       category,
       price::numeric(12,2),
    total_qty,
       floor(total_qty * (0.15 + rem_pct / 100.0))::int,
    (price::numeric(12,2) * total_qty)::numeric(14,2),
    0
FROM priced;