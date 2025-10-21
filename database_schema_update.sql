-- Database Schema Update for Order Delivery Status Management
-- Run this script if orders table doesn't have proper columns or values

USE music_store;

-- Check and update orders table structure
-- Ensure status column exists and can hold delivery status values
ALTER TABLE orders 
MODIFY COLUMN status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'Delivery status: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED';

-- Ensure delivery_date column exists
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS delivery_date DATETIME NULL COMMENT 'Date when order was delivered';

-- Ensure payment_status column exists
ALTER TABLE orders 
MODIFY COLUMN payment_status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'Payment status: PENDING, COMPLETED, FAILED, REFUNDED';

-- Update any NULL status values to PENDING
UPDATE orders 
SET status = 'PENDING' 
WHERE status IS NULL;

-- Update any NULL payment_status values to PENDING
UPDATE orders 
SET payment_status = 'PENDING' 
WHERE payment_status IS NULL;

-- Verify the schema
DESCRIBE orders;

-- Show sample data
SELECT id, status, payment_status, delivery_date, order_date 
FROM orders 
LIMIT 10;
