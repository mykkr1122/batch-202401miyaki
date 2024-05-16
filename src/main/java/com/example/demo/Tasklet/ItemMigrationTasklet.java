package com.example.demo.Tasklet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Domain.ErrorInfo;
import com.example.demo.Domain.Item;
import com.example.demo.Domain.Original;

/*
 * orginalテーブルからitemテーブルへのデータ移行
 */
@Component
public class ItemMigrationTasklet implements Tasklet {
    private static final Logger logger = LoggerFactory.getLogger(ItemMigrationTasklet.class);

    @Autowired
    private JdbcTemplate template;

    @Value("${error.output.path}")
    private String outputPath;

    @Value("${error.output.filename}")
    private String outputFilename;

    private List<ErrorInfo> errorInfoList = new ArrayList<>();

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 1ページあたり10万件を処理
        int pageSize = 100000;

        int totalPage = getTotalPages(pageSize);
        logger.info(totalPage + "0万件を処理します");

        for (int pageNumber = 1; pageNumber <= totalPage; pageNumber++) {

            final int offset = (pageNumber - 1) * pageSize;
            processBatch(offset, pageSize);
            logger.info(pageNumber + "0万件の処理が完了しました");
        }

        return RepeatStatus.FINISHED;

    }

    /**
     * originalテーブルから情報を抽出
     * 
     * @param offset
     * @param pageSize
     * @return originalList
     */
    private List<Original> fetchData(int offset, int pageSize) {
        String sql = "SELECT name, condition, category_name, " +
                "brand, price, shipping, description " +
                "FROM original " +
                "LIMIT ? OFFSET ?";
        try {
            List<Original> originalList = template.query(sql, new BeanPropertyRowMapper<>(Original.class), pageSize,
                    offset);

            List<Original> validOriginals = new ArrayList<>();

            for (Original original : originalList) {
                if (isValid(original)) {
                    ErrorInfo errorInfo = new ErrorInfo(original.getCategoryName(),
                            "INVALID_CATEGORY_NAME");
                    errorInfoList.add(errorInfo);
                } else {
                    validOriginals.add(original);
                }
            }

            if (!errorInfoList.isEmpty()) {
                writeErrorInfoToCSV(errorInfoList);
            }
            return validOriginals;
        } catch (DataAccessException e) {
            logger.error("データの取得中にエラーが発生しました: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 総ページ数を計算
     * 
     * @param pageSize
     * @return totalPage
     */
    private int getTotalPages(int pageSize) {
        String countSql = "SELECT COUNT(*) FROM ( " +
                "SELECT name, condition, category_name, " +
                "brand, price, shipping, description " +
                "FROM original " +
                ") AS subquery ";
        int totalRecords = template.queryForObject(countSql, Integer.class);
        return (int) Math.ceil((double) totalRecords / pageSize);
    }

    /**
     * originalテーブルから抽出してきたcategory_nameが
     * null または 空 または 3階層以上あるか
     * 
     * @param original
     * @return 要件に沿う時、true
     */
    private boolean isValid(Original original) {
        String categoryName = original.getCategoryName();
        return categoryName == null || categoryName.isEmpty() || !isNestedCategory(categoryName);
    }

    /**
     * originalテーブルから抽出してきたcategory_nameが
     * 3階層以上あるか
     * 
     * @param categoryName
     * @return 3階層以上ある時、true
     */
    private boolean isNestedCategory(String categoryName) {
        String[] categories = categoryName.split("/");

        return categories.length >= 3;
    }

    /**
     * 移行処理をひとまとめにするメソッド
     * 
     * @param offset
     * @param pageSize
     */
    private void processBatch(int offset, int pageSize) {
        List<Original> originalList = fetchData(offset, pageSize);

        for (Original original : originalList) {
            Item item = mapToItem(original);
            saveItem(item);
        }
    }

    /**
     * originalテーブルのデータをitemテーブルのデータにマッピング
     * 
     * @param original
     * @return item
     */
    private Item mapToItem(Original original) {
        Item item = new Item();
        item.setName(original.getName());
        item.setCondition(original.getCondition());
        item.setBrand(original.getBrand());
        item.setPrice(original.getPrice());
        item.setShipping(original.getShipping());
        item.setDescription(original.getDescription());

        String categoryName = original.getCategoryName();
        Integer categoryId = findCategoryId(categoryName);

        if (categoryId == null) {
            logger.info("カテゴリ名に対するカテゴリIDが見つかりませんでした " + categoryName);
            return null;
        } else {
            item.setCategory(categoryId);
        }

        return item;

    }

    /**
     * categoryテーブルから、category_nameに合うIDを取ってくる
     * 
     * @param categoryName
     * @return categoryId
     */
    private Integer findCategoryId(String categoryName) {
        String sql = "SELECT id FROM category WHERE name_all = ?";
        try {
            Integer categoryId = template.queryForObject(sql, Integer.class, categoryName);
            return categoryId;
        } catch (EmptyResultDataAccessException e) {
            errorInfoList.add(new ErrorInfo(categoryName, "CATEGORY NOT FOUND"));
            return null;
        }
    }

    /**
     * アイテム情報をDBに保存
     * 
     * @param item
     */
    private void saveItem(Item item) {
        String sql = " INSERT INTO item (name, condition, category, brand, price,shipping, description)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        template.update(sql,
                item.getName(),
                item.getCondition(),
                item.getCategory(),
                item.getBrand(),
                item.getPrice(),
                item.getShipping(),
                item.getDescription());

    }

    /**
     * エラーコードをCSVファイルに書き出し
     * 
     * @param errorInfoList
     */
    private void writeErrorInfoToCSV(List<ErrorInfo> errorInfoList) {
        // ディレクトリを作成
        File directory = new File(outputPath);
        if (!directory.exists()) {
            directory.mkdir();
        }

        // フルパスを構築
        File file = new File(directory, outputFilename);

        // CSVファイルを書き込む
        try (Writer writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write("CategoryName,ErrorCode\n");
            for (ErrorInfo errorInfo : errorInfoList) {
                writer.write(errorInfo.getCategoryName() + "," + errorInfo.getErrorCode() + "\n");
            }
            logger.info("エラー情報をCSVファイルに書き出しました");
        } catch (IOException e) {
            logger.error("CSVファイルの書き込み中にエラーが発生しました" + e.getMessage());
        }
    }

}
