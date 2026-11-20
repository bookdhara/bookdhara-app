-- Google sign-in: Google's stable account id ("sub"), so a later email change on Google
-- still finds the same BookDhara account. Null until the user signs in with Google.
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255);
CREATE UNIQUE INDEX ux_users_google_sub ON users (google_sub);
