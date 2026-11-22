package com.bookdhara.wishlist;

import com.bookdhara.TestLogin;
import com.bookdhara.notify.EmailNotifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WishlistAndAlertsTest {

    private static final String BOOK = """
            {"title":"%s","author":"%s","isbn":%s,"price":150,"condition":"GOOD","city":"%s","pincode":"400053",
             "allowsPickup":true,"allowsShipping":false,"payout":"CASH"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private EmailNotifier notifier;

    private String seller;
    private String buyer;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM wishlist_items");
        jdbc.update("DELETE FROM book_alerts");
        jdbc.update("DELETE FROM reports");
        jdbc.update("DELETE FROM user_blocks");
        jdbc.update("DELETE FROM credit_entries");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM book_photos");
        jdbc.update("DELETE FROM books");
        jdbc.update("DELETE FROM otp_codes");
        jdbc.update("DELETE FROM users");
        seller = TestLogin.login(mvc, json, "riya@example.com", "Riya");
        buyer = TestLogin.login(mvc, json, "aman@example.com", "Aman");
    }

    @Test
    void savingBooks() throws Exception {
        long a = list("Atomic Habits", "James Clear", null, "Mumbai");
        long b = list("Ikigai", "Hector Garcia", null, "Mumbai");

        mvc.perform(put("/api/me/wishlist/" + a).header("Authorization", buyer)).andExpect(status().isNoContent());
        mvc.perform(put("/api/me/wishlist/" + b).header("Authorization", buyer)).andExpect(status().isNoContent());
        mvc.perform(put("/api/me/wishlist/" + b).header("Authorization", buyer)).andExpect(status().isNoContent());

        mvc.perform(get("/api/me/wishlist").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Ikigai"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
        mvc.perform(get("/api/books/" + a).header("Authorization", buyer)).andExpect(jsonPath("$.saved").value(true));
        mvc.perform(get("/api/books/" + a).header("Authorization", seller)).andExpect(jsonPath("$.saved").value(false));

        mvc.perform(delete("/api/me/wishlist/" + b).header("Authorization", buyer)).andExpect(status().isNoContent());
        // A removed listing drops out of the saved list.
        mvc.perform(delete("/api/books/" + a).header("Authorization", seller)).andExpect(status().is2xxSuccessful());
        mvc.perform(get("/api/me/wishlist").header("Authorization", buyer)).andExpect(jsonPath("$.length()").value(0));

        mvc.perform(put("/api/me/wishlist/999999").header("Authorization", buyer)).andExpect(status().isNotFound());
    }

    @Test
    void managingAlerts() throws Exception {
        long id = read(alert(buyer, "  Atomic   Habits ", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.query").value("Atomic Habits"))).get("id").asLong();
        alert(buyer, "atomic habits", "Pune").andExpect(status().isConflict());
        alert(buyer, "ab", null).andExpect(status().isBadRequest());

        mvc.perform(get("/api/me/alerts").header("Authorization", buyer)).andExpect(jsonPath("$.length()").value(1));
        // Someone else can't delete my alert.
        mvc.perform(delete("/api/me/alerts/" + id).header("Authorization", seller)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/alerts").header("Authorization", buyer)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/me/alerts/" + id).header("Authorization", buyer)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/alerts").header("Authorization", buyer)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void matchingListingsEmailOncePerHour() throws Exception {
        alert(buyer, "james clear", null).andExpect(status().isCreated());
        alert(seller, "atomic", null).andExpect(status().isCreated());

        long bookId = list("Atomic Habits", "James Clear", null, "Mumbai");

        verify(notifier, timeout(3000)).send(eq("aman@example.com"), contains("\"Atomic Habits\" is on BookDhara for ₹150"),
                contains("?book=" + bookId), contains("Atomic Habits"));
        // The seller's own alert doesn't fire for their own book.
        verify(notifier, after(500).never()).send(eq("riya@example.com"), contains("is on BookDhara"), anyString(), anyString());

        list("Atomic Habits (2nd copy)", "James Clear", null, "Mumbai");
        verify(notifier, after(800).times(1)).send(eq("aman@example.com"), contains("is on BookDhara"), anyString(), anyString());
    }

    @Test
    void cityAndIsbnAlerts() throws Exception {
        alert(buyer, "Ikigai", "Pune").andExpect(status().isCreated());
        alert(buyer, "9781847941831", null).andExpect(status().isCreated());

        list("Ikigai", "Hector Garcia", null, "Mumbai");
        verify(notifier, after(800).never()).send(eq("aman@example.com"), contains("Ikigai"), anyString(), anyString());

        list("Some Other Title", null, "\"9781847941831\"", "Mumbai");
        verify(notifier, timeout(3000)).send(eq("aman@example.com"), contains("Some Other Title"), anyString(), anyString());
    }

    private long list(String title, String author, String isbnJson, String city) throws Exception {
        String body = BOOK.formatted(title, author == null ? "" : author, isbnJson == null ? "null" : isbnJson, city);
        return read(mvc.perform(post("/api/books").header("Authorization", seller)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions alert(String token, String query, String city) throws Exception {
        String body = "{\"query\":\"" + query + "\"" + (city == null ? "" : ",\"city\":\"" + city + "\"") + "}";
        return mvc.perform(post("/api/me/alerts").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
