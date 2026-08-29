-- Site suspension settings for SaaS tenant status management
MERGE INTO site_setting (setting_key, setting_value, updated_at) KEY (setting_key) VALUES ('site.suspended', 'false', CURRENT_TIMESTAMP);
MERGE INTO site_setting (setting_key, setting_value, updated_at) KEY (setting_key) VALUES ('site.suspendedTitle', 'Servicio Suspendido Por Falta de Pago', CURRENT_TIMESTAMP);
MERGE INTO site_setting (setting_key, setting_value, updated_at) KEY (setting_key) VALUES ('site.suspendedMessage', 'Estimado cliente, su cuenta se encuentra temporalmente suspendida por un saldo pendiente. Por favor contacte con soporte técnico para restablecer su servicio.', CURRENT_TIMESTAMP);
MERGE INTO site_setting (setting_key, setting_value, updated_at) KEY (setting_key) VALUES ('site.suspendedContactEmail', 'soporte@nattechnologies.com', CURRENT_TIMESTAMP);
MERGE INTO site_setting (setting_key, setting_value, updated_at) KEY (setting_key) VALUES ('site.suspendedContactPhone', '+504 9999-9999', CURRENT_TIMESTAMP);
