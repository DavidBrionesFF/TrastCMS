CREATE TABLE travel_destination (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    summary VARCHAR(500),
    description TEXT,
    image_url VARCHAR(1000),
    country VARCHAR(120),
    region VARCHAR(120),
    city VARCHAR(120),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    featured BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    demo_data BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE travel_activity (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    description VARCHAR(500),
    icon VARCHAR(80),
    image_url VARCHAR(1000),
    active BOOLEAN NOT NULL,
    demo_data BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE travel_extra (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    description VARCHAR(600),
    price DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    pricing_unit VARCHAR(30) NOT NULL,
    max_quantity INT NOT NULL,
    image_url VARCHAR(1000),
    active BOOLEAN NOT NULL,
    demo_data BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE travel_tour (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(240) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    code VARCHAR(80) NOT NULL UNIQUE,
    short_description VARCHAR(600),
    description TEXT,
    included_json TEXT,
    excluded_json TEXT,
    itinerary_json TEXT,
    faq_json TEXT,
    gallery_json TEXT,
    destination_id VARCHAR(36),
    duration_value INT NOT NULL,
    duration_unit VARCHAR(20) NOT NULL,
    difficulty VARCHAR(40),
    minimum_age INT,
    maximum_age INT,
    minimum_group_size INT NOT NULL,
    maximum_group_size INT NOT NULL,
    adult_price DECIMAL(19,4) NOT NULL,
    child_price DECIMAL(19,4) NOT NULL,
    infant_price DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    pricing_mode VARCHAR(30) NOT NULL,
    deposit_percentage DECIMAL(7,4) NOT NULL,
    featured_image_url VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    featured BOOLEAN NOT NULL,
    confirmation_mode VARCHAR(30) NOT NULL,
    demo_data BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_travel_tour_destination FOREIGN KEY (destination_id)
        REFERENCES travel_destination(id)
);

CREATE TABLE travel_tour_activity (
    tour_id VARCHAR(36) NOT NULL,
    activity_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (tour_id, activity_id),
    CONSTRAINT fk_travel_tour_activity_tour FOREIGN KEY (tour_id)
        REFERENCES travel_tour(id) ON DELETE CASCADE,
    CONSTRAINT fk_travel_tour_activity_activity FOREIGN KEY (activity_id)
        REFERENCES travel_activity(id) ON DELETE CASCADE
);

CREATE TABLE travel_departure (
    id VARCHAR(36) PRIMARY KEY,
    tour_id VARCHAR(36) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    capacity INT NOT NULL,
    reserved_seats INT NOT NULL,
    held_seats INT NOT NULL,
    blocked_seats INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    adult_price DECIMAL(19,4),
    child_price DECIMAL(19,4),
    infant_price DECIMAL(19,4),
    meeting_point VARCHAR(500),
    google_calendar_event_id VARCHAR(300),
    google_calendar_url VARCHAR(1000),
    notes TEXT,
    demo_data BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_travel_departure_tour FOREIGN KEY (tour_id)
        REFERENCES travel_tour(id)
);

CREATE TABLE travel_booking (
    id VARCHAR(36) PRIMARY KEY,
    booking_code VARCHAR(40) NOT NULL UNIQUE,
    tour_id VARCHAR(36) NOT NULL,
    departure_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36),
    customer_email VARCHAR(190) NOT NULL,
    customer_name VARCHAR(190) NOT NULL,
    customer_phone VARCHAR(60),
    adult_count INT NOT NULL,
    child_count INT NOT NULL,
    infant_count INT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal DECIMAL(19,4) NOT NULL,
    extras_total DECIMAL(19,4) NOT NULL,
    discount_total DECIMAL(19,4) NOT NULL,
    tax_total DECIMAL(19,4) NOT NULL,
    grand_total DECIMAL(19,4) NOT NULL,
    deposit_required DECIMAL(19,4) NOT NULL,
    amount_paid DECIMAL(19,4) NOT NULL,
    balance_due DECIMAL(19,4) NOT NULL,
    booking_status VARCHAR(40) NOT NULL,
    payment_status VARCHAR(40) NOT NULL,
    order_id VARCHAR(36),
    order_number VARCHAR(40),
    coupon_code VARCHAR(80),
    extras_json TEXT,
    notes TEXT,
    hold_expires_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    demo_data BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_travel_booking_tour FOREIGN KEY (tour_id)
        REFERENCES travel_tour(id),
    CONSTRAINT fk_travel_booking_departure FOREIGN KEY (departure_id)
        REFERENCES travel_departure(id)
);

CREATE TABLE travel_traveller (
    id VARCHAR(36) PRIMARY KEY,
    booking_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    traveller_type VARCHAR(30) NOT NULL,
    birth_date DATE,
    nationality VARCHAR(100),
    dietary_notes VARCHAR(1000),
    medical_notes VARCHAR(1000),
    emergency_name VARCHAR(180),
    emergency_phone VARCHAR(60),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_travel_traveller_booking FOREIGN KEY (booking_id)
        REFERENCES travel_booking(id) ON DELETE CASCADE
);

CREATE TABLE travel_settings (
    id VARCHAR(30) PRIMARY KEY,
    default_currency VARCHAR(3) NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    tax_rate DECIMAL(7,4) NOT NULL,
    booking_hold_minutes INT NOT NULL,
    minimum_lead_hours INT NOT NULL,
    guest_booking BOOLEAN NOT NULL,
    google_calendar_enabled BOOLEAN NOT NULL,
    google_calendar_id VARCHAR(300),
    notification_email VARCHAR(190),
    terms_url VARCHAR(1000),
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX ix_travel_tour_status ON travel_tour(status);
CREATE INDEX ix_travel_tour_destination ON travel_tour(destination_id);
CREATE INDEX ix_travel_departure_starts ON travel_departure(starts_at);
CREATE INDEX ix_travel_booking_email ON travel_booking(customer_email);
CREATE INDEX ix_travel_booking_status ON travel_booking(booking_status);
