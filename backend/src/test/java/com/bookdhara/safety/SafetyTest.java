package com.bookdhara.safety;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "bookdhara.admin.emails=team@example.com, second@example.com")
@AutoConfigureMockMvc
class SafetyTest {

    private static final String BOOK = """
            {"title":"%s","price":150,"condition":"GOOD","city":"Mumbai","pincode":"400053",
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
    private long sellerId;
    private long buyerId;

    @BeforeEach
    void setUp() throws Exception {
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
        sellerId = me(seller).get("id").asLong();
        buyerId = me(buyer).get("id").asLong();
    }

    @Test
    void blockingHidesListingsAndStopsBuyingBothWays() throws Exception {
        long bookId = listBook("Ikigai");

        mvc.perform(put("/api/me/blocks/" + sellerId).header("Authorization", buyer)).andExpect(status().isNoContent());
        mvc.perform(put("/api/me/blocks/" + sellerId).header("Authorization", buyer)).andExpect(status().isNoContent());

        mvc.perform(get("/api/books").header("Authorization", buyer)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/books/" + bookId).header("Authorization", buyer)).andExpect(status().isNotFound());
        placeOrder(buyer, bookId).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("BLOCKED"));
        mvc.perform(get("/api/me/blocks").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(sellerId))
                .andExpect(jsonPath("$[0].name").value("Riya"));

        // The blocked seller can't see the blocker's listings either.
        String buyerBook = BOOK.formatted("Deep Work");
        long otherBook = read(mvc.perform(post("/api/books").header("Authorization", buyer)
                .contentType(MediaType.APPLICATION_JSON).content(buyerBook)).andExpect(status().isCreated())).get("id").asLong();
        mvc.perform(get("/api/books/" + otherBook).header("Authorization", seller)).andExpect(status().isNotFound());

        mvc.perform(delete("/api/me/blocks/" + sellerId).header("Authorization", buyer)).andExpect(status().isNoContent());
        mvc.perform(get("/api/books").header("Authorization", buyer)).andExpect(jsonPath("$.length()").value(1));
        placeOrder(buyer, bookId).andExpect(status().isCreated());

        mvc.perform(put("/api/me/blocks/" + buyerId).header("Authorization", buyer)).andExpect(status().isBadRequest());
    }

    @Test
    void blockingStopsChatOnAnExistingOrder() throws Exception {
        long orderId = read(placeOrder(buyer, listBook("Ikigai")).andExpect(status().isCreated())).get("id").asLong();
        mvc.perform(put("/api/me/blocks/" + buyerId).header("Authorization", seller)).andExpect(status().isNoContent());

        mvc.perform(post("/api/orders/" + orderId + "/messages").header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"hello?\"}"))
                .andExpect(status().isForbidden());
        // The order itself can still be cancelled.
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", buyer)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test
    void reportsAreSavedOnceAndTheTeamIsEmailed() throws Exception {
        long bookId = listBook("Fake <b>book</b>");

        report(buyer, "BOOK", bookId, "FAKE_LISTING", "Photo is from Google").andExpect(status().isNoContent());
        report(buyer, "BOOK", bookId, "SPAM", null).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reports", Long.class)).isEqualTo(1);

        verify(notifier, timeout(3000)).send(eq("team@example.com"), contains("FAKE_LISTING"),
                contains("Photo is from Google"), contains("Fake &lt;b&gt;book&lt;/b&gt;"));
        verify(notifier, timeout(3000)).send(eq("second@example.com"), anyString(), anyString(), anyString());

        report(buyer, "USER", sellerId, "NO_SHOW", "Didn't come to the meetup").andExpect(status().isNoContent());
        report(seller, "BOOK", bookId, "SPAM", null).andExpect(status().isBadRequest());
        report(seller, "USER", sellerId, "SPAM", null).andExpect(status().isBadRequest());
        report(buyer, "BOOK", 999999, "SPAM", null).andExpect(status().isNotFound());
        report(buyer, "BOOK", bookId, "NOT_A_REASON", null).andExpect(status().isBadRequest());
    }

    @Test
    void deletingAnAccountRemovesItsDataAndEndsSessions() throws Exception {
        long sold = listBook("Ikigai");
        long forSale = listBook("Deep Work");
        long orderId = read(placeOrder(buyer, sold).andExpect(status().isCreated())).get("id").asLong();

        mvc.perform(delete("/api/me").header("Authorization", seller))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPEN_ORDERS"));

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", buyer)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
        mvc.perform(delete("/api/me").header("Authorization", seller)).andExpect(status().isNoContent());

        // Old session stops working at once.
        mvc.perform(get("/api/me").header("Authorization", seller)).andExpect(status().isUnauthorized());
        // Listings are gone.
        mvc.perform(get("/api/books").header("Authorization", buyer)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/books/" + forSale).header("Authorization", buyer)).andExpect(status().isNotFound());
        // Personal data is gone; the buyer's order history still works.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = 'riya@example.com'", Long.class)).isZero();
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerName").value("Deleted user"));

        // The same email can sign up again as a brand-new account.
        String again = TestLogin.login(mvc, json, "riya@example.com", "Riya Again");
        assertThat(me(again).get("id").asLong()).isNotEqualTo(sellerId);
    }

    private long listBook(String title) throws Exception {
        return read(mvc.perform(post("/api/books").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK.formatted(title)))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions placeOrder(String token, long bookId) throws Exception {
        return mvc.perform(post("/api/orders").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + bookId + ",\"method\":\"PICKUP\",\"creditsToUse\":0}"));
    }

    private ResultActions report(String token, String type, long id, String reason, String details) throws Exception {
        String body = "{\"type\":\"" + type + "\",\"targetId\":" + id + ",\"reason\":\"" + reason + "\""
                + (details == null ? "" : ",\"details\":\"" + details + "\"") + "}";
        return mvc.perform(post("/api/reports").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode me(String token) throws Exception {
        return read(mvc.perform(get("/api/me").header("Authorization", token)).andExpect(status().isOk()));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
