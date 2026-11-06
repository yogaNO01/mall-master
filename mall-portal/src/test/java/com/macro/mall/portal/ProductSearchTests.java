package com.macro.mall.portal;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.portal.dao.PortalProductDao;
import com.macro.mall.portal.service.impl.PmsPortalProductServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductSearchTests {
    @AfterEach void clearPagination() { PageHelper.clearPage(); }

    @Test void normalizesTokensEscapesWildcardsAndBoundsPagination() {
        PortalProductDao dao = mock(PortalProductDao.class);
        PmsPortalProductServiceImpl service = new PmsPortalProductServiceImpl();
        ReflectionTestUtils.setField(service, "portalProductDao", dao);
        service.search("  爱马仕　鞋 爱马仕 100%_  ", "  衬衫 ", " CAT_= ", 58L, 57L, -1, 500, 99);
        verify(dao).searchProducts(List.of("%爱马仕%", "%鞋%", "%100=%=_%"), "%衬衫%", "%CAT=_==%", 58L, 57L, 0);
        assertEquals(1, PageHelper.getLocalPage().getPageNum());
        assertEquals(100, PageHelper.getLocalPage().getPageSize());
    }

    @Test void oldSignatureAndEmptyQueryRemainSupported() {
        PortalProductDao dao = mock(PortalProductDao.class);
        PmsPortalProductServiceImpl service = new PmsPortalProductServiceImpl();
        ReflectionTestUtils.setField(service, "portalProductDao", dao);
        service.search("   ", null, null, null, null, null);
        verify(dao).searchProducts(List.of(), null, null, null, null, 0);
        assertEquals(1, PageHelper.getLocalPage().getPageNum());
    }

    @Test void realMapperMatchesAllFieldsAndKeepsVisibilityFiltersAndPagination() throws Exception {
        String url = System.getenv("MALL_TEST_MYSQL_URL");
        Assumptions.assumeTrue(url != null, "Set local MySQL test connection to run mapper integration tests");
        Configuration configuration = new Configuration(new Environment("search-test", new JdbcTransactionFactory(),
                new UnpooledDataSource("com.mysql.cj.jdbc.Driver", url, System.getenv("MALL_TEST_MYSQL_USER"), System.getenv("MALL_TEST_MYSQL_PASSWORD"))));
        configuration.addInterceptor(new PageInterceptor());
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource resource : resolver.getResources("classpath*:com/macro/mall/mapper/*.xml")) {
            try (var input = resource.getInputStream()) {
                new XMLMapperBuilder(input, configuration, resource.toString(), configuration.getSqlFragments()).parse();
            }
        }
        Resource resource = resolver.getResource("classpath:dao/PortalProductDao.xml");
        try (var input = resource.getInputStream()) {
            new XMLMapperBuilder(input, configuration, resource.toString(), configuration.getSqlFragments()).parse();
        }
        try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession(false)) {
            try {
                String insert = "INSERT INTO pms_product (name,sub_title,keywords,product_sn,brand_name,product_category_name,brand_id,product_category_id,price,sale,sort,delete_status,publish_status) VALUES (?,?,?,?,?,?,987654,987654,?,0,0,?,?)";
                try (PreparedStatement statement = session.getConnection().prepareStatement(insert)) {
                    for (int i = 0; i < 4; i++) {
                        statement.setString(1, i == 1 ? "轻便板鞋" : "真丝衬衫");
                        statement.setString(2, "春季通勤");
                        statement.setString(3, i == 1 ? "都市 100%_" : "桑蚕丝");
                        statement.setString(4, "SEARCH-FIXTURE-" + i);
                        statement.setString(5, "测试品牌");
                        statement.setString(6, "测试分类");
                        statement.setInt(7, 100 + i * 20);
                        statement.setInt(8, i == 2 ? 1 : 0);
                        statement.setInt(9, i == 3 ? 0 : 1);
                        statement.executeUpdate();
                    }
                }
                PmsPortalProductServiceImpl service = new PmsPortalProductServiceImpl();
                ReflectionTestUtils.setField(service, "portalProductDao", session.getMapper(PortalProductDao.class));
                assertEquals(1, service.search("桑蚕丝", null, 987654L, 1, 12, 0).size(), "keyword column is searched");
                assertEquals(2, service.search("通勤", null, 987654L, 1, 12, 0).size(), "subtitle is searched");
                assertEquals(2, service.search("测试品牌", null, 987654L, 1, 12, 0).size(), "brand is searched");
                assertEquals(2, service.search("测试分类", null, 987654L, 1, 12, 0).size(), "category is searched");
                assertEquals(1, service.search("品牌 都市", null, 987654L, 1, 12, 0).size(), "tokens match across fields");
                assertEquals(1, service.search("100%_", null, 987654L, 1, 12, 0).size(), "wildcards are literal text");
                assertEquals(1, service.search(null, "衬衫", null, null, 987654L, 1, 12, 0).size());
                assertEquals(1, service.search(null, null, "FIXTURE-1", null, 987654L, 1, 12, 0).size());
                assertEquals(0, service.search("不存在", null, 987654L, 1, 12, 0).size());
                assertEquals(0, service.search(null, 1L, 987654L, 1, 12, 0).size(), "brand filter also applies");
                List<PmsProduct> page = service.search(null, null, 987654L, 2, 1, 3);
                assertEquals(1, page.size());
                assertEquals("轻便板鞋", page.get(0).getName(), "pagination is applied after search and sorting");
            } finally { session.rollback(true); }
        }
    }
}
