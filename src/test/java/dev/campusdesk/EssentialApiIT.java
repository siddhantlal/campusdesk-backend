package dev.campusdesk;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.*;
import dev.campusdesk.users.*;
import dev.campusdesk.tickets.*;
import dev.campusdesk.categories.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc
class EssentialApiIT {
    private static final String PASSWORD="Essential-Test-Password-42";
    private static PostgreSQLContainer<?> postgres;
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        String url=System.getProperty("it.db.url");
        if (url==null) {
            postgres=new PostgreSQLContainer<>("postgres:17.6-alpine"); postgres.start();
            properties.add("spring.datasource.url",postgres::getJdbcUrl);
            properties.add("spring.datasource.username",postgres::getUsername);
            properties.add("spring.datasource.password",postgres::getPassword);
        } else {
            properties.add("spring.datasource.url",() -> url);
            properties.add("spring.datasource.username",() -> System.getProperty("it.db.user"));
            properties.add("spring.datasource.password",() -> System.getProperty("it.db.password"));
        }
        properties.add("app.jwt-secret",() -> Base64.getEncoder().encodeToString("test-only-key-32-bytes-long-12345678".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        properties.add("app.bootstrap.enabled",() -> false);
        properties.add("app.demo.enabled",() -> false);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired TicketRepository tickets;
    @Autowired CategoryRepository categories;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory emf;
    private String admin,student,other,staff,secondStaff;
    private long staffId,secondStaffId,studentId,categoryId;
    @BeforeEach void setup() throws Exception {
        jdbc.execute("TRUNCATE ticket_events, comments, tickets, categories, app_users RESTART IDENTITY CASCADE");
        String hash=encoder.encode(PASSWORD);
        users.saveAndFlush(new User("Admin","admin@test.example",hash,Role.ADMIN));
        studentId=users.saveAndFlush(new User("Student","student@test.example",hash,Role.STUDENT)).getId();
        users.saveAndFlush(new User("Other","other@test.example",hash,Role.STUDENT));
        staffId=users.saveAndFlush(new User("Staff","staff@test.example",hash,Role.STAFF)).getId();
        secondStaffId=users.saveAndFlush(new User("Staff Two","staff2@test.example",hash,Role.STAFF)).getId();
        categoryId=categories.saveAndFlush(new Category("IT","Computing services")).getId();
        admin=login("admin"); student=login("student"); other=login("other"); staff=login("staff"); secondStaff=login("staff2");
    }
    private String login(String name) throws Exception {
        return json(call("POST","/api/v1/auth/login",null,Map.of("email",name+"@test.example","password",PASSWORD),200)).get("accessToken").asText();
    }
    private MvcResult call(String method,String path,String token,Object body,int expected) throws Exception {
        // URI overload preserves already URL-encoded queries; template overload would encode '%' again.
        var uri=java.net.URI.create(path);
        var request=switch(method) { case "POST" -> post(uri); case "PATCH" -> patch(uri); default -> get(uri); };
        if (token!=null) request.header("Authorization","Bearer "+token);
        if (body!=null) request.contentType("application/json").content(mapper.writeValueAsBytes(body));
        MvcResult result=mvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).as(method+" "+path+" -> "+result.getResponse().getContentAsString()).isEqualTo(expected);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("passwordHash","password_hash");
        return result;
    }
    private JsonNode json(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }
    private JsonNode create(String title) throws Exception {
        return json(call("POST","/api/v1/tickets",student,Map.of("title",title,"description","Campus network needs repair","categoryId",categoryId,"priority","HIGH"),201));
    }
    private JsonNode assign(long id,long assignee,long version) throws Exception {
        return json(call("PATCH","/api/v1/tickets/"+id+"/assignment",admin,Map.of("assigneeId",assignee,"version",version),200));
    }
    private JsonNode status(long id,String token,String target,long version,String field,String value) throws Exception {
        Map<String,Object> body=new HashMap<>(Map.of("status",target,"version",version));
        if (field!=null) body.put(field,value);
        return json(call("PATCH","/api/v1/tickets/"+id+"/status",token,body,200));
    }
    @Test void registrationCredentialsTokensAndDeactivation() throws Exception {
        var account=Map.of("name","New Student","email","NEW@test.example","password",PASSWORD);
        JsonNode registered=json(call("POST","/api/v1/auth/register",null,account,201));
        assertThat(registered.get("role").asText()).isEqualTo("STUDENT");
        assertThat(registered.get("email").asText()).isEqualTo("new@test.example");
        call("POST","/api/v1/auth/register",null,account,409);
        call("POST","/api/v1/auth/register",null,Map.of("name","Bad","email","bad@test.example","password",PASSWORD,"role","ADMIN"),400);
        call("POST","/api/v1/auth/register",null,Map.of("name","Bad","email","bad@test.example","password",PASSWORD,"active",true),400);
        JsonNode wrong=json(call("POST","/api/v1/auth/login",null,Map.of("email","student@test.example","password","incorrect"),401));
        JsonNode unknown=json(call("POST","/api/v1/auth/login",null,Map.of("email","absent@test.example","password","incorrect"),401));
        assertThat(wrong.get("detail")).isEqualTo(unknown.get("detail"));
        call("GET","/api/v1/users/me","invalid.token.value",null,401);
        var claims=JwtClaimsSet.builder().issuer("campusdesk").audience(List.of("campusdesk-api")).subject(""+studentId)
            .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(Instant.now().minusSeconds(300)).build();
        String expired=jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        call("GET","/api/v1/users/me",expired,null,401);
        call("PATCH","/api/v1/admin/users/"+studentId+"/status",admin,Map.of("active",false,"version",0),200);
        call("GET","/api/v1/users/me",student,null,401);
        call("POST","/api/v1/auth/login",null,Map.of("email","student@test.example","password",PASSWORD),401);
    }
    @Test void permissionsHideTicketsAndScopeListCounts() throws Exception {
        JsonNode t=create("Library Wi-Fi"); long id=t.get("id").asLong();
        call("GET","/api/v1/tickets/"+id,other,null,404);
        call("GET","/api/v1/tickets/999999",other,null,404);
        call("POST","/api/v1/tickets/"+id+"/comments",other,Map.of("body","Intrusion","version",0),404);
        call("GET","/api/v1/tickets/"+id+"/events",staff,null,404);
        for (String token:List.of(student,staff)) call("GET","/api/v1/admin/statistics",token,null,403);
        assertThat(json(call("GET","/api/v1/tickets",other,null,200)).get("totalElements").asInt()).isZero();
        assertThat(json(call("GET","/api/v1/tickets",staff,null,200)).get("totalElements").asInt()).isZero();
        assign(id,staffId,0);
        assertThat(json(call("GET","/api/v1/tickets",staff,null,200)).get("totalElements").asInt()).isEqualTo(1);
        call("PATCH","/api/v1/tickets/"+id+"/assignment",student,Map.of("assigneeId",staffId,"version",1),403);
        call("PATCH","/api/v1/tickets/"+id+"/status",student,Map.of("status","IN_PROGRESS","version",1),403);
    }
    @Test void lifecycleAssignmentCommentsRollbackAndImmutableHistory() throws Exception {
        long id=create("Library Wi-Fi").get("id").asLong(); String path="/api/v1/tickets/"+id;
        call("PATCH",path+"/assignment",admin,Map.of("assigneeId",studentId,"version",0),400);
        JsonNode t=assign(id,staffId,0); assertThat(t.get("status").asText()).isEqualTo("OPEN");
        t=assign(id,secondStaffId,1); call("GET",path,staff,null,404);
        call("POST",path+"/comments",secondStaff,Map.of("body","Investigating","version",2),201);
        t=json(call("GET",path,student,null,200)); assertThat(t.get("version").asLong()).isEqualTo(3);
        call("PATCH",path+"/status",admin,Map.of("status","CLOSED","version",3),409);
        t=status(id,secondStaff,"IN_PROGRESS",3,null,null);
        call("PATCH",path+"/status",secondStaff,Map.of("status","RESOLVED","version",4),400);
        assertThat(json(call("GET",path+"/events",admin,null,200)).get("totalElements").asInt()).isEqualTo(5);
        t=status(id,secondStaff,"RESOLVED",4,"resolutionSummary","Replaced access point");
        call("PATCH",path+"/status",student,Map.of("status","OPEN","version",5),400);
        t=status(id,student,"OPEN",5,"reason","Connection still drops");
        assertThat(t.get("assignee").get("id").asLong()).isEqualTo(secondStaffId);
        assertThat(t.get("resolvedAt").isNull()).isTrue();
        t=status(id,admin,"IN_PROGRESS",6,null,null);
        t=status(id,admin,"RESOLVED",7,"resolutionSummary","Cable replaced");
        t=status(id,student,"CLOSED",8,null,null);
        call("POST",path+"/comments",admin,Map.of("body","Too late","version",9),409);
        call("PATCH",path+"/assignment",admin,Map.of("assigneeId",staffId,"version",9),409);
        JsonNode history=json(call("GET",path+"/events?size=100",student,null,200));
        assertThat(history.get("totalElements").asInt()).isEqualTo(10);
        assertThat(history.get("content").get(6).get("eventType").asText()).isEqualTo("REOPENED");
        assertThatThrownBy(() -> jdbc.update("UPDATE ticket_events SET new_value='tampered' WHERE ticket_id=?",id)).hasMessageContaining("append-only");
    }
    @Test void filtersValidationAndInactiveEntities() throws Exception {
        create("Library 100% Wi-Fi"); create("Hostel fan");
        JsonNode page=json(call("GET","/api/v1/tickets?title=100%25&status=OPEN&priority=HIGH&categoryId="+categoryId+"&size=1&sort=updatedAt&direction=asc",student,null,200));
        assertThat(page.get("totalElements").asInt()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("title").asText()).isEqualTo("Library 100% Wi-Fi");
        call("GET","/api/v1/tickets?size=101",student,null,400);
        call("GET","/api/v1/tickets?sort=passwordHash",student,null,400);
        call("GET","/api/v1/tickets?status=INVALID",student,null,400);
        call("GET","/api/v1/tickets?assigneeId="+staffId,student,null,403);
        call("POST","/api/v1/tickets",student,Map.of("title","","description","Valid","categoryId",categoryId,"priority","HIGH"),400);
        call("PATCH","/api/v1/categories/"+categoryId,admin,Map.of("name","IT","description","Computing services","active",false,"version",0),200);
        call("POST","/api/v1/tickets",student,Map.of("title","New request","description","Valid","categoryId",categoryId,"priority","HIGH"),400);
        assertThat(json(call("GET","/api/v1/categories",student,null,200)).get("totalElements").asInt()).isZero();
        call("PATCH","/api/v1/admin/users/"+staffId+"/status",admin,Map.of("active",false,"version",0),200);
        call("PATCH","/api/v1/tickets/1/assignment",admin,Map.of("assigneeId",staffId,"version",0),400);
        assertThat(json(call("GET","/api/v1/tickets/1/events",admin,null,200)).get("totalElements").asInt()).isEqualTo(1);
    }
    @Test void concurrentDatabaseUpdatesAndStaleApiWritesConflict() throws Exception {
        long id=create("Concurrent ticket").get("id").asLong();
        var loaded=new CountDownLatch(2); var release=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        Callable<Boolean> update=() -> {
            EntityManager em=emf.createEntityManager();
            try {
                em.getTransaction().begin(); Ticket t=em.find(Ticket.class,id); loaded.countDown();
                if (!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                t.touch(); em.getTransaction().commit(); return true;
            } catch (OptimisticLockException | RollbackException ex) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback(); return false;
            } finally { em.close(); }
        };
        try {
            Future<Boolean> a=pool.submit(update),b=pool.submit(update);
            assertThat(loaded.await(10,TimeUnit.SECONDS)).isTrue(); release.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        } finally { release.countDown(); pool.shutdownNow(); }
        JsonNode conflict=json(call("PATCH","/api/v1/tickets/"+id+"/assignment",admin,Map.of("assigneeId",staffId,"version",0),409));
        assertThat(conflict.get("code").asText()).isEqualTo("VERSION_CONFLICT");
        assertThat(json(call("GET","/api/v1/tickets/"+id+"/events",admin,null,200)).get("totalElements").asInt()).isEqualTo(1);
    }
}
