package com.bookdhara.notify;

import com.bookdhara.TestLogin;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderNotificationTest {

    private static final String BOOK = """
            {"title":"%s","price":150,"condition":"GOOD","city":"Mumbai","pincode":"400053",
             "allowsPickup":true,"allowsShipping":true,"payout":"CASH"}
            """;
    private static final String ADDRESS = """
            {"name":"Aman K","phone":"9876500002","address":"12 MG Road","city":"Pune","pincode":"411001"}
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
    void sellerHearsAboutANewOrderAndSeesItUntilHandedOver() throws Exception {
        long bookId = listBook("Atomic Habits");
        JsonNode order = read(placeOrder(bookId, "PICKUP", null).andExpect(status().isCreated()));

        verify(notifier, timeout(3000)).send(eq("riya@example.com"), contains("Your book sold: Atomic Habits"),
                contains("Aman bought \"Atomic Habits\""), contains("Hi Riya,"));

        mvc.perform(get("/api/orders/attention").header("Authorization", seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(order.get("id").asLong()))
                .andExpect(jsonPath("$[0].actions.canEnterHandoverCode").value(true))
                .andExpect(jsonPath("$[0].handoverCode").doesNotExist());
        mvc.perform(get("/api/orders/attention").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(post("/api/orders/" + order.get("id").asLong() + "/handover").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + order.get("handoverCode").asText() + "\"}"))
                .andExpect(status().isOk());

        verify(notifier, timeout(3000)).send(eq("riya@example.com"), contains("Sale complete"), anyString(), anyString());
        mvc.perform(get("/api/orders/attention").header("Authorization", seller))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buyerHearsWhenShippedAndMustConfirm() throws Exception {
        long bookId = listBook("Deep Work");
        long orderId = read(placeOrder(bookId, "SHIPPING", ADDRESS).andExpect(status().isCreated())).get("id").asLong();

        mvc.perform(post("/api/orders/" + orderId + "/ship").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"trackingNote\":\"DTDC 123\"}"))
                .andExpect(status().isOk());

        verify(notifier, timeout(3000)).send(eq("aman@example.com"), contains("on its way"),
                contains("Tracking: DTDC 123"), anyString());
        mvc.perform(get("/api/orders/attention").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].actions.canConfirmReceived").value(true));
        mvc.perform(get("/api/orders/attention").header("Authorization", seller))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theOtherPersonHearsAboutACancellation() throws Exception {
        long bookId = listBook("Ikigai");
        long orderId = read(placeOrder(bookId, "PICKUP", null)).get("id").asLong();

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Found it nearby\"}"))
                .andExpect(status().isOk());

        verify(notifier, timeout(3000)).send(eq("riya@example.com"), contains("Order cancelled"),
                contains("cancelled by Aman. Reason: Found it nearby."), anyString());
        verify(notifier, after(500).never()).send(eq("aman@example.com"), contains("Order cancelled"),
                anyString(), anyString());
    }

    @Test
    void bookTitlesCantInjectHtmlOrHeaders() throws Exception {
        long bookId = listBook("<b>Bold</b> \\r\\nBcc: x@evil.com");
        placeOrder(bookId, "PICKUP", null).andExpect(status().isCreated());

        verify(notifier, timeout(3000)).send(eq("riya@example.com"),
                org.mockito.ArgumentMatchers.argThat(subject -> !subject.contains("\n") && !subject.contains("\r")),
                anyString(),
                org.mockito.ArgumentMatchers.argThat(html -> html.contains("&lt;b&gt;Bold&lt;/b&gt;") && !html.contains("<b>Bold")));
        verify(notifier, never()).send(eq("x@evil.com"), anyString(), anyString(), anyString());
    }

    private long listBook(String title) throws Exception {
        return read(mvc.perform(post("/api/books").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK.formatted(title)))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions placeOrder(long bookId, String method, String address) throws Exception {
        String body = "{\"bookId\":" + bookId + ",\"method\":\"" + method + "\",\"creditsToUse\":0"
                + (address == null ? "" : ",\"address\":" + address) + "}";
        return mvc.perform(post("/api/orders").header("Authorization", buyer)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
