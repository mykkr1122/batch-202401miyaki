package com.example.demo.Tasklet;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.example.demo.Domain.Category;

/**
 * orginalテーブルからcategoryテーブルへのデータ移行
 */
@Component
public class CategoryMigrationTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(CategoryMigrationTasklet.class);

    private final JdbcTemplate template;

    public CategoryMigrationTasklet(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 1ページあたり1万件を処理
        int pageSize = 10000;
        int totalPage = getTotalPages(pageSize);
        logger.info(totalPage + "万件を処理します");

        for (int pageNumber = 1; pageNumber <= totalPage; pageNumber++) {
            List<String> categoryNames = fetchDataPage(pageNumber, pageSize);

            for (String categoryName : categoryNames) {
                if (categoryName != null) {
                    processCategory(categoryName);
                }

            }
            logger.info(pageNumber + "万件の処理を完了しました");
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * originalテーブルからcateogy_nameを抽出
     * 
     * @param pageNumber
     * @param pageSize
     * @return categoryName
     */
    private List<String> fetchDataPage(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        String sql = "SELECT DISTINCT category_name FROM original " +
                "ORDER BY category_name LIMIT ? OFFSET ?";
        return template.queryForList(sql, String.class, pageSize, offset);
    }

    /**
     * 総ページ数を計算
     * 
     * @param pageSize
     * @return totalPage
     */
    private int getTotalPages(int pageSize) {
        String countSql = "SELECT COUNT(*) FROM original ";
        int totalRecords = template.queryForObject(countSql, Integer.class);
        return (int) Math.ceil((double) totalRecords / pageSize);
    }

    /**
     * 抽出してきたcategory_nameを階層分け
     * 
     * @param categoryName
     */
    private void processCategory(String categoryName) {
        String[] categories = categoryName.split("/");

        // 1番目のカテゴリの処理
        String categoryFirst = categories[0];
        if (!isFirstCategoryExists(categoryFirst)) {
            insertCategory(new Category(categoryFirst, null, null));
        }

        // 2番目のカテゴリを処理
        String categorySecond = categories[1];
        // 1つ目のカテゴリーのIDを取ってくる
        Integer categoryFirstId = findRootParentId(categoryFirst);
        if (!isSecondCategoryExists(categorySecond, categoryFirstId)) {
            insertCategory(new Category(categorySecond, categoryFirstId, null));
        }

        // 3番目のカテゴリを処理
        String categoryThird = categories[2];
        Integer lowestParentId = findParentId(categorySecond, categoryFirstId);
        if (!isThirdCategoryExists(categoryThird, lowestParentId, categoryName)) {
            String nameAll = categoryName;
            insertCategory(new Category(categoryThird, lowestParentId, nameAll));
        }

    }

    /**
     * 1番目のカテゴリのIDを取ってくる
     * 
     * @param categoryName
     * @return id(1番目)
     */
    private Integer findRootParentId(String categoryName) {
        String sql = "SELECT id FROM category WHERE name = ? AND parent_id IS NULL AND name_all IS NULL";

        try {
            return template.queryForObject(sql, Integer.class, categoryName);
        } catch (EmptyResultDataAccessException e) {
            logger.info(categoryName + "のルートIDは見つかりませんでした");
            return null;
        }
    }

    /**
     * 2番目のカテゴリのIDを取ってくる
     * 
     * @param categoryName
     * @param parentId
     * @return id(2番目)
     */
    private Integer findParentId(String categoryName, Integer parentId) {
        String sql = "SELECT id FROM category WHERE name = ? AND parent_id = ? AND name_all IS NULL";

        try {
            return template.queryForObject(sql, Integer.class, categoryName, parentId);
        } catch (EmptyResultDataAccessException e) {
            logger.info(categoryName + "のIDは見つかりませんでした");
            logger.info("引数で渡した親IDは" + parentId);
            return null;
        }
    }

    /**
     * 1番目のカテゴリの重複チェック
     * 
     * @param categoryName
     * @return 存在する時、true
     */
    private boolean isFirstCategoryExists(String categoryName) {
        String sql = "SELECT COUNT(*) FROM category WHERE name = ? AND parent_id IS NULL AND name_all IS NULL";
        int count = template.queryForObject(sql, Integer.class, categoryName);
        return count > 0;
    }

    /**
     * 2番目のカテゴリの重複チェック
     * 
     * @param categoryName
     * @param parentId
     * @return 存在する時、true
     */
    private boolean isSecondCategoryExists(String categoryName, Integer parentId) {
        String sql = "SELECT COUNT(*) FROM category WHERE name = ? AND parent_id = ? AND name_all IS NULL";
        int count = template.queryForObject(sql, Integer.class, categoryName, parentId);
        return count > 0;
    }

    /**
     * 3番目のカテゴリの重複チェック
     * 
     * @param categoryName
     * @param parentId
     * @param nameAll
     * @return 存在する時、true
     */
    private boolean isThirdCategoryExists(String categoryName, Integer parentId, String nameAll) {
        String sql = "SELECT COUNT(*) FROM category WHERE name = ? AND parent_id = ? AND name_all = ?";
        int count = template.queryForObject(sql, Integer.class, categoryName, parentId, nameAll);
        return count > 0;
    }

    /**
     * カテゴリー情報をDBに保存
     * 
     * @param category
     */
    private void insertCategory(Category category) {

        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO category (name, parent_id, name_all) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, category.getName());
            ps.setObject(2, category.getParentId(), Types.INTEGER);
            ps.setString(3, category.getNameAll());
            return ps;

        }, keyHolder);
    }
}
