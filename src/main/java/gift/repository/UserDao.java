package gift.repository;

import gift.common.exception.ProductNotFoundException;
import gift.common.exception.UserNotFoundException;
import gift.enums.ReadQuery;
import gift.model.product.Product;
import gift.model.product.ProductRequest;
import gift.model.user.User;
import gift.model.user.UserRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(UserRequest userRequest) {
        var sql = "INSERT INTO users (password, email) VALUES (?, ?)";
        jdbcTemplate.update(sql, userRequest.password(), userRequest.email());

        User user = findByEmail(userRequest.email());
        return user;
    }

    public User findByEmail(String email) {
        var sql = ReadQuery.FIND_BY_EMAIL.getQuery();
        try {
            return jdbcTemplate.queryForObject(
                sql,
                (resultSet, rowNum) -> new User(
                    resultSet.getLong("id"),
                    resultSet.getString("password"),
                    resultSet.getString("email")
                ),
                email
            );
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException();
        }
    }

    public boolean existsByEmail(String email) {
        var sql = "select count(*) from users where email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{email}, Integer.class);
        return count > 0;
    }

    public List<Product> findWishList(Long id) {
        var sql = "select * from product join user_product on product.id = user_product.product_id where user_product.user_id = ?";
        try {
            return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> new Product(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getInt("price"),
                    resultSet.getString("imageUrl")
                ),
                id
            );
        } catch (EmptyResultDataAccessException e) {
            throw new ProductNotFoundException();
        }
    }

    public void registerWishList(Long userId, Long productId, int count) {
        var sql = "INSERT INTO user_product (user_id, product_id) VALUES (?, ?)";

        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(sql, userId, productId);
        }
    }

    public void delete(Long userId, Long productId, int count) {
        var sql = String.format("DELETE FROM user_product WHERE user_id = ? AND product_id = ? LIMIT %d", count);
        jdbcTemplate.update(sql, userId, productId);
    }
}
