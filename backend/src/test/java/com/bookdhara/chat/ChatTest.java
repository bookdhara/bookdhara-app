package com.bookdhara.chat;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatTest {

    private static final String BOOK = """
            {"title":"Ikigai","price":150,"condition":"GOOD","city":"Mumbai","pincode":"400053",
             "allowsPickup":true,"allowsShipping":true,"payout":"CASH"}
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
    private String stranger;
    private long orderId;

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
        stranger = TestLogin.login(mvc, json, "zed@example.com", "Zed");

        long bookId = read(mvc.perform(post("/api/books").header("Authorization", seller)
                .contentType(MediaType.APPLICATION_JSON).content(BOOK)).andExpect(status().isCreated())).get("id").asLong();
        orderId = read(mvc.perform(post("/api/orders").header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":" + bookId + ",\"method\":\"PICKUP\",\"creditsToUse\":0}"))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    @Test
    void buyerAndSellerChatAndUnreadCountsFollow() throws Exception {
        long first = read(send(buyer, "Hi! Can we meet at Andheri station, 6 pm Saturday?")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))).get("id").asLong();

        // The seller sees one unread message, on the order and in their alerts.
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", seller))
                .andExpect(jsonPath("$.unreadMessages").value(1));
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", buyer))
                .andExpect(jsonPath("$.unreadMessages").value(0));

        chat(seller, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherName").value("Aman"))
                .andExpect(jsonPath("$.bookTitle").value("Ikigai"))
                .andExpect(jsonPath("$.canSend").value(true))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].mine").value(false));
        // Reading cleared it.
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", seller))
                .andExpect(jsonPath("$.unreadMessages").value(0));

        send(seller, "Sure, see you at the east exit 👋").andExpect(status().isCreated());

        // The buyer is alerted to the reply, even though as a buyer of a pickup order they have nothing else to do.
        mvc.perform(get("/api/orders/attention").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].unreadMessages").value(1));

        // Only new messages after a given id.
        chat(buyer, first)
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].body").value("Sure, see you at the east exit 👋"));
        mvc.perform(get("/api/orders/attention").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void outsidersCantReadOrWrite() throws Exception {
        send(buyer, "private plans").andExpect(status().isCreated());
        chat(stranger, 0).andExpect(status().isNotFound());
        send(stranger, "hello").andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/" + orderId + "/messages")).andExpect(status().isUnauthorized());
    }

    @Test
    void closedOrdersAreReadOnly() throws Exception {
        send(buyer, "Changed my mind, sorry").andExpect(status().isCreated());
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", buyer)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

        send(seller, "No problem").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CHAT_CLOSED"));
        chat(seller, 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.canSend").value(false))
                .andExpect(jsonPath("$.messages.length()").value(1));
    }

    @Test
    void emailsTheOtherPersonOnceNotForEveryMessage() throws Exception {
        send(buyer, "Hi, is 6 pm ok?").andExpect(status().isCreated());
        send(buyer, "Or 7 pm?").andExpect(status().isCreated());

        verify(notifier, timeout(3000)).send(eq("riya@example.com"), contains("Aman sent you a message about Ikigai"),
                contains("Hi, is 6 pm ok?"), anyString());
        verify(notifier, after(500).times(1)).send(eq("riya@example.com"), contains("sent you a message"),
                anyString(), anyString());
    }

    @Test
    void messagesAreCleanedAndRateLimited() throws Exception {
        send(buyer, "   ").andExpect(status().isBadRequest());
        send(buyer, "\u0007\u0000").andExpect(status().isBadRequest());
        assertThat(ChatService.clean("a\r\n\n\n\n\nb\u0007")).isEqualTo("a\n\nb");

        for (int i = 0; i < ChatService.MAX_PER_MINUTE; i++) {
            send(buyer, "msg " + i).andExpect(status().isCreated());
        }
        send(buyer, "one too many").andExpect(status().isTooManyRequests());
    }

    private ResultActions send(String token, String body) throws Exception {
        return mvc.perform(post("/api/orders/" + orderId + "/messages").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("body", body))));
    }

    private ResultActions chat(String token, long after) throws Exception {
        return mvc.perform(get("/api/orders/" + orderId + "/messages?after=" + after).header("Authorization", token));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
