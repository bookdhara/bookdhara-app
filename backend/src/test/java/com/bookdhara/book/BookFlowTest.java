package com.bookdhara.book;

import com.bookdhara.TestLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookFlowTest {

    private static final String ATOMIC_HABITS = """
            {"isbn":"978-1-84794-183-1","title":"Atomic Habits","author":"James Clear","mrp":399,"price":160,
             "condition":"GOOD","note":"Read once","city":"Mumbai","pincode":"400053",
             "allowsPickup":true,"allowsShipping":true,"payout":"CREDITS"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private IsbnLookup isbnLookup;

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
    void sellerListsBookAndBuyerFindsIt() throws Exception {
        long id = createBook(seller, ATOMIC_HABITS);

        // The seller sees what they'll get: 160 as credits = 200 (25% bonus).
        mvc.perform(get("/api/books/" + id).header("Authorization", seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.isbn").value("9781847941831"))
                .andExpect(jsonPath("$.payoutAmount").value(200));

        // A buyer sees it on Home, in search, and in detail without the seller's payout.
        mvc.perform(get("/api/books").header("Authorization", buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Atomic Habits"));
        mvc.perform(get("/api/books").param("q", "clear").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/books").param("q", "9781847941831").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/books").param("q", "dune").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/books").param("city", "mumbai").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/books").param("city", "Pune").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/books/" + id).header("Authorization", buyer))
                .andExpect(jsonPath("$.mine").value(false))
                .andExpect(jsonPath("$.seller.name").value("Riya"))
                .andExpect(jsonPath("$.payoutAmount").doesNotExist());

        // Sellers don't see their own books on Home.
        mvc.perform(get("/api/books").header("Authorization", seller))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/me/books").header("Authorization", seller))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void homeFiltersAndSorting() throws Exception {
        createBook(seller, ATOMIC_HABITS);
        createBook(seller, ATOMIC_HABITS.replace("Atomic Habits", "Dune").replace("\"price\":160", "\"price\":90")
                .replace("GOOD", "LIKE_NEW").replace("Mumbai", "Pune").replace("978-1-84794-183-1", ""));

        mvc.perform(get("/api/books").param("condition", "LIKE_NEW").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Dune"));
        mvc.perform(get("/api/books").param("maxPrice", "100").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Dune"));
        mvc.perform(get("/api/books").param("sort", "PRICE_LOW").header("Authorization", buyer))
                .andExpect(jsonPath("$[0].title").value("Dune"))
                .andExpect(jsonPath("$[1].title").value("Atomic Habits"));
        mvc.perform(get("/api/books").param("sort", "PRICE_HIGH").header("Authorization", buyer))
                .andExpect(jsonPath("$[0].title").value("Atomic Habits"));
        mvc.perform(get("/api/books/cities").header("Authorization", buyer))
                .andExpect(jsonPath("$[0]").value("Mumbai"))
                .andExpect(jsonPath("$[1]").value("Pune"));
        mvc.perform(get("/api/books").param("condition", "SHINY").header("Authorization", buyer))
                .andExpect(status().isBadRequest());
    }

    @Test
    void photosCanBeAddedByTheSellerAndViewedPublicly() throws Exception {
        long id = createBook(seller, ATOMIC_HABITS);
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};

        String body = mvc.perform(multipart("/api/books/" + id + "/photos")
                        .file(new MockMultipartFile("file", "cover.jpg", "image/jpeg", jpeg))
                        .header("Authorization", seller))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String url = json.readTree(body).get("url").asText();

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(jpeg));

        mvc.perform(get("/api/books").header("Authorization", buyer))
                .andExpect(jsonPath("$[0].coverPhotoUrl").value(url));

        // Others can't add photos; only images; at most 3.
        mvc.perform(multipart("/api/books/" + id + "/photos")
                        .file(new MockMultipartFile("file", "x.jpg", "image/jpeg", jpeg))
                        .header("Authorization", buyer))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/books/" + id + "/photos")
                        .file(new MockMultipartFile("file", "x.pdf", "application/pdf", jpeg))
                        .header("Authorization", seller))
                .andExpect(status().isBadRequest());
        for (int i = 0; i < 2; i++) {
            mvc.perform(multipart("/api/books/" + id + "/photos")
                            .file(new MockMultipartFile("file", "x.jpg", "image/jpeg", jpeg))
                            .header("Authorization", seller))
                    .andExpect(status().isCreated());
        }
        mvc.perform(multipart("/api/books/" + id + "/photos")
                        .file(new MockMultipartFile("file", "x.jpg", "image/jpeg", jpeg))
                        .header("Authorization", seller))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PHOTO_LIMIT"));
    }

    @Test
    void sellerCanRemoveListingButOthersCannot() throws Exception {
        long id = createBook(seller, ATOMIC_HABITS);

        mvc.perform(delete("/api/books/" + id).header("Authorization", buyer))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/books/" + id).header("Authorization", seller))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/books").header("Authorization", buyer))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/books/" + id).header("Authorization", buyer))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidListingsAreRejected() throws Exception {
        String noDelivery = ATOMIC_HABITS.replace("\"allowsPickup\":true,\"allowsShipping\":true",
                "\"allowsPickup\":false,\"allowsShipping\":false");
        mvc.perform(post("/api/books").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content(noDelivery))
                .andExpect(status().isBadRequest());

        String badPincode = ATOMIC_HABITS.replace("400053", "4000");
        mvc.perform(post("/api/books").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content(badPincode))
                .andExpect(status().isBadRequest());

        String badIsbn = ATOMIC_HABITS.replace("978-1-84794-183-1", "12345");
        mvc.perform(post("/api/books").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content(badIsbn))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ISBN"));

        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(ATOMIC_HABITS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void isbnLookupReturnsDetailsOrNotFound() throws Exception {
        when(isbnLookup.find(anyString())).thenAnswer(inv ->
                "9781847941831".equals(inv.getArgument(0))
                        ? Optional.of(new IsbnLookup.BookInfo("9781847941831", "Atomic Habits", "James Clear", null))
                        : Optional.empty());

        mvc.perform(get("/api/isbn/978-1847941831").header("Authorization", seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Atomic Habits"))
                .andExpect(jsonPath("$.author").value("James Clear"));
        mvc.perform(get("/api/isbn/9780000000002").header("Authorization", seller))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ISBN_NOT_FOUND"));
    }

    private long createBook(String token, String body) throws Exception {
        String response = mvc.perform(post("/api/books").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
}
