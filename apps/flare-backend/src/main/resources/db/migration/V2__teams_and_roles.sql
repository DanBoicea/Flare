-- Teams and staff roles: real configuration, not demo data (docs/architecture/data-model.md).

insert into team (id, name_ro, name_en) values
    ('CLEANING', 'Curățenie', 'Cleaning'),
    ('MAINTENANCE', 'Mentenanță', 'Maintenance'),
    ('SECURITY', 'Securitate', 'Security'),
    ('PASSENGER_ASSISTANCE', 'Asistență pasageri', 'Passenger assistance');

insert into role (id, name_ro, name_en) values
    ('STAFF', 'Personal', 'Staff'),
    ('DUTY_MANAGER', 'Manager de tură', 'Duty manager');
