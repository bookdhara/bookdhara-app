-- Login moves from phone OTP to email OTP. Phone stays for delivery orders later.
ALTER TABLE users ADD COLUMN email VARCHAR(254);
CREATE UNIQUE INDEX ux_users_email ON users (email);
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;

ALTER TABLE otp_codes ADD COLUMN email VARCHAR(254);
ALTER TABLE otp_codes ALTER COLUMN phone DROP NOT NULL;
CREATE INDEX idx_otp_codes_email_created ON otp_codes (email, created_at);
