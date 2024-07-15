package gift.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import gift.auth.JwtService;
import gift.auth.JwtTokenProvider;
import gift.model.Member;
import gift.model.Product;
import gift.repository.MemberRepository;
import gift.repository.ProductRepository;
import gift.request.ProductAddRequest;
import gift.request.ProductUpdateRequest;
import gift.response.ProductResponse;
import gift.service.MemberService;
import gift.service.ProductService;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductApiControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    Member member;
    String token;
    List<Product> savedProducts;

    @BeforeEach
    void before() {
        List<Product> products = new ArrayList<>();
        IntStream.range(0, 10)
            .forEach( i -> {
                products.add(new Product("product"+i, 1000, "https://a.com"));
            });
        savedProducts = productRepository.saveAll(products);
        member = memberService.join("aaa123@a.com", "1234");
        token = jwtTokenProvider.generateToken(member);
    }

    @AfterEach
    void after() {
        memberRepository.delete(member);
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("상품 조회 테스트")
    void getAllProducts() {
        //given
        String url = "http://localhost:" + port + "/api/products";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        RequestEntity<Void> request = new RequestEntity<>(
            headers, HttpMethod.GET, URI.create(url));

        //when
        ResponseEntity<List<ProductResponse>> response
            = restTemplate.exchange(request,
            new ParameterizedTypeReference<>() {
            });

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(10);
        IntStream.range(0, 10)
            .forEach( i -> {
                ProductResponse pr = response.getBody().get(i);
                assertThat(pr.id()).isEqualTo(savedProducts.get(i).getId());
                assertThat(pr.name()).isEqualTo(savedProducts.get(i).getName());
                assertThat(pr.price()).isEqualTo(savedProducts.get(i).getPrice());
                assertThat(pr.imageUrl()).isEqualTo(savedProducts.get(i).getImageUrl());
            });
    }

    @Test
    @DisplayName("상품 추가 테스트")
    void addProduct() {
        //given
        String url = "http://localhost:" + port + "/api/products";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ProductAddRequest productAddRequest = new ProductAddRequest("product11", 1500, "https://b.com");
        RequestEntity<ProductAddRequest> request = new RequestEntity<>(
            productAddRequest, headers, HttpMethod.POST, URI.create(url));

        //when
        ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("상품 변경 테스트")
    void updateProduct() {
        //given
        String url = "http://localhost:" + port + "/api/products";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        //기존 첫번째 상품을 변경한다.
        ProductUpdateRequest ProductUpdateRequest = new ProductUpdateRequest(savedProducts.get(0).getId(), "product11", 1500, "https://b.com");
        RequestEntity<ProductUpdateRequest> request = new RequestEntity<>(
            ProductUpdateRequest, headers, HttpMethod.PUT, URI.create(url));

        //when
        ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("상품 삭제 메소드")
    void deleteProduct() {
        //given
        URI uri = UriComponentsBuilder
            .fromUriString("http://localhost:" + port)
            .path("/api/products")
            .queryParam("id", savedProducts.get(0).getId())
            .encode()
            .build()
            .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        //기존 첫번째 상품을 삭제한다.
        RequestEntity<Void> request = new RequestEntity<>(
             headers, HttpMethod.DELETE, uri);

        //when
        ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }


}