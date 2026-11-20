# Include inside the production HTTPS server block for the storefront and admin.
# The mall-admin service account must be able to write here; Nginx needs read access.
client_max_body_size 11m;

location ^~ /media/ {
    alias /opt/macro/uploads/;
    autoindex off;
    add_header X-Content-Type-Options nosniff always;
    limit_except GET {
        deny all;
    }
}
