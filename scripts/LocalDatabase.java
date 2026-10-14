import java.sql.*;

// Java's single-source launch mode avoids installing psql for the portable Windows runtime.
class LocalDatabase {
    public static void main(String[] args) throws Exception {
        String base = System.getenv("DB_URL").replaceAll("/campusdesk$", "/postgres");
        try (Connection connection = DriverManager.getConnection(base,System.getenv("DB_USER"),System.getenv("DB_PASSWORD"))) {
            for (String name : new String[]{"campusdesk", "campusdesk_test"}) {
                try (PreparedStatement query=connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname=?")) {
                    query.setString(1,name);
                    try (ResultSet result=query.executeQuery()) {
                        if (!result.next()) try (Statement statement=connection.createStatement()) {
                            statement.execute("CREATE DATABASE " + name);
                        }
                    }
                }
            }
        }
    }
}
