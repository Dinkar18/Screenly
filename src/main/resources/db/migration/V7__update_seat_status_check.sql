ALTER TABLE showtime_seats DROP CONSTRAINT IF EXISTS showtime_seats_status_check;
ALTER TABLE showtime_seats ADD CONSTRAINT showtime_seats_status_check CHECK (status IN ('AVAILABLE', 'BOOKED', 'LOCKED', 'RESERVED'));
