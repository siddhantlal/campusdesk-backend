import java.nio.file.*;
import java.sql.*;
import java.util.*;

class DatabaseBenchmark {
    private static final int WARMUP=100, SAMPLES=500;
    public static void main(String[] args) throws Exception {
        long user=Long.parseLong(args[0]), category=Long.parseLong(args[1]);
        Path output=Path.of(args[2]); Files.createDirectories(output);
        try (Connection db=DriverManager.getConnection(System.getenv("DB_URL"),System.getenv("DB_USER"),System.getenv("DB_PASSWORD"))) {
            db.setAutoCommit(false);
            try (PreparedStatement seed=db.prepareStatement("""
                WITH inserted AS (
                  INSERT INTO tickets(title, description, category_id, creator_id, priority, status, created_at, updated_at, version)
                  SELECT 'Benchmark ticket #' || i, 'Synthetic benchmark data', ?, ?,
                    CASE WHEN i % 3 = 0 THEN 'HIGH' ELSE 'MEDIUM' END, 'OPEN',
                    now() - i * interval '1 second', now() - i * interval '1 second', 0
                  FROM generate_series(1,10000) i RETURNING id
                ) INSERT INTO ticket_events(ticket_id, actor_id, event_type, new_value, created_at)
                  SELECT id, ?, 'CREATED', 'OPEN', now() FROM inserted
                """)) {
                seed.setLong(1,category); seed.setLong(2,user); seed.setLong(3,user); seed.executeUpdate();
            }
            db.commit();
            try (Statement statement=db.createStatement()) { statement.execute("ANALYZE tickets"); }
            db.commit();
            String query="SELECT id, title, status, created_at FROM tickets WHERE creator_id=? ORDER BY created_at DESC, id DESC LIMIT 20";
            // Drop only within a transaction, then rollback. Never permanently remove the application index.
            try {
                try (Statement statement=db.createStatement()) { statement.execute("DROP INDEX tickets_creator_created_idx"); }
                plan(db,query,user,output.resolve("before-plan.txt"));
                measure(db,query,user,output.resolve("before.json"));
            } finally { db.rollback(); }
            plan(db,query,user,output.resolve("after-plan.txt"));
            measure(db,query,user,output.resolve("after.json"));
            db.rollback();
        }
    }
    private static void plan(Connection db,String query,long user,Path output) throws Exception {
        try (PreparedStatement stmt=db.prepareStatement("EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) " + query)) {
            stmt.setLong(1,user); StringBuilder text=new StringBuilder();
            try (ResultSet rows=stmt.executeQuery()) { while(rows.next()) text.append(rows.getString(1)).append('\n'); }
            Files.writeString(output,text.toString());
        }
    }
    private static void measure(Connection db,String query,long user,Path output) throws Exception {
        try (PreparedStatement stmt=db.prepareStatement(query)) {
            stmt.setLong(1,user);
            for(int i=0;i<WARMUP;i++) consume(stmt);
            double[] milliseconds=new double[SAMPLES]; long start=System.nanoTime();
            for(int i=0;i<SAMPLES;i++) { long tick=System.nanoTime(); consume(stmt); milliseconds[i]=(System.nanoTime()-tick)/1e6; }
            double seconds=(System.nanoTime()-start)/1e9; Arrays.sort(milliseconds);
            String json=String.format(Locale.ROOT,"""
                {"measurement":"database via JDBC, not application API","warmup":%d,"samples":%d,"concurrency":1,
                 "durationSeconds":%.6f,"medianMs":%.6f,"p95Ms":%.6f,"errorRate":0,"throughputPerSecond":%.3f}
                """,WARMUP,SAMPLES,seconds,(milliseconds[249]+milliseconds[250])/2,milliseconds[474],SAMPLES/seconds);
            Files.writeString(output,json);
        }
    }
    private static void consume(PreparedStatement stmt) throws SQLException {
        try(ResultSet rows=stmt.executeQuery()) { while(rows.next()) { rows.getLong(1); rows.getString(2); rows.getString(3); rows.getTimestamp(4); } }
    }
}
