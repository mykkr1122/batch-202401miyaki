package com.example.demo.Config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.demo.Listener.CustomStepListener;
import com.example.demo.Listener.JobListener;
import com.example.demo.Tasklet.CategoryMigrationTasklet;
import com.example.demo.Tasklet.ItemMigrationTasklet;

@Configuration
public class BatchConfing {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobListener jobListener;

    @Bean
    CategoryMigrationTasklet firstTasklet() {
        return new CategoryMigrationTasklet(dataSource);
    }

    @Bean
    ItemMigrationTasklet secondTasklet() {
        return new ItemMigrationTasklet();
    }

    /**
     * CategoryMigrationTaskletを実行するステップ
     * 
     * @param jobRepository
     * @param transactionManager
     * @return categoryStep
     */
    @Bean
    public Step categoryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("categoryStep", jobRepository)
                .tasklet(firstTasklet(), transactionManager)
                .listener(new CustomStepListener())
                .build();

    }

    /**
     * ItemMigrationTaskletを実行するステップ
     * 
     * @param jobRepository
     * @param transactionManager
     * @return itemStep
     */
    @Bean
    public Step itemStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("itemStep", jobRepository)
                .tasklet(secondTasklet(), transactionManager)
                .listener(new CustomStepListener())
                .build();
    }

    /**
     * stepを実行するジョブ
     * 
     * @param jobRepository
     * @param categoryStep
     * @param itemStep
     * @return migrationJob
     */
    @Bean
    public Job migrationJob(JobRepository jobRepository, @Qualifier("categoryStep") Step categoryStep,
            @Qualifier("itemStep") Step itemStep) {
        return new JobBuilder("job", jobRepository)
                .start(categoryStep)
                .next(itemStep)
                .listener(jobListener)
                .build();
    }

    public void launchJob(Job job) throws Exception {
        jobLauncher.run(job, new JobParameters());
    }
}
