CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(250),
    email VARCHAR(100),
    dob DATE,
    gender ENUM("male", "female")
)