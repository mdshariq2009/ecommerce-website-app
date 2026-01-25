-- Insert Admin User (password: admin123)
MERGE INTO users (id, name, email, password, role, created_at) 
KEY(email)
VALUES (1, 'Admin User', 'admin@ecommerce.com', 
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 
        'ADMIN', CURRENT_TIMESTAMP);

-- Insert Sample Products
MERGE INTO products (id, name, description, price, category, stock, image, created_at) 
KEY(id)
VALUES
(1, 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and 512GB SSD', 1299.99, 'Electronics', 50, 'https://via.placeholder.com/300', CURRENT_TIMESTAMP),
(2, 'Wireless Mouse', 'Ergonomic wireless mouse with precision tracking', 29.99, 'Electronics', 200, 'https://via.placeholder.com/300', CURRENT_TIMESTAMP),
(3, 'USB-C Hub', '7-in-1 USB-C hub with HDMI, USB 3.0, and card reader', 49.99, 'Electronics', 150, 'https://via.placeholder.com/300', CURRENT_TIMESTAMP),
(4, 'Mechanical Keyboard', 'RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 100, 'https://via.placeholder.com/300', CURRENT_TIMESTAMP),
(5, 'Webcam HD', '1080p HD webcam with built-in microphone', 79.99, 'Electronics', 75, 'https://via.placeholder.com/300', CURRENT_TIMESTAMP);
