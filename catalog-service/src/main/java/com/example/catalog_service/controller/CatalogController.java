package com.example.catalog_service.controller;

import com.example.catalog_service.jpa.CatalogEntity;
import com.example.catalog_service.service.CatalogService;
import com.example.catalog_service.vo.ResponseCatalog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/catalog-service") // 이 컨트롤러의 기본 URL 경로(prefix)
public class CatalogController {
   // Spring 환경설정 정보를 주입받는 객체 (application.yml 등의 설정 값 접근 가능)
    Environment env;
    CatalogService catalogService;

    /**
     * 생성자 주입 방식 (Autowired)
     * - 스프링이 CatalogController를 생성할 때 Environment와 CatalogService Bean을 자동 주입
     */
    @Autowired
    public CatalogController(Environment env, CatalogService catalogService) {
        this.env = env;
        this.catalogService = catalogService;
    }

    /**
     * 서비스 상태 확인용 API (Health Check)
     * - 로컬 포트(local.server.port)와 실제 서버 포트(server.port) 정보를 함께 출력
     */
    @GetMapping("/health-check")
    public String status() {
        return String.format("It's Working in Catalog Service on LOCAL PORT %s (SERVER PORT %s)",
                env.getProperty("local.server.port"),
                env.getProperty("server.port"));
    }

    /**
     * 전체 카탈로그 목록 조회 API
     * - GET /catalog-service/catalogs
     */
    @GetMapping("/catalogs")
    public ResponseEntity<List<ResponseCatalog>> getCatalogs() {
        // DB로부터 전체 Catalog 목록 조회 (JPA Entity 형태)
        Iterable<CatalogEntity> catalogList = catalogService.getAllCatalogs();

        // 응답용 DTO 리스트 생성
        List<ResponseCatalog> result = new ArrayList<>();
        // ModelMapper를 사용하여 Entity → VO 변환 수행
        catalogList.forEach(v -> {
            result.add(new ModelMapper().map(v, ResponseCatalog.class));
        });

        // HTTP 200(OK) 상태코드와 함께 JSON 배열 형태로 응답
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
