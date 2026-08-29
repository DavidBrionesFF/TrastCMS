package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Introduces shared-database discriminator multitenancy without discarding the
 * existing single-site installation. Existing public content and travel data
 * remain in Primary Site, Academy data is moved to Academy, and Store/SaaS data
 * is moved to the SaaS storefront.
 */
public class V21__multisite_foundation extends BaseJavaMigration {
    private static final String PRIMARY = "11111111-1111-1111-1111-111111111111";
    private static final String ACADEMY = "22222222-2222-2222-2222-222222222222";
    private static final String SAAS = "33333333-3333-3333-3333-333333333333";

    private static final List<String> PRIMARY_TABLES = List.of(
            "category", "post", "post_revision", "media_asset", "push_subscription", "plugin_registration",
            "travel_destination", "travel_activity", "travel_extra", "travel_tour",
            "travel_departure", "travel_booking", "travel_traveller",
            "crm_company", "crm_contact", "crm_pipeline_stage", "crm_deal", "crm_activity",
            "crm_form", "crm_submission", "comment",
            "newsletter_subscriber", "newsletter_campaign", "newsletter_send_log",
            "commerce_customer", "commerce_cart", "commerce_cart_item", "commerce_order",
            "commerce_order_item", "commerce_payment", "commerce_coupon",
            "commerce_clinpays_webhook_event"
    );

    private static final List<String> ACADEMY_TABLES = List.of(
            "academy_course", "academy_section", "academy_lesson", "academy_quiz",
            "academy_question", "academy_answer_option", "academy_enrollment",
            "academy_lesson_progress", "academy_quiz_attempt", "academy_question_response",
            "academy_api_token", "academy_import_job"
    );

    private static final List<String> SAAS_TABLES = List.of(
            "store_product", "store_variant", "store_inventory_movement",
            "saas_product", "saas_plan", "saas_subscription", "saas_license",
            "saas_activation", "saas_usage_event", "saas_release"
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        createNetworkTables(c);
        seedSites(c);
        addNetworkAdminFlagAndLegacyMemberships(c);
        ensureLegacyNewsletterSchema(c);

        for (String table : PRIMARY_TABLES) addTenantColumn(c, table, PRIMARY);
        for (String table : ACADEMY_TABLES) addTenantColumn(c, table, ACADEMY);
        for (String table : SAAS_TABLES) addTenantColumn(c, table, SAAS);

        migrateAcademyThemeContent(c);
        migrateReferencedMedia(c);
        migrateCommerceOwnership(c);
        migrateDerivedMemberships(c);
        addTenantForeignKeys(c);
        enlargeSiteSettingKey(c);
        migrateSettings(c);
        seedSiteSuspensionDefaults(c);
        migratePluginStates(c);
        tenantizeUniqueKeys(c);
        addTenantIndexes(c);
    }

    private void createNetworkTables(Connection c) throws SQLException {
        exec(c, "CREATE TABLE IF NOT EXISTS cms_site (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "site_key VARCHAR(80) NOT NULL UNIQUE," +
                "name VARCHAR(160) NOT NULL," +
                "status VARCHAR(24) NOT NULL," +
                "locale VARCHAR(20) NOT NULL," +
                "timezone VARCHAR(80) NOT NULL," +
                "created_at TIMESTAMP NOT NULL," +
                "updated_at TIMESTAMP NOT NULL)");
        exec(c, "CREATE TABLE IF NOT EXISTS cms_site_endpoint (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "site_id VARCHAR(36) NOT NULL," +
                "host VARCHAR(253) NOT NULL," +
                "port INT NOT NULL," +
                "scheme VARCHAR(10) NOT NULL," +
                "primary_endpoint BOOLEAN NOT NULL," +
                "active BOOLEAN NOT NULL," +
                "created_at TIMESTAMP NOT NULL," +
                "CONSTRAINT fk_site_endpoint_site FOREIGN KEY(site_id) REFERENCES cms_site(id)," +
                "CONSTRAINT uq_site_endpoint UNIQUE(host,port))");
        exec(c, "CREATE TABLE IF NOT EXISTS cms_site_membership (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "site_id VARCHAR(36) NOT NULL," +
                "user_id VARCHAR(36) NOT NULL," +
                "role VARCHAR(30) NOT NULL," +
                "enabled BOOLEAN NOT NULL," +
                "created_at TIMESTAMP NOT NULL," +
                "updated_at TIMESTAMP NOT NULL," +
                "CONSTRAINT fk_membership_site FOREIGN KEY(site_id) REFERENCES cms_site(id)," +
                "CONSTRAINT fk_membership_user FOREIGN KEY(user_id) REFERENCES cms_user(id) ON DELETE CASCADE," +
                "CONSTRAINT uq_site_membership UNIQUE(site_id,user_id))");
    }


    /**
     * Newsletter originally shipped a second V17 migration from bundled-plugin
     * resources. Existing IDE deployments resolved the core V17, while a Maven
     * package could expose both V17 resources and make Flyway reject the build.
     * V21 owns the compatibility repair so Newsletter is present regardless of
     * which optional plugin resources existed in the old classpath.
     */
    private void ensureLegacyNewsletterSchema(Connection c) throws SQLException {
        if (!tableExists(c, "newsletter_subscriber")) {
            exec(c, "CREATE TABLE newsletter_subscriber (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "email VARCHAR(190) NOT NULL UNIQUE," +
                    "name VARCHAR(120)," +
                    "status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'," +
                    "push_subscription_id VARCHAR(36)," +
                    "source VARCHAR(120)," +
                    "token VARCHAR(100) NOT NULL," +
                    "subscribed_at TIMESTAMP NOT NULL," +
                    "unsubscribed_at TIMESTAMP," +
                    "created_at TIMESTAMP NOT NULL)");
        }
        if (!tableExists(c, "newsletter_campaign")) {
            exec(c, "CREATE TABLE newsletter_campaign (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "title VARCHAR(220) NOT NULL," +
                    "subject VARCHAR(220) NOT NULL," +
                    "body_html TEXT NOT NULL," +
                    "push_enabled BOOLEAN NOT NULL DEFAULT FALSE," +
                    "push_title VARCHAR(100)," +
                    "push_body VARCHAR(300)," +
                    "push_url VARCHAR(1000)," +
                    "status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'," +
                    "scheduled_at TIMESTAMP," +
                    "sent_at TIMESTAMP," +
                    "recipient_count INTEGER," +
                    "open_count INTEGER NOT NULL DEFAULT 0," +
                    "created_by VARCHAR(190) NOT NULL," +
                    "created_at TIMESTAMP NOT NULL," +
                    "updated_at TIMESTAMP NOT NULL)");
        }
        if (!tableExists(c, "newsletter_send_log")) {
            exec(c, "CREATE TABLE newsletter_send_log (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "campaign_id VARCHAR(36) NOT NULL," +
                    "subscriber_id VARCHAR(36) NOT NULL," +
                    "sent_at TIMESTAMP NOT NULL," +
                    "status VARCHAR(30) NOT NULL DEFAULT 'SENT'," +
                    "CONSTRAINT fk_send_log_campaign FOREIGN KEY (campaign_id) REFERENCES newsletter_campaign(id)," +
                    "CONSTRAINT fk_send_log_subscriber FOREIGN KEY (subscriber_id) REFERENCES newsletter_subscriber(id))");
        }
        try { exec(c, "CREATE INDEX idx_newsletter_sub_status ON newsletter_subscriber(status, subscribed_at)"); } catch (SQLException ignored) { }
        try { exec(c, "CREATE INDEX idx_newsletter_campaign_status ON newsletter_campaign(status, scheduled_at)"); } catch (SQLException ignored) { }
    }

    private void seedSiteSuspensionDefaults(Connection c) throws SQLException {
        if (!tableExists(c, "site_setting")) return;
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("site.suspended", "false");
        defaults.put("site.suspendedTitle", "Servicio Suspendido Por Falta de Pago");
        defaults.put("site.suspendedMessage", "Estimado cliente, su cuenta se encuentra temporalmente suspendida por un saldo pendiente. Por favor contacte con soporte técnico para restablecer su servicio.");
        defaults.put("site.suspendedContactEmail", "soporte@nattechnologies.com");
        defaults.put("site.suspendedContactPhone", "+504 9999-9999");
        for (String site : List.of(PRIMARY, ACADEMY, SAAS)) {
            for (var entry : defaults.entrySet()) {
                String key = site + "::" + entry.getKey();
                if (exists(c, "SELECT 1 FROM site_setting WHERE setting_key=?", key)) continue;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO site_setting(setting_key,setting_value,updated_at) VALUES(?,?,?)")) {
                    ps.setString(1, key);
                    ps.setString(2, entry.getValue());
                    ps.setTimestamp(3, Timestamp.from(Instant.now()));
                    ps.executeUpdate();
                }
            }
        }
    }

    private void seedSites(Connection c) throws SQLException {
        upsertSite(c, PRIMARY, "primary-site", "Primary Site", "es-HN", "America/Tegucigalpa");
        upsertSite(c, ACADEMY, "academy", "Academy", "es-HN", "America/Tegucigalpa");
        upsertSite(c, SAAS, "saas-store", "Software Store", "es-HN", "America/Tegucigalpa");
        insertEndpoint(c, "41111111-1111-1111-1111-111111111111", PRIMARY, "localhost", 8080, "http", true);
        insertEndpoint(c, "42222222-2222-2222-2222-222222222222", ACADEMY, "academy.localhost", 8080, "http", true);
        insertEndpoint(c, "43333333-3333-3333-3333-333333333333", SAAS, "saas.localhost", 8080, "http", true);
    }

    private void upsertSite(Connection c, String id, String key, String name, String locale, String timezone) throws SQLException {
        if (exists(c, "SELECT 1 FROM cms_site WHERE id=?", id)) return;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO cms_site(id,site_key,name,status,locale,timezone,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?)")) {
            ps.setString(1, id); ps.setString(2, key); ps.setString(3, name); ps.setString(4, "ACTIVE");
            ps.setString(5, locale); ps.setString(6, timezone); Timestamp now = Timestamp.from(Instant.now());
            ps.setTimestamp(7, now); ps.setTimestamp(8, now); ps.executeUpdate();
        }
    }

    private void insertEndpoint(Connection c, String id, String site, String host, int port, String scheme, boolean primary) throws SQLException {
        if (exists(c, "SELECT 1 FROM cms_site_endpoint WHERE id=?", id)) return;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO cms_site_endpoint(id,site_id,host,port,scheme,primary_endpoint,active,created_at) VALUES(?,?,?,?,?,?,?,?)")) {
            ps.setString(1, id); ps.setString(2, site); ps.setString(3, host); ps.setInt(4, port); ps.setString(5, scheme);
            ps.setBoolean(6, primary); ps.setBoolean(7, true); ps.setTimestamp(8, Timestamp.from(Instant.now())); ps.executeUpdate();
        }
    }

    private void addNetworkAdminFlagAndLegacyMemberships(Connection c) throws SQLException {
        if (!columnExists(c, "cms_user", "network_admin")) {
            exec(c, "ALTER TABLE cms_user ADD COLUMN network_admin BOOLEAN NOT NULL DEFAULT FALSE");
        }
        // Legacy ADMIN accounts become administrators of Primary Site, not
        // administrators of the whole network. BootstrapService promotes only
        // TRASTCMS_ADMIN_EMAIL to network_admin after Flyway finishes.
        try (PreparedStatement select = c.prepareStatement("SELECT id, role FROM cms_user")) {
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString(1);
                    String role = rs.getString(2);
                    ensureMembership(c, PRIMARY, userId, role);
                }
            }
        }
    }

    private void ensureMembership(Connection c, String siteId, String userId, String role) throws SQLException {
        if (exists(c, "SELECT 1 FROM cms_site_membership WHERE site_id=? AND user_id=?", siteId, userId)) return;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO cms_site_membership(id,site_id,user_id,role,enabled,created_at,updated_at) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, siteId); ps.setString(3, userId);
            ps.setString(4, role); ps.setBoolean(5, true); Timestamp now = Timestamp.from(Instant.now()); ps.setTimestamp(6, now); ps.setTimestamp(7, now); ps.executeUpdate();
        }
    }

    private void addTenantColumn(Connection c, String table, String siteId) throws SQLException {
        if (!tableExists(c, table)) return;
        if (!columnExists(c, table, "site_id")) exec(c, "ALTER TABLE " + table + " ADD COLUMN site_id VARCHAR(36)");
        try (PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET site_id=? WHERE site_id IS NULL OR site_id=''")) {
            ps.setString(1, siteId); ps.executeUpdate();
        }
        setNotNull(c, table, "site_id", "VARCHAR(36)");
    }

    private void migrateAcademyThemeContent(Connection c) throws SQLException {
        if (!tableExists(c, "post") || !columnExists(c, "post", "theme_origin")) return;
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE post SET site_id=? WHERE LOWER(COALESCE(theme_origin,''))='nexus'")) {
            ps.setString(1, ACADEMY);
            ps.executeUpdate();
        }
        if (tableExists(c, "post_revision")) {
            exec(c, "UPDATE post_revision SET site_id=(SELECT p.site_id FROM post p WHERE p.id=post_revision.post_id) " +
                    "WHERE EXISTS (SELECT 1 FROM post p WHERE p.id=post_revision.post_id)");
        }
        if (tableExists(c, "comment")) {
            exec(c, "UPDATE comment SET site_id=(SELECT p.site_id FROM post p WHERE p.id=comment.post_id) " +
                    "WHERE EXISTS (SELECT 1 FROM post p WHERE p.id=comment.post_id)");
        }
    }


    private void migrateReferencedMedia(Connection c) throws SQLException {
        if (!tableExists(c, "media_asset") || !columnExists(c, "media_asset", "site_id")) return;

        Set<String> academyMedia = new LinkedHashSet<>();
        if (tableExists(c, "academy_lesson") && columnExists(c, "academy_lesson", "media_id")) {
            collectValues(c, "SELECT media_id FROM academy_lesson WHERE media_id IS NOT NULL AND media_id<>''", academyMedia, false);
        }
        if (tableExists(c, "academy_course") && columnExists(c, "academy_course", "featured_image_url")) {
            collectValues(c, "SELECT featured_image_url FROM academy_course WHERE featured_image_url IS NOT NULL", academyMedia, true);
        }
        if (tableExists(c, "post") && columnExists(c, "post", "featured_image_url")) {
            collectValues(c, "SELECT featured_image_url FROM post WHERE site_id='" + ACADEMY + "' AND featured_image_url IS NOT NULL", academyMedia, true);
        }
        moveMedia(c, academyMedia, ACADEMY);

        Set<String> saasMedia = new LinkedHashSet<>();
        if (tableExists(c, "store_product") && columnExists(c, "store_product", "featured_image_url")) {
            collectValues(c, "SELECT featured_image_url FROM store_product WHERE site_id='" + SAAS + "' AND featured_image_url IS NOT NULL", saasMedia, true);
        }
        if (tableExists(c, "store_variant") && columnExists(c, "store_variant", "image_url")) {
            collectValues(c, "SELECT image_url FROM store_variant WHERE site_id='" + SAAS + "' AND image_url IS NOT NULL", saasMedia, true);
        }
        // An asset explicitly referenced by Academy takes precedence over Store
        // in the unlikely event that a legacy record reused the exact same media id.
        saasMedia.removeAll(academyMedia);
        moveMedia(c, saasMedia, SAAS);
    }

    private void collectValues(Connection c, String sql, Set<String> target, boolean parseMediaUrl) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String value = rs.getString(1);
                if (value == null || value.isBlank()) continue;
                String id = parseMediaUrl ? mediaIdFromUrl(value) : value.trim();
                if (id != null && !id.isBlank()) target.add(id);
            }
        }
    }

    private String mediaIdFromUrl(String value) {
        String marker = "/api/public/media/";
        int index = value.indexOf(marker);
        if (index < 0) return null;
        String candidate = value.substring(index + marker.length());
        int end = candidate.indexOf('?');
        if (end >= 0) candidate = candidate.substring(0, end);
        end = candidate.indexOf('#');
        if (end >= 0) candidate = candidate.substring(0, end);
        end = candidate.indexOf('/');
        if (end >= 0) candidate = candidate.substring(0, end);
        return candidate.trim();
    }

    private void moveMedia(Connection c, Set<String> mediaIds, String siteId) throws SQLException {
        if (mediaIds.isEmpty()) return;
        try (PreparedStatement ps = c.prepareStatement("UPDATE media_asset SET site_id=? WHERE id=?")) {
            for (String id : mediaIds) {
                ps.setString(1, siteId);
                ps.setString(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Existing commerce is assumed to belong to Primary Site unless the line
     * items prove that the transaction was created by Academy, Store, or SaaS.
     * This preserves live tourism orders while retaining pre-multisite digital
     * sales in their natural tenant.
     */
    private void migrateCommerceOwnership(Connection c) throws SQLException {
        if (!tableExists(c, "commerce_order") || !tableExists(c, "commerce_order_item")) return;
        assignParentFromProvider(c, "commerce_order", "commerce_order_item", "order_id");
        if (tableExists(c, "commerce_payment")) {
            exec(c, "UPDATE commerce_payment SET site_id=(SELECT o.site_id FROM commerce_order o WHERE o.id=commerce_payment.order_id) " +
                    "WHERE EXISTS (SELECT 1 FROM commerce_order o WHERE o.id=commerce_payment.order_id)");
        }
        exec(c, "UPDATE commerce_order_item SET site_id=(SELECT o.site_id FROM commerce_order o WHERE o.id=commerce_order_item.order_id) " +
                "WHERE EXISTS (SELECT 1 FROM commerce_order o WHERE o.id=commerce_order_item.order_id)");

        if (tableExists(c, "commerce_cart") && tableExists(c, "commerce_cart_item")) {
            assignParentFromProvider(c, "commerce_cart", "commerce_cart_item", "cart_id");
            exec(c, "UPDATE commerce_cart_item SET site_id=(SELECT x.site_id FROM commerce_cart x WHERE x.id=commerce_cart_item.cart_id) " +
                    "WHERE EXISTS (SELECT 1 FROM commerce_cart x WHERE x.id=commerce_cart_item.cart_id)");
        }
        alignCommerceCustomers(c);
    }

    private void assignParentFromProvider(Connection c, String parentTable, String itemTable, String fk) throws SQLException {
        String expression = "CASE " +
                "WHEN EXISTS (SELECT 1 FROM " + itemTable + " i WHERE i." + fk + "=p.id AND LOWER(i.provider_key) IN ('saas','store')) THEN '" + SAAS + "' " +
                "WHEN EXISTS (SELECT 1 FROM " + itemTable + " i WHERE i." + fk + "=p.id AND LOWER(i.provider_key)='academy') THEN '" + ACADEMY + "' " +
                "ELSE '" + PRIMARY + "' END";
        exec(c, "UPDATE " + parentTable + " p SET site_id=" + expression);
    }

    private void alignCommerceCustomers(Connection c) throws SQLException {
        if (!tableExists(c, "commerce_customer")) return;
        // The legacy index is global; remove it before duplicating the same email into another tenant.
        dropSingleColumnUnique(c, "commerce_customer", "email");
        // Duplicate a customer identity when historic orders/carts span tenants.
        List<CustomerRow> originals = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT id,email,display_name,phone,metadata_json,created_at,updated_at FROM commerce_customer")) {
            while (rs.next()) originals.add(new CustomerRow(
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    rs.getTimestamp(6), rs.getTimestamp(7)));
        }
        for (CustomerRow customer : originals) {
            LinkedHashSet<String> tenants = new LinkedHashSet<>();
            collectCustomerTenants(c, "commerce_order", customer.id(), tenants);
            collectCustomerTenants(c, "commerce_cart", customer.id(), tenants);
            if (tenants.isEmpty()) tenants.add(PRIMARY);
            boolean first = true;
            for (String tenant : tenants) {
                String targetId = first ? customer.id() : UUID.randomUUID().toString();
                if (first) {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE commerce_customer SET site_id=? WHERE id=?")) {
                        ps.setString(1, tenant); ps.setString(2, customer.id()); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO commerce_customer(id,email,display_name,phone,metadata_json,created_at,updated_at,site_id) VALUES(?,?,?,?,?,?,?,?)")) {
                        ps.setString(1, targetId); ps.setString(2, customer.email()); ps.setString(3, customer.displayName());
                        ps.setString(4, customer.phone()); ps.setString(5, customer.metadataJson());
                        ps.setTimestamp(6, customer.createdAt()); ps.setTimestamp(7, customer.updatedAt()); ps.setString(8, tenant);
                        ps.executeUpdate();
                    }
                }
                rewireCustomer(c, "commerce_order", customer.id(), targetId, tenant);
                rewireCustomer(c, "commerce_cart", customer.id(), targetId, tenant);
                first = false;
            }
        }
    }

    private void collectCustomerTenants(Connection c, String table, String customerId, Set<String> target) throws SQLException {
        if (!tableExists(c, table) || !columnExists(c, table, "customer_id")) return;
        try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT site_id FROM " + table + " WHERE customer_id=?")) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) if (rs.getString(1) != null) target.add(rs.getString(1)); }
        }
    }

    private void rewireCustomer(Connection c, String table, String oldId, String newId, String tenant) throws SQLException {
        if (!tableExists(c, table) || !columnExists(c, table, "customer_id")) return;
        try (PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET customer_id=? WHERE customer_id=? AND site_id=?")) {
            ps.setString(1, newId); ps.setString(2, oldId); ps.setString(3, tenant); ps.executeUpdate();
        }
    }

    /**
     * Preserve access to products that were already associated with a user before
     * multisite existed. Legacy identities retain Primary Site membership for
     * backwards compatibility, while Academy and SaaS memberships are inferred
     * from enrollments, tokens, licenses/subscriptions and historical orders.
     */
    private void migrateDerivedMemberships(Connection c) throws SQLException {
        if (tableExists(c, "academy_enrollment")) {
            ensureMembershipsFromUserId(c, ACADEMY, "SELECT DISTINCT user_id FROM academy_enrollment WHERE user_id IS NOT NULL AND user_id<>''");
            ensureMembershipsFromEmail(c, ACADEMY, "SELECT DISTINCT email FROM academy_enrollment WHERE email IS NOT NULL AND email<>''");
        }
        if (tableExists(c, "academy_api_token") && columnExists(c, "academy_api_token", "user_id")) {
            ensureMembershipsFromUserId(c, ACADEMY, "SELECT DISTINCT user_id FROM academy_api_token WHERE user_id IS NOT NULL AND user_id<>''");
        }
        if (tableExists(c, "saas_subscription")) {
            ensureMembershipsFromEmail(c, SAAS, "SELECT DISTINCT customer_email FROM saas_subscription WHERE customer_email IS NOT NULL AND customer_email<>''");
        }
        if (tableExists(c, "saas_license")) {
            ensureMembershipsFromEmail(c, SAAS, "SELECT DISTINCT customer_email FROM saas_license WHERE customer_email IS NOT NULL AND customer_email<>''");
        }
        if (tableExists(c, "commerce_order") && columnExists(c, "commerce_order", "site_id")) {
            ensureMembershipsFromEmail(c, ACADEMY, "SELECT DISTINCT customer_email FROM commerce_order WHERE site_id='" + ACADEMY + "' AND customer_email IS NOT NULL AND customer_email<>''");
            ensureMembershipsFromEmail(c, SAAS, "SELECT DISTINCT customer_email FROM commerce_order WHERE site_id='" + SAAS + "' AND customer_email IS NOT NULL AND customer_email<>''");
        }
        if (tableExists(c, "commerce_customer") && columnExists(c, "commerce_customer", "site_id")) {
            ensureMembershipsFromEmail(c, ACADEMY, "SELECT DISTINCT email FROM commerce_customer WHERE site_id='" + ACADEMY + "' AND email IS NOT NULL AND email<>''");
            ensureMembershipsFromEmail(c, SAAS, "SELECT DISTINCT email FROM commerce_customer WHERE site_id='" + SAAS + "' AND email IS NOT NULL AND email<>''");
        }
    }

    private void ensureMembershipsFromUserId(Connection c, String siteId, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String userId = rs.getString(1);
                if (userId != null && exists(c, "SELECT 1 FROM cms_user WHERE id=?", userId)) {
                    ensureMembership(c, siteId, userId, "CUSTOMER");
                }
            }
        }
    }

    private void ensureMembershipsFromEmail(Connection c, String siteId, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String email = rs.getString(1);
                if (email == null || email.isBlank()) continue;
                try (PreparedStatement ps = c.prepareStatement("SELECT id FROM cms_user WHERE LOWER(email)=LOWER(?)")) {
                    ps.setString(1, email);
                    try (ResultSet user = ps.executeQuery()) {
                        if (user.next()) ensureMembership(c, siteId, user.getString(1), "CUSTOMER");
                    }
                }
            }
        }
    }

    private void addTenantForeignKeys(Connection c) throws SQLException {
        for (String table : concat(PRIMARY_TABLES, ACADEMY_TABLES, SAAS_TABLES)) {
            if (!tableExists(c, table) || !columnExists(c, table, "site_id")) continue;
            String name = "fk_" + table + "_site";
            if (name.length() > 60) name = "fk_site_" + Integer.toUnsignedString(table.hashCode());
            if (!constraintExists(c, table, name)) {
                exec(c, "ALTER TABLE " + table + " ADD CONSTRAINT " + name + " FOREIGN KEY(site_id) REFERENCES cms_site(id)");
            }
        }
    }

    private void enlargeSiteSettingKey(Connection c) throws SQLException {
        if (!tableExists(c, "site_setting") || !columnExists(c, "site_setting", "setting_key")) return;
        String product = c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (product.contains("mysql") || product.contains("mariadb")) {
            exec(c, "ALTER TABLE site_setting MODIFY setting_key VARCHAR(260) NOT NULL");
        } else if (product.contains("postgres")) {
            exec(c, "ALTER TABLE site_setting ALTER COLUMN setting_key TYPE VARCHAR(260)");
        } else {
            exec(c, "ALTER TABLE site_setting ALTER COLUMN setting_key VARCHAR(260) NOT NULL");
        }
    }

    private void migrateSettings(Connection c) throws SQLException {
        prefixPrimaryKeys(c, "site_setting", "setting_key", PRIMARY + "::");
        if (tableExists(c, "commerce_settings")) updateId(c, "commerce_settings", "default", PRIMARY);
        if (tableExists(c, "travel_settings")) {
            enlargeVarcharId(c, "travel_settings", 36);
            updateId(c, "travel_settings", "default", PRIMARY);
        }
        if (tableExists(c, "academy_settings")) updateId(c, "academy_settings", "academy", ACADEMY);
        if (tableExists(c, "translation_settings")) {
            enlargeVarcharId(c, "translation_settings", 36);
            updateId(c, "translation_settings", "default", PRIMARY);
        }
        rebuildSaasSettings(c);
    }

    private void enlargeVarcharId(Connection c, String table, int length) throws SQLException {
        if (!tableExists(c, table) || !columnExists(c, table, "id")) return;
        String product = c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (product.contains("mysql") || product.contains("mariadb")) {
            exec(c, "ALTER TABLE " + table + " MODIFY id VARCHAR(" + length + ") NOT NULL");
        } else if (product.contains("postgres")) {
            exec(c, "ALTER TABLE " + table + " ALTER COLUMN id TYPE VARCHAR(" + length + ")");
        } else {
            exec(c, "ALTER TABLE " + table + " ALTER COLUMN id VARCHAR(" + length + ") NOT NULL");
        }
    }

    private void prefixPrimaryKeys(Connection c, String table, String column, String prefix) throws SQLException {
        if (!tableExists(c, table)) return;
        List<String> values = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT " + column + " FROM " + table)) {
            while (rs.next()) values.add(rs.getString(1));
        }
        for (String value : values) {
            if (value == null || value.contains("::")) continue;
            try (PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET " + column + "=? WHERE " + column + "=?")) {
                ps.setString(1, prefix + value); ps.setString(2, value); ps.executeUpdate();
            }
        }
    }

    private void updateId(Connection c, String table, String oldId, String newId) throws SQLException {
        if (!columnExists(c, table, "id")) return;
        try (PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET id=? WHERE id=?")) {
            ps.setString(1, newId); ps.setString(2, oldId); ps.executeUpdate();
        }
    }

    private void rebuildSaasSettings(Connection c) throws SQLException {
        if (!tableExists(c, "saas_settings")) return;
        String tmp = "saas_settings_multisite";
        if (!tableExists(c, tmp)) {
            exec(c, "CREATE TABLE " + tmp + " (" +
                    "id VARCHAR(36) PRIMARY KEY, company_name VARCHAR(180) NOT NULL, support_email VARCHAR(190), billing_email VARCHAR(190)," +
                    "default_currency VARCHAR(3) NOT NULL, default_trial_days INT NOT NULL, default_grace_days INT NOT NULL," +
                    "allow_self_service_cancel BOOLEAN NOT NULL, allow_plan_changes BOOLEAN NOT NULL, require_device_name BOOLEAN NOT NULL," +
                    "show_annual_savings BOOLEAN NOT NULL, pricing_headline VARCHAR(250), pricing_description VARCHAR(1000), portal_message VARCHAR(1000)," +
                    "terms_url VARCHAR(1000), privacy_url VARCHAR(1000), updated_at TIMESTAMP NOT NULL)");
        }
        if (!exists(c, "SELECT 1 FROM " + tmp + " WHERE id=?", SAAS)) {
            exec(c, "INSERT INTO " + tmp + "(id,company_name,support_email,billing_email,default_currency,default_trial_days,default_grace_days,allow_self_service_cancel,allow_plan_changes,require_device_name,show_annual_savings,pricing_headline,pricing_description,portal_message,terms_url,privacy_url,updated_at) " +
                    "SELECT '" + SAAS + "',company_name,support_email,billing_email,default_currency,default_trial_days,default_grace_days,allow_self_service_cancel,allow_plan_changes,require_device_name,show_annual_savings,pricing_headline,pricing_description,portal_message,terms_url,privacy_url,updated_at FROM saas_settings");
        }
        exec(c, "DROP TABLE saas_settings");
        renameTable(c, tmp, "saas_settings");
    }

    private void migratePluginStates(Connection c) throws SQLException {
        if (!tableExists(c, "bundled_plugin_state")) return;
        Map<String, State> existing = new HashMap<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT plugin_id,installed_version,enabled FROM bundled_plugin_state")) {
            while (rs.next()) existing.put(rs.getString(1), new State(rs.getString(2), rs.getBoolean(3)));
        }
        for (String plugin : existing.keySet()) {
            if (plugin.contains("::")) continue;
            try (PreparedStatement ps = c.prepareStatement("UPDATE bundled_plugin_state SET plugin_id=? WHERE plugin_id=?")) {
                ps.setString(1, PRIMARY + "::" + plugin); ps.setString(2, plugin); ps.executeUpdate();
            }
        }
        List<String> all = List.of("trastcrm","trastpay","traststore","trastsaas","trasttravel","trastaccount","trasttranslate","trastmaintenance","trastcomments","trastnewsletter","trastacademy");
        Set<String> primary = Set.of("trastcrm","trastpay","trasttravel","trastaccount","trasttranslate","trastmaintenance");
        Set<String> academy = Set.of("trastpay","trastaccount","trasttranslate","trastacademy");
        Set<String> saas = Set.of("trastpay","traststore","trastsaas","trastaccount","trasttranslate","trastnewsletter");
        for (String plugin : all) {
            State state = existing.get(plugin);
            ensurePluginState(c, PRIMARY, plugin, state == null ? "3.4.0" : state.version, state == null ? primary.contains(plugin) : state.enabled);
            ensurePluginState(c, ACADEMY, plugin, state == null ? "3.4.0" : state.version, academy.contains(plugin));
            ensurePluginState(c, SAAS, plugin, state == null ? "3.4.0" : state.version, saas.contains(plugin));
        }
    }

    private void ensurePluginState(Connection c, String site, String plugin, String version, boolean enabled) throws SQLException {
        String key = site + "::" + plugin;
        if (exists(c, "SELECT 1 FROM bundled_plugin_state WHERE plugin_id=?", key)) return;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO bundled_plugin_state(plugin_id,installed_version,enabled,updated_at) VALUES(?,?,?,?)")) {
            ps.setString(1, key); ps.setString(2, version == null ? "3.4.0" : version); ps.setBoolean(3, enabled); ps.setTimestamp(4, Timestamp.from(Instant.now())); ps.executeUpdate();
        }
    }

    private void tenantizeUniqueKeys(Connection c) throws SQLException {
        Map<String, List<UniqueDef>> defs = new LinkedHashMap<>();
        defs.put("category", List.of(new UniqueDef("name","uq_category_site_name"), new UniqueDef("slug","uq_category_site_slug")));
        defs.put("post", List.of(new UniqueDef("slug","uq_post_site_slug")));
        defs.put("plugin_registration", List.of(new UniqueDef("plugin_key","uq_plugin_registration_site_key")));
        defs.put("push_subscription", List.of(new UniqueDef("endpoint","uq_push_site_endpoint")));
        defs.put("academy_course", List.of(new UniqueDef("slug","uq_academy_site_slug")));
        defs.put("store_product", List.of(new UniqueDef("slug","uq_store_site_slug"), new UniqueDef("sku","uq_store_site_sku")));
        defs.put("store_variant", List.of(new UniqueDef("sku","uq_store_variant_site_sku")));
        defs.put("travel_destination", List.of(new UniqueDef("slug","uq_travel_destination_site_slug")));
        defs.put("travel_activity", List.of(new UniqueDef("slug","uq_travel_activity_site_slug")));
        defs.put("travel_extra", List.of(new UniqueDef("slug","uq_travel_extra_site_slug")));
        defs.put("travel_tour", List.of(new UniqueDef("slug","uq_travel_tour_site_slug"), new UniqueDef("code","uq_travel_tour_site_code")));
        defs.put("travel_booking", List.of(new UniqueDef("booking_code","uq_travel_booking_site_code")));
        defs.put("newsletter_subscriber", List.of(new UniqueDef("email","uq_newsletter_site_email")));
        defs.put("crm_form", List.of(new UniqueDef("form_key","uq_crm_form_site_key")));
        defs.put("commerce_customer", List.of(new UniqueDef("email","uq_commerce_customer_site_email")));
        defs.put("commerce_order", List.of(new UniqueDef("order_number","uq_commerce_order_site_number")));
        defs.put("commerce_coupon", List.of(new UniqueDef("code","uq_commerce_coupon_site_code")));
        defs.put("saas_product", List.of(new UniqueDef("product_key","uq_saas_product_site_key")));
        for (var entry : defs.entrySet()) {
            if (!tableExists(c, entry.getKey()) || !columnExists(c, entry.getKey(), "site_id")) continue;
            for (UniqueDef def : entry.getValue()) {
                dropSingleColumnUnique(c, entry.getKey(), def.column);
                if (!constraintExists(c, entry.getKey(), def.name)) {
                    exec(c, "ALTER TABLE " + entry.getKey() + " ADD CONSTRAINT " + def.name + " UNIQUE(site_id," + def.column + ")");
                }
            }
        }
    }

    private void addTenantIndexes(Connection c) throws SQLException {
        for (String table : concat(PRIMARY_TABLES, ACADEMY_TABLES, SAAS_TABLES)) {
            if (!tableExists(c, table) || !columnExists(c, table, "site_id")) continue;
            String name = "ix_" + table.replace("commerce_", "c_").replace("academy_", "a_").replace("travel_", "t_").replace("newsletter_", "n_").replace("store_", "s_").replace("saas_", "z_") + "_site";
            if (name.length() > 60) name = "ix_site_" + Math.abs(table.hashCode());
            try { exec(c, "CREATE INDEX " + name + " ON " + table + "(site_id)"); } catch (SQLException ignored) { }
        }
    }

    private void dropSingleColumnUnique(Connection c, String table, String column) throws SQLException {
        Set<String> candidates = new LinkedHashSet<>();
        String sql = "SELECT tc.constraint_name FROM information_schema.table_constraints tc " +
                "JOIN information_schema.key_column_usage kcu ON tc.constraint_name=kcu.constraint_name AND tc.table_name=kcu.table_name " +
                "WHERE LOWER(tc.table_name)=? AND tc.constraint_type='UNIQUE' GROUP BY tc.constraint_name " +
                "HAVING COUNT(*)=1 AND MAX(LOWER(kcu.column_name))=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table.toLowerCase(Locale.ROOT)); ps.setString(2, column.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) candidates.add(rs.getString(1)); }
        } catch (SQLException ignored) { }
        for (String name : candidates) dropConstraintOrIndex(c, table, name);

        Map<String, List<String>> indexes = new LinkedHashMap<>();
        try (ResultSet rs = c.getMetaData().getIndexInfo(null, null, table, true, false)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME"); String col = rs.getString("COLUMN_NAME");
                if (name != null && col != null) indexes.computeIfAbsent(name, k -> new ArrayList<>()).add(col.toLowerCase(Locale.ROOT));
            }
        } catch (SQLException ignored) { }
        for (var e : indexes.entrySet()) if (e.getValue().size() == 1 && e.getValue().get(0).equals(column.toLowerCase(Locale.ROOT))) dropConstraintOrIndex(c, table, e.getKey());
    }

    private void dropConstraintOrIndex(Connection c, String table, String name) {
        if (name == null || name.toUpperCase(Locale.ROOT).contains("PRIMARY")) return;
        String q = quote(c, name);
        String product;
        try { product = c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT); } catch (SQLException e) { product = ""; }
        try { exec(c, "ALTER TABLE " + table + " DROP CONSTRAINT " + q); return; } catch (SQLException ignored) { }
        try {
            if (product.contains("mysql") || product.contains("mariadb")) exec(c, "ALTER TABLE " + table + " DROP INDEX " + q);
            else exec(c, "DROP INDEX " + q);
        } catch (SQLException ignored) { }
    }

    private void setNotNull(Connection c, String table, String column, String type) throws SQLException {
        String product = c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (product.contains("mysql") || product.contains("mariadb")) exec(c, "ALTER TABLE " + table + " MODIFY " + column + " " + type + " NOT NULL");
        else exec(c, "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET NOT NULL");
    }

    private void renameTable(Connection c, String from, String to) throws SQLException {
        String product = c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (product.contains("mysql") || product.contains("mariadb")) exec(c, "RENAME TABLE " + from + " TO " + to);
        else exec(c, "ALTER TABLE " + from + " RENAME TO " + to);
    }

    private boolean tableExists(Connection c, String table) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        for (String candidate : List.of(table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT))) {
            try (ResultSet rs = md.getTables(null, null, candidate, new String[]{"TABLE"})) { if (rs.next()) return true; }
        }
        return false;
    }

    private boolean columnExists(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        for (String t : List.of(table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT))) {
            try (ResultSet rs = md.getColumns(null, null, t, null)) {
                while (rs.next()) if (column.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean constraintExists(Connection c, String table, String name) {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM information_schema.table_constraints WHERE LOWER(table_name)=? AND LOWER(constraint_name)=?")) {
            ps.setString(1, table.toLowerCase(Locale.ROOT)); ps.setString(2, name.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException ignored) { return false; }
    }

    private boolean exists(Connection c, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void exec(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) { st.execute(sql); }
    }

    private String quote(Connection c, String identifier) {
        try {
            String q = c.getMetaData().getIdentifierQuoteString();
            if (q == null || q.isBlank()) return identifier;
            return q + identifier.replace(q, q + q) + q;
        } catch (SQLException e) { return identifier; }
    }

    @SafeVarargs
    private final <T> List<T> concat(List<T>... lists) {
        List<T> result = new ArrayList<>();
        for (List<T> list : lists) result.addAll(list);
        return result;
    }

    private record CustomerRow(String id, String email, String displayName, String phone, String metadataJson, Timestamp createdAt, Timestamp updatedAt) {}
    private record State(String version, boolean enabled) {}
    private record UniqueDef(String column, String name) {}
}
