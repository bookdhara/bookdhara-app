package com.bookdhara.order;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowTest {

    private static final String BOOK = """
            {"title":"%s","price":%d,"condition":"GOOD","city":"Mumbai","pincode":"400053",
             "allowsPickup":true,"allowsShipping":true,"payout":"%s"}
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
    void pickupOrderCompletesWithHandoverCodeAndPaysSellerInCredits() throws Exception {
        long bookId = listBook(seller, "Atomic Habits", 160, "CREDITS");

        JsonNode order = read(placeOrder(buyer, bookId, "PICKUP", 0, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.cashPaid").value(165))      // 160 + ₹5 fee, no delivery
                .andExpect(jsonPath("$.paymentMode").value("TEST"))
                .andExpect(jsonPath("$.myRole").value("BUYER"))
                .andExpect(jsonPath("$.sellerPayoutAmount").doesNotExist()));
        long orderId = order.get("id").asLong();
        String code = order.get("handoverCode").asText();

        // The book is reserved: off Home, and can't be removed by the seller.
        mvc.perform(get("/api/books").header("Authorization", buyer)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/books/" + bookId).header("Authorization", seller)).andExpect(status().isConflict());

        // The seller sees what they'll get, but never the code.
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", seller))
                .andExpect(jsonPath("$.handoverCode").doesNotExist())
                .andExpect(jsonPath("$.sellerPayoutAmount").value(200))
                .andExpect(jsonPath("$.actions.canEnterHandoverCode").value(true));
        mvc.perform(get("/api/wallet").header("Authorization", seller))
                .andExpect(jsonPath("$.creditsOnHold").value(200));

        String wrong = code.equals("0000") ? "1111" : "0000";
        handover(seller, orderId, wrong).andExpect(status().isBadRequest());
        handover(buyer, orderId, code).andExpect(status().isForbidden());
        handover(seller, orderId, code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/wallet").header("Authorization", seller))
                .andExpect(jsonPath("$.credits").value(205))       // 200 sale + 5 pickup bonus
                .andExpect(jsonPath("$.creditsOnHold").value(0));
        mvc.perform(get("/api/wallet").header("Authorization", buyer))
                .andExpect(jsonPath("$.credits").value(5));
        mvc.perform(get("/api/books/" + bookId).header("Authorization", seller))
                .andExpect(jsonPath("$.status").value("SOLD"));
        mvc.perform(get("/api/orders").param("role", "selling").header("Authorization", seller))
                .andExpect(jsonPath("$[0].id").value(orderId));
    }

    @Test
    void creditsPayPartOfTheBookAndAreRefundedOnCancel() throws Exception {
        // The buyer earns 205 credits by selling a book first.
        long first = listBook(buyer, "Dune", 160, "CREDITS");
        String code = read(placeOrder(seller, first, "PICKUP", 0, null)).get("handoverCode").asText();
        long firstOrder = read(mvc.perform(get("/api/orders").header("Authorization", seller))).get(0).get("id").asLong();
        handover(buyer, firstOrder, code).andExpect(status().isOk());

        long bookId = listBook(seller, "Sapiens", 300, "CASH");
        mvc.perform(get("/api/orders/quote").param("bookId", "" + bookId).param("method", "SHIPPING")
                        .param("credits", "999").header("Authorization", buyer))
                .andExpect(jsonPath("$.maxCredits").value(205))
                .andExpect(jsonPath("$.cashToPay").value(300 - 205 + 60 + 15));

        placeOrder(buyer, bookId, "SHIPPING", 300, ADDRESS).andExpect(status().isBadRequest()); // more than balance
        placeOrder(buyer, bookId, "SHIPPING", 100, null).andExpect(status().isBadRequest());    // no address
        long orderId = read(placeOrder(buyer, bookId, "SHIPPING", 100, ADDRESS)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cashPaid").value(300 - 100 + 60 + 15))
                .andExpect(jsonPath("$.shippingAddress.phone").value("+919876500002"))).get("id").asLong();

        mvc.perform(get("/api/wallet").header("Authorization", buyer)).andExpect(jsonPath("$.credits").value(105));

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Changed my mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/wallet").header("Authorization", buyer)).andExpect(jsonPath("$.credits").value(205));
        mvc.perform(get("/api/books/" + bookId).header("Authorization", buyer))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void deliveryOrderShipsThenBuyerConfirms() throws Exception {
        long bookId = listBook(seller, "Ikigai", 120, "CASH");
        long orderId = read(placeOrder(buyer, bookId, "SHIPPING", 0, ADDRESS)).get("id").asLong();

        // Someone else can't see or buy it.
        String stranger = TestLogin.login(mvc, json, "neha@example.com", "Neha");
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", stranger)).andExpect(status().isNotFound());
        placeOrder(stranger, bookId, "PICKUP", 0, null).andExpect(status().isConflict());

        mvc.perform(post("/api/orders/" + orderId + "/confirm-received").header("Authorization", seller))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/orders/" + orderId + "/ship").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"trackingNote\":\"Delhivery 12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", buyer))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/orders/" + orderId + "/confirm-received").header("Authorization", buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/wallet").header("Authorization", seller))
                .andExpect(jsonPath("$.cashEarned").value(120))
                .andExpect(jsonPath("$.credits").value(0));
    }

    @Test
    void cantBuyOwnBookOrUnofferedDelivery() throws Exception {
        long bookId = listBook(seller, "Ikigai", 120, "CASH");
        placeOrder(seller, bookId, "PICKUP", 0, null).andExpect(status().isBadRequest());

        String pickupOnly = BOOK.formatted("Gita", 50, "CASH").replace("\"allowsShipping\":true", "\"allowsShipping\":false");
        long other = read(mvc.perform(post("/api/books").header("Authorization", seller)
                .contentType(MediaType.APPLICATION_JSON).content(pickupOnly))).get("id").asLong();
        placeOrder(buyer, other, "SHIPPING", 0, ADDRESS)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SHIPPING_NOT_OFFERED"));
    }

    private long listBook(String token, String title, int price, String payout) throws Exception {
        return read(mvc.perform(post("/api/books").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK.formatted(title, price, payout)))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions placeOrder(String token, long bookId, String method, int credits, String address) throws Exception {
        String body = "{\"bookId\":" + bookId + ",\"method\":\"" + method + "\",\"creditsToUse\":" + credits
                + (address == null ? "" : ",\"address\":" + address) + "}";
        return mvc.perform(post("/api/orders").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions handover(String token, long orderId, String code) throws Exception {
        return mvc.perform(post("/api/orders/" + orderId + "/handover").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + code + "\"}"));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
