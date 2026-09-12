INSERT INTO products (name, description, price, stock_quantity, category, created_at) VALUES
('Laptop Pro 15', 'High performance laptop with 16GB RAM', 89999.00, 10, 'Electronics', NOW()),
('Wireless Mouse', 'Ergonomic wireless mouse', 999.00, 50, 'Electronics', NOW()),
('Cotton T-Shirt', 'Premium cotton t-shirt', 499.00, 100, 'Fashion', NOW()),
('Java Programming Book', 'Complete guide to Java Full Stack', 799.00, 25, 'Books', NOW())
ON CONFLICT DO NOTHING;
